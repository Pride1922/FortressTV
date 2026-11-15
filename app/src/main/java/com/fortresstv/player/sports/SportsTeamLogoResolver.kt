package com.fortresstv.player.sports

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap

/**
 * On-demand, locally cached team crest resolver backed by TheSportsDB's free lookup endpoint.
 * Used as a fallback for EPG-only fixtures (e.g. leagues not covered by ESPN) when
 * [SportsFixture.homeTeamLogoUrl] or [SportsFixture.awayTeamLogoUrl] is null.
 */
internal object SportsTeamLogoResolver {

    private const val PREFS_NAME = "fortress_sports_team_logos"
    private const val HIT_TTL_MS = 14L * 24 * 60 * 60 * 1000 // 14 days
    private const val MISS_TTL_MS = 24L * 60 * 60 * 1000 // 24 hours
    private const val NEGATIVE_SENTINEL = ""
    private const val BASE_URL = "https://www.thesportsdb.com/api/v1/json/123/searchteams.php?t="
    private const val LIGA_PORTUGAL_2_BADGE = "https://r2.thesportsdb.com/images/media/league/badge/pns32l1782689191.png"
    private const val PRIMEIRA_LIGA_BADGE = "https://r2.thesportsdb.com/images/media/league/badge/vuytsr1549891157.png"
    private val ligaPortugal2Regex = Regex(
        """\b(?:liga\s*portugal\s*2|segunda\s*liga|2[ªa.]?\s*liga|ii\s*liga|liga\s*pro|meu\s*super|sabseg)\b""",
        RegexOption.IGNORE_CASE
    )
    private val primeiraLigaRegex = Regex(
        """\b(?:primeira\s*liga|liga\s*portugal(?:\s*betclic)?)\b""",
        RegexOption.IGNORE_CASE
    )

    private val memoryCache = ConcurrentHashMap<String, String>()
    private val inFlightMutex = Mutex()
    private val networkSemaphore = Semaphore(2)

    fun competitionLogoFallback(competition: String): String? {
        val trimmed = competition.trim()
        if (trimmed.isBlank()) return null
        return when {
            ligaPortugal2Regex.containsMatchIn(trimmed) -> LIGA_PORTUGAL_2_BADGE
            primeiraLigaRegex.containsMatchIn(trimmed) -> PRIMEIRA_LIGA_BADGE
            else -> null
        }
    }

    fun getCached(context: Context, teamName: String): String? {
        val key = cacheKey(teamName) ?: return null
        memoryCache[key]?.let { return it.takeIf { url -> url.isNotBlank() } }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = readPrefEntry(prefs, key, System.currentTimeMillis()) ?: return null
        memoryCache[key] = cached
        return cached.takeIf { it.isNotBlank() }
    }

    suspend fun resolve(
        context: Context,
        teamName: String,
        sport: SportsKind? = null
    ): String? {
        val key = cacheKey(teamName) ?: return null
        getCached(context, teamName)?.let { return it }
        if (memoryCache[key] == NEGATIVE_SENTINEL) return null

        return withContext(Dispatchers.IO) {
            inFlightMutex.withLock {
                memoryCache[key]?.let { return@withContext it.takeIf { url -> url.isNotBlank() } }
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                readPrefEntry(prefs, key, System.currentTimeMillis())?.let { cached ->
                    memoryCache[key] = cached
                    return@withContext cached.takeIf { it.isNotBlank() }
                }

                val resolved = networkSemaphore.withPermit {
                    lookupTeamBadge(teamName, sport)
                }
                val storedValue = resolved ?: NEGATIVE_SENTINEL
                memoryCache[key] = storedValue
                writePrefEntry(prefs, key, storedValue, System.currentTimeMillis())
                resolved
            }
        }
    }

    internal fun clearMemoryCacheForTest() {
        memoryCache.clear()
    }

    internal fun cacheKey(teamName: String): String? {
        val normalized = normalizeForMatch(teamName)
        return normalized.takeIf { it.isNotBlank() }
    }

    internal fun buildSearchCandidates(teamName: String): List<String> {
        val trimmed = teamName.trim()
        if (trimmed.isBlank()) return emptyList()
        val candidates = LinkedHashSet<String>()
        candidates.add(trimmed)
        val stripped = stripClubAffixes(trimmed)
        if (stripped.isNotBlank() && stripped != trimmed) {
            candidates.add(stripped)
        }
        val ascii = stripAccents(trimmed)
        if (ascii.isNotBlank() && ascii != trimmed) {
            candidates.add(ascii)
        }
        val asciiStripped = stripClubAffixes(ascii)
        if (asciiStripped.isNotBlank()) {
            candidates.add(asciiStripped)
        }
        return candidates.toList()
    }

    internal fun parseBestBadgeFromJson(
        rawJson: String,
        queryTeamName: String,
        sport: SportsKind?
    ): String? {
        val root = runCatching { JSONObject(rawJson) }.getOrNull() ?: return null
        val teams = root.optJSONArray("teams") ?: return null
        if (teams.length() == 0) return null

        val expectedSport = when (sport) {
            SportsKind.FOOTBALL -> "Soccer"
            SportsKind.BASKETBALL -> "Basketball"
            SportsKind.RUGBY -> "Rugby"
            SportsKind.TENNIS -> "Tennis"
            SportsKind.MOTORSPORT -> "Motorsport"
            else -> null
        }
        val targetNorm = normalizeForMatch(stripClubAffixes(queryTeamName))

        var bestUrl: String? = null
        var bestScore = -1
        for (i in 0 until teams.length()) {
            val team = teams.optJSONObject(i) ?: continue
            val badge = team.optString("strBadge").trim()
            if (badge.isBlank() || badge == "null") continue

            val teamSport = team.optString("strSport").trim()
            if (expectedSport != null && teamSport.isNotBlank() && !teamSport.equals(expectedSport, ignoreCase = true)) {
                continue
            }

            val strTeam = team.optString("strTeam").trim()
            val strAlt = team.optString("strTeamAlternate").trim()
            val teamNorm = normalizeForMatch(stripClubAffixes(strTeam))
            val altNorms = strAlt.split(",").map { normalizeForMatch(stripClubAffixes(it)) }

            val score = when {
                teamNorm == targetNorm -> 100
                targetNorm in altNorms -> 90
                teamNorm.contains(targetNorm) || targetNorm.contains(teamNorm) -> 70
                else -> 10
            }
            if (score > bestScore) {
                bestScore = score
                bestUrl = badge
            }
        }
        return bestUrl
    }

    private fun lookupTeamBadge(teamName: String, sport: SportsKind?): String? {
        for (candidate in buildSearchCandidates(teamName)) {
            val encoded = URLEncoder.encode(candidate, "UTF-8")
            val json = fetchJson("$BASE_URL$encoded") ?: continue
            val badge = parseBestBadgeFromJson(json, teamName, sport)
            if (!badge.isNullOrBlank()) return badge
        }
        return null
    }

    private fun fetchJson(urlString: String): String? {
        return runCatching {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "FortressTV/1.0")
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun readPrefEntry(prefs: SharedPreferences, key: String, nowMs: Long): String? {
        val raw = prefs.getString(key, null) ?: return null
        val sep = raw.indexOf('|')
        if (sep <= 0) return null
        val savedAt = raw.substring(0, sep).toLongOrNull() ?: return null
        val value = raw.substring(sep + 1)
        val ttl = if (value.isBlank()) MISS_TTL_MS else HIT_TTL_MS
        if (nowMs - savedAt > ttl) {
            prefs.edit().remove(key).apply()
            return null
        }
        return value
    }

    private fun writePrefEntry(prefs: SharedPreferences, key: String, value: String, nowMs: Long) {
        prefs.edit().putString(key, "$nowMs|$value").apply()
    }

    private fun normalizeForMatch(value: String): String =
        stripAccents(value)
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun stripAccents(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    private fun stripClubAffixes(value: String): String =
        value.trim()
            .replace(Regex("^(?:FC|SL|SC|UD|CD|GD|CF|AC|AD|SCU|CS|CA|RC|AS|SS|US|SD)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(?:FC|SC|CF|SAD)$", RegexOption.IGNORE_CASE), "")
            .trim()
}

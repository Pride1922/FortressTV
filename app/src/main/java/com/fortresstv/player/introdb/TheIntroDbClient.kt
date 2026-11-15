package com.fortresstv.player.introdb

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

class TheIntroDbClient(
    private val cacheDir: File? = null
) {
    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(
        val segments: EpisodeSegments?,
        val expiresAt: Long
    )

    suspend fun getSegments(
        tmdbId: String? = null,
        imdbId: String? = null,
        tvdbId: String? = null,
        season: Int? = null,
        episode: Int? = null
    ): EpisodeSegments? = withContext(Dispatchers.IO) {
        val candidates = buildList {
            tvdbId?.trim()?.toIntOrNull()?.takeIf { it > 0 }?.let { add("tvdb_id" to it.toString()) }
            tmdbId?.trim()?.toIntOrNull()?.takeIf { it > 0 }?.let { add("tmdb_id" to it.toString()) }
            imdbId?.trim()?.takeIf { IMDB_ID_REGEX.matches(it) }?.let { add("imdb_id" to it.lowercase()) }
        }
        if (candidates.isEmpty()) return@withContext null

        val isEpisode = season != null && episode != null
        val coordSuffix = if (isEpisode) "_s${season}_e${episode}" else "_movie"
        val now = System.currentTimeMillis()
        val normalizedImdbId = imdbId?.trim()?.takeIf { IMDB_ID_REGEX.matches(it) }?.lowercase()

        var primarySegments: EpisodeSegments? = null
        for ((idParam, idValue) in candidates) {
            val cacheKey = "v4_3_${idParam}_${idValue}${coordSuffix}"

            // 1. Check in-memory cache
            val memCached = memoryCache[cacheKey]
            if (memCached != null) {
                if (now < memCached.expiresAt) {
                    if (memCached.segments != null) {
                        primarySegments = memCached.segments
                        break
                    }
                    continue
                } else {
                    memoryCache.remove(cacheKey)
                }
            }

            // 2. Check disk cache
            val diskCached = readDiskCache(cacheKey, season ?: 0, episode ?: 0, now)
            if (diskCached != null) {
                val (segments, expiresAt) = diskCached
                memoryCache[cacheKey] = CacheEntry(segments, expiresAt)
                if (segments != null) {
                    primarySegments = segments
                    break
                }
                continue
            }

            // 3. Network fetch from TheIntroDB (api.theintrodb.org)
            try {
                val encodedId = URLEncoder.encode(idValue, "UTF-8")
                val urlString = if (isEpisode) {
                    "$BASE_URL?$idParam=$encodedId&season=$season&episode=$episode"
                } else {
                    "$BASE_URL?$idParam=$encodedId"
                }
                val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "FortressTV/1.0 (Android)")
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                    val segments = parseResponseJson(responseBody, season ?: 0, episode ?: 0)
                    val expiresAt = now + TTL_SUCCESS_MS
                    memoryCache[cacheKey] = CacheEntry(segments, expiresAt)
                    writeDiskCache(cacheKey, segments, expiresAt)
                    if (segments != null) {
                        primarySegments = segments
                        break
                    }
                } else if (responseCode == 404 || responseCode == 400 || responseCode == 422) {
                    // Negative cache for this ID; continue to next candidate ID if available
                    val expiresAt = now + TTL_NEGATIVE_MS
                    memoryCache[cacheKey] = CacheEntry(null, expiresAt)
                    writeDiskCache(cacheKey, null, expiresAt)
                } else {
                    logWarning(TAG, "TheIntroDB request failed with HTTP $responseCode for $idParam=$idValue")
                }
            } catch (e: Exception) {
                logWarning(TAG, "TheIntroDB network error: ${e.message}")
            }
        }

        val needsFallback = primarySegments == null ||
            (primarySegments.intro.isEmpty() || primarySegments.credits.isEmpty())
        val fallbackSegments = if (isEpisode && normalizedImdbId != null && needsFallback) {
            fetchIntroDbAppSegments(normalizedImdbId, season!!, episode!!, now)
        } else {
            null
        }

        return@withContext mergeSegments(primarySegments, fallbackSegments)
    }

    private fun fetchIntroDbAppSegments(
        imdbId: String,
        season: Int,
        episode: Int,
        now: Long
    ): EpisodeSegments? {
        val cacheKey = "introdbapp_v1_${imdbId}_s${season}_e${episode}"

        val memCached = memoryCache[cacheKey]
        if (memCached != null) {
            if (now < memCached.expiresAt) {
                return memCached.segments
            } else {
                memoryCache.remove(cacheKey)
            }
        }

        val diskCached = readDiskCache(cacheKey, season, episode, now)
        if (diskCached != null) {
            val (segments, expiresAt) = diskCached
            memoryCache[cacheKey] = CacheEntry(segments, expiresAt)
            return segments
        }

        return try {
            val encodedId = URLEncoder.encode(imdbId, "UTF-8")
            val urlString = "$INTRODB_APP_URL?imdb_id=$encodedId&season=$season&episode=$episode"
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "FortressTV/1.0 (Android)")
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val segments = parseIntroDbAppJson(responseBody, season, episode)
                val expiresAt = now + TTL_SUCCESS_MS
                memoryCache[cacheKey] = CacheEntry(segments, expiresAt)
                writeDiskCache(cacheKey, segments, expiresAt)
                segments
            } else if (responseCode == 404 || responseCode == 400 || responseCode == 422) {
                val expiresAt = now + TTL_NEGATIVE_MS
                memoryCache[cacheKey] = CacheEntry(null, expiresAt)
                writeDiskCache(cacheKey, null, expiresAt)
                null
            } else {
                logWarning(TAG, "IntroDB.app request failed with HTTP $responseCode for $imdbId")
                null
            }
        } catch (e: Exception) {
            logWarning(TAG, "IntroDB.app network error: ${e.message}")
            null
        }
    }

    private fun mergeSegments(primary: EpisodeSegments?, fallback: EpisodeSegments?): EpisodeSegments? {
        if (primary == null) return fallback
        if (fallback == null) return primary
        return EpisodeSegments(
            season = primary.season,
            episode = primary.episode,
            intro = primary.intro.ifEmpty { fallback.intro },
            recap = primary.recap.ifEmpty { fallback.recap },
            credits = primary.credits.ifEmpty { fallback.credits },
            preview = primary.preview.ifEmpty { fallback.preview }
        )
    }

    private fun readDiskCache(key: String, season: Int, episode: Int, now: Long): Pair<EpisodeSegments?, Long>? {
        val dir = cacheDir ?: return null
        val file = File(dir, "introdb_${key.hashCode()}.json")
        if (!file.exists()) return null

        return try {
            val raw = file.readText()
            val json = JSONObject(raw)
            val expiresAt = json.optLong("expires_at", 0L)
            if (now >= expiresAt) {
                file.delete()
                return null
            }
            val found = json.optBoolean("found", false)
            if (!found) {
                Pair(null, expiresAt)
            } else {
                val dataObj = json.optJSONObject("data")
                val segments = if (dataObj != null) {
                    parseResponseJson(dataObj.toString(), season, episode)
                } else null
                Pair(segments, expiresAt)
            }
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    private fun writeDiskCache(key: String, segments: EpisodeSegments?, expiresAt: Long) {
        val dir = cacheDir ?: return
        try {
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "introdb_${key.hashCode()}.json")
            val json = JSONObject().apply {
                put("expires_at", expiresAt)
                put("found", segments != null)
                if (segments != null) {
                    put("data", serializeSegments(segments))
                }
            }
            file.writeText(json.toString())
        } catch (e: Exception) {
            logWarning(TAG, "Failed to write TheIntroDB cache: ${e.message}")
        }
    }

    private fun serializeSegments(segments: EpisodeSegments): JSONObject {
        fun serializeList(list: List<MediaSegment>): JSONArray {
            val array = JSONArray()
            list.forEach { seg ->
                val obj = JSONObject().apply {
                    put("start_ms", seg.startMs)
                    if (seg.endMs != null) put("end_ms", seg.endMs)
                }
                array.put(obj)
            }
            return array
        }

        return JSONObject().apply {
            put("season", segments.season)
            put("episode", segments.episode)
            put("intro", serializeList(segments.intro))
            put("recap", serializeList(segments.recap))
            put("credits", serializeList(segments.credits))
            put("preview", serializeList(segments.preview))
        }
    }

    fun parseResponseJson(jsonString: String, season: Int, episode: Int): EpisodeSegments? {
        return try {
            val root = JSONObject(jsonString)

            fun parseSegmentList(key: String, type: SegmentType): List<MediaSegment> {
                val array = root.optJSONArray(key) ?: return emptyList()
                val list = mutableListOf<MediaSegment>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val startIsNull = !obj.has("start_ms") || obj.isNull("start_ms")
                    val endIsNull = !obj.has("end_ms") || obj.isNull("end_ms")
                    if (startIsNull && endIsNull) continue

                    val startMs = if (startIsNull) 0L else obj.optLong("start_ms", 0L).coerceAtLeast(0L)
                    val rawEndMs = if (!endIsNull) obj.optLong("end_ms", -1L).takeIf { it >= 0L } else null

                    // Per v4 spec, credits/preview with start_ms=0 (or null) and no end_ms means no segment
                    if ((type == SegmentType.CREDITS || type == SegmentType.PREVIEW) && startMs == 0L && rawEndMs == null) {
                        continue
                    }

                    // If end_ms is specified but not after start_ms (including 0 for "no segment"), ignore
                    if (rawEndMs != null && rawEndMs <= startMs) {
                        continue
                    }

                    list.add(MediaSegment(type = type, startMs = startMs, endMs = rawEndMs))
                }
                return list
            }

            val intro = parseSegmentList("intro", SegmentType.INTRO)
            val recap = parseSegmentList("recap", SegmentType.RECAP)
            val credits = parseSegmentList("credits", SegmentType.CREDITS)
            val preview = parseSegmentList("preview", SegmentType.PREVIEW)

            EpisodeSegments(
                season = root.optInt("season", season),
                episode = root.optInt("episode", episode),
                intro = intro,
                recap = recap,
                credits = credits,
                preview = preview
            )
        } catch (e: Exception) {
            logWarning(TAG, "Failed to parse TheIntroDB response: ${e.message}")
            null
        }
    }

    fun parseIntroDbAppJson(jsonString: String, season: Int, episode: Int): EpisodeSegments? {
        return try {
            val root = JSONObject(jsonString)

            fun parseObjectSegment(key: String, type: SegmentType): List<MediaSegment> {
                val obj = root.optJSONObject(key) ?: return emptyList()
                val startMs = if (obj.has("start_ms") && !obj.isNull("start_ms")) {
                    obj.optLong("start_ms", -1L)
                } else if (obj.has("start_sec") && !obj.isNull("start_sec")) {
                    (obj.optDouble("start_sec", -1.0) * 1000.0).toLong()
                } else {
                    0L
                }.coerceAtLeast(0L)

                val rawEndMs = if (obj.has("end_ms") && !obj.isNull("end_ms")) {
                    obj.optLong("end_ms", -1L).takeIf { it >= 0L }
                } else if (obj.has("end_sec") && !obj.isNull("end_sec")) {
                    (obj.optDouble("end_sec", -1.0) * 1000.0).toLong().takeIf { it >= 0L }
                } else {
                    null
                }

                if ((type == SegmentType.CREDITS || type == SegmentType.PREVIEW) && startMs == 0L && rawEndMs == null) {
                    return emptyList()
                }
                if (rawEndMs != null && rawEndMs <= startMs) {
                    return emptyList()
                }
                return listOf(MediaSegment(type = type, startMs = startMs, endMs = rawEndMs))
            }

            val intro = parseObjectSegment("intro", SegmentType.INTRO)
            val recap = parseObjectSegment("recap", SegmentType.RECAP)
            val credits = parseObjectSegment("outro", SegmentType.CREDITS)
            val preview = parseObjectSegment("post_credits", SegmentType.PREVIEW)
            if (intro.isEmpty() && recap.isEmpty() && credits.isEmpty() && preview.isEmpty()) {
                return null
            }

            EpisodeSegments(
                season = root.optInt("season", season),
                episode = root.optInt("episode", episode),
                intro = intro,
                recap = recap,
                credits = credits,
                preview = preview
            )
        } catch (e: Exception) {
            logWarning(TAG, "Failed to parse IntroDB.app response: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "TheIntroDbClient"
        private const val BASE_URL = "https://api.theintrodb.org/v4/media"
        private const val INTRODB_APP_URL = "https://api.introdb.app/segments"
        private val IMDB_ID_REGEX = Regex("^tt\\d+$", RegexOption.IGNORE_CASE)
        private const val TTL_SUCCESS_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
        private const val TTL_NEGATIVE_MS = 24L * 60 * 60 * 1000 // 24 hours

        @Volatile
        private var instance: TheIntroDbClient? = null

        fun getInstance(context: Context): TheIntroDbClient {
            return instance ?: synchronized(this) {
                instance ?: TheIntroDbClient(
                    cacheDir = File(context.cacheDir, "introdb_cache")
                ).also { instance = it }
            }
        }

        private fun logWarning(tag: String, message: String) {
            try {
                android.util.Log.w(tag, message)
            } catch (_: Throwable) {
                // Ignore in JVM tests where android.util.Log is not mocked
            }
        }
    }
}

package com.fortresstv.player.omdb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

class OmdbClient(
    private val apiKey: String,
    private val apiBaseUrl: String = DEFAULT_BASE_URL
) {

    suspend fun validateApiKey(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val queryParams = mapOf(
                "apikey" to apiKey.trim(),
                "i" to "tt0111161" // The Shawshank Redemption test probe
            )
            val response = executeRequest(queryParams)
            val json = JSONObject(response)
            if (json.optString("Response", "False").equals("True", ignoreCase = true)) {
                true
            } else {
                val errorMsg = json.optString("Error", "Invalid API key")
                error(errorMsg)
            }
        }
    }

    suspend fun getByImdbId(imdbId: String): OmdbMetadata? = withContext(Dispatchers.IO) {
        val cleanImdb = normalizeImdbId(imdbId) ?: return@withContext null
        runCatching {
            val queryParams = mapOf(
                "apikey" to apiKey.trim(),
                "i" to cleanImdb
            )
            val response = executeRequest(queryParams)
            parseMetadata(JSONObject(response))
        }.getOrNull()
    }

    suspend fun getByTitle(
        title: String,
        year: String? = null,
        type: String? = null
    ): OmdbMetadata? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null
        runCatching {
            val cleanTitle = cleanSearchTitle(title)
            val queryParams = mutableMapOf(
                "apikey" to apiKey.trim(),
                "t" to cleanTitle
            )
            val cleanYear = year?.trim()?.take(4)?.filter { it.isDigit() }?.takeIf { it.length == 4 }
            if (cleanYear != null) {
                queryParams["y"] = cleanYear
            }
            if (!type.isNullOrBlank()) {
                queryParams["type"] = type.trim()
            }
            val response = executeRequest(queryParams)
            parseMetadata(JSONObject(response))
        }.getOrNull()
    }

    private fun parseMetadata(json: JSONObject): OmdbMetadata? {
        if (!json.optString("Response", "False").equals("True", ignoreCase = true)) {
            return null
        }

        var rtScore: String? = null
        var metascoreVal: String? = json.optString("Metascore").takeIf { it.isNotBlank() && it != "N/A" }

        val ratings = json.optJSONArray("Ratings")
        if (ratings != null) {
            for (i in 0 until ratings.length()) {
                val item = ratings.optJSONObject(i) ?: continue
                val source = item.optString("Source")
                val value = item.optString("Value")
                if (source.contains("Rotten Tomatoes", ignoreCase = true)) {
                    rtScore = value
                } else if (source.contains("Metacritic", ignoreCase = true) && metascoreVal == null) {
                    metascoreVal = value.substringBefore("/")
                }
            }
        }

        return OmdbMetadata(
            title = json.optString("Title").takeIf { it.isNotBlank() && it != "N/A" },
            year = json.optString("Year").takeIf { it.isNotBlank() && it != "N/A" },
            rated = json.optString("Rated").takeIf { it.isNotBlank() && it != "N/A" },
            released = json.optString("Released").takeIf { it.isNotBlank() && it != "N/A" },
            runtime = json.optString("Runtime").takeIf { it.isNotBlank() && it != "N/A" },
            genre = json.optString("Genre").takeIf { it.isNotBlank() && it != "N/A" },
            director = json.optString("Director").takeIf { it.isNotBlank() && it != "N/A" },
            writer = json.optString("Writer").takeIf { it.isNotBlank() && it != "N/A" },
            actors = json.optString("Actors").takeIf { it.isNotBlank() && it != "N/A" },
            plot = json.optString("Plot").takeIf { it.isNotBlank() && it != "N/A" },
            awards = json.optString("Awards").takeIf { it.isNotBlank() && it != "N/A" },
            poster = json.optString("Poster").takeIf { it.isNotBlank() && it != "N/A" },
            imdbRating = json.optString("imdbRating").takeIf { it.isNotBlank() && it != "N/A" },
            imdbVotes = json.optString("imdbVotes").takeIf { it.isNotBlank() && it != "N/A" },
            imdbId = json.optString("imdbID").takeIf { it.isNotBlank() && it != "N/A" },
            type = json.optString("Type").takeIf { it.isNotBlank() && it != "N/A" },
            rottenTomatoes = rtScore,
            metascore = metascoreVal,
            boxOffice = json.optString("BoxOffice").takeIf { it.isNotBlank() && it != "N/A" }
        )
    }

    private fun executeRequest(queryParams: Map<String, String>): String {
        val queryString = queryParams.entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, "UTF-8")}"
        }
        val url = URI("$apiBaseUrl/?$queryString").toURL()
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", "FortressTV/2.0")
            connection.setRequestProperty("Accept", "application/json")

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                error("HTTP $status: $responseText")
            }
            responseText
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://www.omdbapi.com"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 8_000

        fun normalizeImdbId(imdbId: String?): String? {
            if (imdbId.isNullOrBlank()) return null
            val trimmed = imdbId.trim()
            return if (trimmed.startsWith("tt", ignoreCase = true)) {
                trimmed.lowercase()
            } else {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.isNotBlank()) "tt$digits" else null
            }
        }

        fun extractYear(rawTitle: String, fallbackYear: String?): String? {
            val fallbackClean = fallbackYear?.trim()?.take(4)?.filter { it.isDigit() }?.takeIf { it.length == 4 }
            if (fallbackClean != null) return fallbackClean
            val match = Regex("\\b(19\\d{2}|20\\d{2})\\b").find(rawTitle)
            return match?.value
        }

        fun cleanSearchTitle(rawTitle: String): String {
            return rawTitle
                // 1. Strip country prefixes like "PT:", "PT |", "EN -", "BR:"
                .replace(Regex("(?i)^[a-z]{2}\\s*[:|\\-]\\s*"), "")
                // 2. Strip bracketed or parenthesized release tags
                .replace(Regex("(?i)\\[[^\\]]*\\]"), " ")
                .replace(Regex("(?i)\\((?:legendado|dublado|dual|multi|audio|som|pt|en|br|es|fr|de|vostfr)\\)"), " ")
                // 3. Strip release tags, formats, and audio specs
                .replace(Regex("(?i)\\b(?:fhd|4k|uhd|hevc|h265|h264|x264|x265|1080p|720p|480p|bluray|blu-ray|web-dl|webrip|dvdrip|hdtv|remux|imax|extended|directors\\s*cut|unrated|proper|repack|dual|multi|dublado|legendado|5\\.1|7\\.1)\\b"), " ")
                // 4. Strip season tags from series titles like S01, S01-S05, Season 1, T1
                .replace(Regex("(?i)\\b(?:s\\d{1,2}(?:\\s*-\\s*s?\\d{1,2})?|season\\s*\\d{1,2}|temporada\\s*\\d{1,2}|t\\d{1,2})\\b"), " ")
                // 5. Strip 4-digit years from title text
                .replace(Regex("\\b(?:19\\d{2}|20\\d{2})\\b"), " ")
                // 6. Clean punctuation & excess whitespace
                .replace(Regex("[\\[\\]()_.:-]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}

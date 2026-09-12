package com.fortresstv.player.subtitles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

class OpenSubtitlesClient(
    private val apiKey: String,
    private val username: String? = null,
    private val password: String? = null,
    private val userAgent: String = "FortressTV v1.0"
) {

    private var cachedToken: String? = null
    private var apiBaseUrl = DEFAULT_API_BASE_URL

    suspend fun login(): Result<String> = withContext(Dispatchers.IO) {
        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Username or password is blank"))
        }
        runCatching {
            val url = URI("$DEFAULT_API_BASE_URL/login").toURL()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Api-Key", apiKey.trim())
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
            }

            val body = JSONObject().apply {
                put("username", username.trim())
                put("password", password.trim())
            }.toString()

            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val error = readStream(conn.errorStream ?: conn.inputStream)
                throw IllegalStateException(apiError("Login failed", responseCode, error))
            }

            val json = JSONObject(readStream(conn.inputStream))
            val token = json.optString("token")
            if (token.isBlank()) {
                throw IllegalStateException("Login succeeded but no token returned")
            }
            cachedToken = token
            apiBaseUrl = normalizeBaseUrl(json.optString("base_url"))
            token
        }
    }

    suspend fun searchSubtitles(
        query: String,
        season: Int? = null,
        episode: Int? = null,
        imdbId: String? = null,
        languages: String? = null
    ): Result<List<OpenSubtitlesItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val params = mutableListOf<String>()
            languages?.takeIf { it.isNotBlank() }?.let {
                params.add("languages=" + URLEncoder.encode(it, "UTF-8"))
            }

            val cleanImdb = imdbId?.replace(Regex("[^0-9]"), "")?.takeIf { it.isNotBlank() }
            if (cleanImdb != null) {
                params.add("imdb_id=$cleanImdb")
            } else {
                params.add("query=" + URLEncoder.encode(query.trim(), "UTF-8"))
            }

            if (season != null && episode != null && season > 0 && episode > 0) {
                params.add("season_number=$season")
                params.add("episode_number=$episode")
                params.add("type=episode")
            } else {
                params.add("type=movie")
            }

            val url = URI("$apiBaseUrl/subtitles?" + params.joinToString("&")).toURL()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("Api-Key", apiKey.trim())
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/json")
                cachedToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val error = readStream(conn.errorStream ?: conn.inputStream)
                throw IllegalStateException(apiError("Search failed", responseCode, error))
            }

            val responseBody = readStream(conn.inputStream)
            parseSearchResults(responseBody)
        }
    }

    suspend fun downloadSubtitle(fileId: Int): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (cachedToken == null && !username.isNullOrBlank() && !password.isNullOrBlank()) {
                login().getOrThrow()
            }
            if (cachedToken == null) throw IllegalStateException("Username and password are required to download subtitles")

            val url = URI("$apiBaseUrl/download").toURL()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Api-Key", apiKey.trim())
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                cachedToken?.let { setRequestProperty("Authorization", "Bearer $it") }
                doOutput = true
            }

            val body = JSONObject().apply {
                put("file_id", fileId)
            }.toString()

            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val error = readStream(conn.errorStream ?: conn.inputStream)
                throw IllegalStateException(apiError("Download request failed", responseCode, error))
            }

            val json = JSONObject(readStream(conn.inputStream))
            val link = json.optString("link")
            if (link.isBlank()) {
                throw IllegalStateException("No download link returned")
            }

            val srtConn = (URI(link).toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("User-Agent", userAgent)
            }

            val srtCode = srtConn.responseCode
            if (srtCode !in 200..299) {
                throw IllegalStateException("Downloading SRT failed (HTTP $srtCode)")
            }

            val content = readStream(srtConn.inputStream)
            stripBom(content)
        }
    }

    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (apiKey.isBlank()) throw IllegalArgumentException("API key is blank")
            if (username.isNullOrBlank() xor password.isNullOrBlank()) {
                throw IllegalArgumentException("Enter both username and password, or leave both blank")
            }
            if (!username.isNullOrBlank()) {
                login().getOrThrow()
                true
            } else {
                val url = URI("$DEFAULT_API_BASE_URL/infos/formats").toURL()
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    setRequestProperty("Api-Key", apiKey.trim())
                    setRequestProperty("User-Agent", userAgent)
                    setRequestProperty("Accept", "application/json")
                }
                val code = conn.responseCode
                if (code in 200..299) true else {
                    val error = readStream(conn.errorStream ?: conn.inputStream)
                    throw IllegalStateException(apiError("API key check failed", code, error))
                }
            }
        }
    }

    internal fun normalizeBaseUrl(host: String): String {
        val cleanHost = host.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
        return when (cleanHost) {
            "api.opensubtitles.com", "vip-api.opensubtitles.com" -> "https://$cleanHost/api/v1"
            else -> DEFAULT_API_BASE_URL
        }
    }

    internal fun apiError(action: String, responseCode: Int, responseBody: String): String {
        val message = runCatching {
            val json = JSONObject(responseBody)
            json.optString("message").ifBlank { json.optString("error") }
        }.getOrNull().orEmpty()
        return "$action (HTTP $responseCode)" + if (message.isBlank()) "" else ": $message"
    }

    internal fun parseSearchResults(jsonString: String): List<OpenSubtitlesItem> {
        val root = JSONObject(jsonString)
        val data = root.optJSONArray("data") ?: return emptyList()
        val results = mutableListOf<OpenSubtitlesItem>()

        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val attrs = item.optJSONObject("attributes") ?: continue
            val language = attrs.optString("language").lowercase()
            val release = attrs.optString("release")
            val downloads = attrs.optInt("download_count", 0)
            val ratings = attrs.optDouble("ratings", 0.0)
            val hearingImpaired = attrs.optBoolean("hearing_impaired", false)

            val files = attrs.optJSONArray("files") ?: JSONArray()
            val firstFile = files.optJSONObject(0) ?: continue
            val fileId = firstFile.optInt("file_id", -1)
            val fileName = firstFile.optString("file_name").orEmpty()

            if (fileId > 0) {
                results.add(
                    OpenSubtitlesItem(
                        id = id,
                        fileId = fileId,
                        fileName = fileName,
                        language = language,
                        release = release.ifBlank { fileName },
                        downloads = downloads,
                        rating = ratings,
                        hearingImpaired = hearingImpaired
                    )
                )
            }
        }
        return results
    }

    internal fun readStream(stream: java.io.InputStream): String {
        val pushback = java.io.PushbackInputStream(stream, 2)
        val head = ByteArray(2)
        val read = pushback.read(head)
        if (read > 0) {
            pushback.unread(head, 0, read)
        }
        val isGzip = read == 2 && head[0] == 0x1f.toByte() && head[1] == 0x8b.toByte()
        val effectiveStream = if (isGzip) {
            java.util.zip.GZIPInputStream(pushback)
        } else {
            pushback
        }
        return BufferedReader(InputStreamReader(effectiveStream, Charsets.UTF_8)).use { it.readText() }
    }

    internal fun stripBom(content: String): String {
        return if (content.startsWith("\uFEFF")) content.substring(1) else content
    }

    private companion object {
        const val DEFAULT_API_BASE_URL = "https://api.opensubtitles.com/api/v1"
    }
}

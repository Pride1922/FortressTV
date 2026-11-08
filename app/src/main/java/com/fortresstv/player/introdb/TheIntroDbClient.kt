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
        season: Int,
        episode: Int
    ): EpisodeSegments? = withContext(Dispatchers.IO) {
        val (idParam, idValue) = when {
            !tmdbId.isNullOrBlank() -> "tmdb_id" to tmdbId.trim()
            !imdbId.isNullOrBlank() -> "imdb_id" to imdbId.trim()
            !tvdbId.isNullOrBlank() -> "tvdb_id" to tvdbId.trim()
            else -> return@withContext null
        }

        val cacheKey = "${idParam}_${idValue}_s${season}_e${episode}"
        val now = System.currentTimeMillis()

        // 1. Check in-memory cache
        val memCached = memoryCache[cacheKey]
        if (memCached != null) {
            if (now < memCached.expiresAt) {
                return@withContext memCached.segments
            } else {
                memoryCache.remove(cacheKey)
            }
        }

        // 2. Check disk cache
        val diskCached = readDiskCache(cacheKey, season, episode, now)
        if (diskCached != null) {
            val (segments, expiresAt) = diskCached
            memoryCache[cacheKey] = CacheEntry(segments, expiresAt)
            return@withContext segments
        }

        // 3. Network fetch
        try {
            val encodedId = URLEncoder.encode(idValue, "UTF-8")
            val urlString = "$BASE_URL?$idParam=$encodedId&season=$season&episode=$episode"
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "FortressTV/1.0 (Android)")
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val segments = parseResponseJson(responseBody, season, episode)
                val expiresAt = now + TTL_SUCCESS_MS
                memoryCache[cacheKey] = CacheEntry(segments, expiresAt)
                writeDiskCache(cacheKey, segments, expiresAt)
                return@withContext segments
            } else if (responseCode == 404 || responseCode == 400) {
                // Negative cache
                val expiresAt = now + TTL_NEGATIVE_MS
                memoryCache[cacheKey] = CacheEntry(null, expiresAt)
                writeDiskCache(cacheKey, null, expiresAt)
                return@withContext null
            } else {
                logWarning(TAG, "TheIntroDB request failed with HTTP $responseCode")
                return@withContext null
            }
        } catch (e: Exception) {
            logWarning(TAG, "TheIntroDB network error: ${e.message}")
            return@withContext null
        }
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
                    val startMs = obj.optLong("start_ms", 0L).coerceAtLeast(0L)
                    val rawEndMs = if (obj.has("end_ms") && !obj.isNull("end_ms")) {
                        obj.optLong("end_ms")
                    } else null

                    // If end_ms is specified but not after start_ms, ignore invalid segment
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

    companion object {
        private const val TAG = "TheIntroDbClient"
        private const val BASE_URL = "https://api.theintrodb.org/v4/media"
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

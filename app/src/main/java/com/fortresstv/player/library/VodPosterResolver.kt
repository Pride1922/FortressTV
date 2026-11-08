package com.fortresstv.player.library

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object VodPosterResolver {
    private val memoryCache = ConcurrentHashMap<String, String>()

    suspend fun resolveMoviePoster(rawTitle: String): String? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanMovieTitle(rawTitle)
        if (cleanTitle.isBlank()) return@withContext null

        memoryCache[cleanTitle]?.let { return@withContext it }

        runCatching {
            val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
            val urlString = "https://v3-cinemeta.strem.io/catalog/movie/top/search=$encoded.json"
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(json)
                val metas = root.optJSONArray("metas")
                if (metas != null && metas.length() > 0) {
                    val poster = metas.optJSONObject(0)?.optString("poster", "")
                    if (!poster.isNullOrBlank()) {
                        memoryCache[cleanTitle] = poster
                        return@withContext poster
                    }
                }
            }
            null
        }.getOrNull()
    }

    private fun cleanMovieTitle(title: String): String {
        return title
            .replace(Regex("\\([0-9]{4}\\)"), "")
            .replace(Regex("\\[[^\\]]+\\]"), "")
            .replace(Regex("[^a-zA-Z0-9'\\s]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}

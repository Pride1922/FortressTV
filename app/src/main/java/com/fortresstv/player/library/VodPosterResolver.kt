package com.fortresstv.player.library

import android.content.Context
import com.fortresstv.player.tmdb.TmdbClient
import com.fortresstv.player.tmdb.TmdbMetadataResolver
import com.fortresstv.player.tmdb.TmdbSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object VodPosterResolver {
    private val memoryCache = ConcurrentHashMap<String, String>()
    private val YEAR_SUFFIX_REGEX = Regex("\\([0-9]{4}\\)")
    private val BRACKET_TAG_REGEX = Regex("\\[[^\\]]+\\]")
    private val NON_ALPHA_SPACE_REGEX = Regex("[^a-zA-Z0-9'\\s]")
    private val WHITESPACE_REGEX = Regex("\\s+")

    suspend fun resolveMoviePoster(
        rawTitle: String,
        context: Context? = null,
        tmdbSettings: TmdbSettings? = null,
        year: String? = null,
        tmdbId: String? = null,
        imdbId: String? = null
    ): String? = resolvePoster(
        rawTitle = rawTitle,
        isSeries = false,
        context = context,
        tmdbSettings = tmdbSettings,
        year = year,
        tmdbId = tmdbId,
        imdbId = imdbId
    )

    suspend fun resolvePoster(
        rawTitle: String,
        isSeries: Boolean = false,
        context: Context? = null,
        tmdbSettings: TmdbSettings? = null,
        year: String? = null,
        tmdbId: String? = null,
        imdbId: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val (cleanedTitle, extractedYear) = TmdbClient.cleanMediaTitle(rawTitle)
        val fallbackClean = cleanMovieTitle(rawTitle)
        val searchTitle = cleanedTitle.ifBlank { fallbackClean }
        if (searchTitle.isBlank()) return@withContext null

        val effectiveYear = year ?: extractedYear
        val cacheKey = "${if (isSeries) "tv" else "movie"}:${tmdbSettings?.language.orEmpty()}:$searchTitle:${effectiveYear.orEmpty()}:${tmdbId.orEmpty()}:${imdbId.orEmpty()}"
        memoryCache[cacheKey]?.let { return@withContext it }

        // 1. Try TMDB if configured
        if (context != null && tmdbSettings != null && tmdbSettings.enabled && tmdbSettings.apiKey.isNotBlank()) {
            val tmdbPoster = runCatching {
                if (isSeries) {
                    TmdbMetadataResolver.resolveSeries(context, tmdbSettings, rawTitle, effectiveYear, tmdbId, imdbId)?.posterUrl
                } else {
                    TmdbMetadataResolver.resolveMovie(context, tmdbSettings, rawTitle, effectiveYear, tmdbId, imdbId)?.posterUrl
                }
            }.getOrNull()
            if (!tmdbPoster.isNullOrBlank()) {
                memoryCache[cacheKey] = tmdbPoster
                return@withContext tmdbPoster
            }
        }

        // 2. Fallback to Cinemeta
        runCatching {
            val encoded = URLEncoder.encode(searchTitle, "UTF-8")
            val mediaType = if (isSeries) "series" else "movie"
            val urlString = "https://v3-cinemeta.strem.io/catalog/$mediaType/top/search=$encoded.json"
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
                        memoryCache[cacheKey] = poster
                        return@withContext poster
                    }
                }
            }
            null
        }.getOrNull()
    }

    private fun cleanMovieTitle(title: String): String {
        return title
            .replace(YEAR_SUFFIX_REGEX, "")
            .replace(BRACKET_TAG_REGEX, "")
            .replace(NON_ALPHA_SPACE_REGEX, " ")
            .trim()
            .replace(WHITESPACE_REGEX, " ")
    }
}

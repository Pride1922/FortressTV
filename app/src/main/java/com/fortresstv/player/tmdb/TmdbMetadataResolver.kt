package com.fortresstv.player.tmdb

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object TmdbMetadataResolver {

    private val memoryCache = ConcurrentHashMap<String, TmdbMetadata?>()
    private const val CACHE_DIR_NAME = "tmdb_cache"
    private const val CACHE_TTL_MS = 14 * 24 * 60 * 60 * 1000L // 14 days

    fun clearMemoryCache() {
        memoryCache.clear()
    }

    suspend fun resolveMovie(
        context: Context,
        settings: TmdbSettings,
        rawTitle: String,
        year: String? = null,
        tmdbId: String? = null,
        imdbId: String? = null
    ): TmdbMetadata? = withContext(Dispatchers.IO) {
        if (!settings.enabled || settings.apiKey.isBlank()) return@withContext null

        val cacheKey = buildCacheKey("movie", rawTitle, year, tmdbId, imdbId, settings.language)
        if (memoryCache.containsKey(cacheKey)) {
            return@withContext memoryCache[cacheKey]
        }

        val cachedDisk = readDiskCache(context, cacheKey)
        if (cachedDisk != null) {
            memoryCache[cacheKey] = cachedDisk
            return@withContext cachedDisk
        }

        val client = TmdbClient(settings.apiKey)
        val language = settings.language.ifBlank { "pt-PT" }

        // Priority 1: Xtream tmdb_id
        val parsedTmdbId = tmdbId?.trim()?.toIntOrNull()?.takeIf { it > 0 }
        var result: TmdbMetadata? = if (parsedTmdbId != null) {
            client.getMovieDetails(parsedTmdbId, language)
        } else null

        // Priority 2: Xtream imdb_id
        if (result == null && !imdbId.isNullOrBlank()) {
            result = client.findMovieByImdb(imdbId, language)
        }

        // Priority 3: Search clean title and year
        if (result == null && rawTitle.isNotBlank()) {
            result = client.searchMovie(rawTitle, year, language)
        }

        memoryCache[cacheKey] = result
        if (result != null) {
            writeDiskCache(context, cacheKey, result)
        }

        result
    }

    suspend fun resolveSeries(
        context: Context,
        settings: TmdbSettings,
        rawTitle: String,
        year: String? = null,
        tmdbId: String? = null,
        imdbId: String? = null
    ): TmdbMetadata? = withContext(Dispatchers.IO) {
        if (!settings.enabled || settings.apiKey.isBlank()) return@withContext null

        val cacheKey = buildCacheKey("tv", rawTitle, year, tmdbId, imdbId, settings.language)
        if (memoryCache.containsKey(cacheKey)) {
            return@withContext memoryCache[cacheKey]
        }

        val cachedDisk = readDiskCache(context, cacheKey)
        if (cachedDisk != null) {
            memoryCache[cacheKey] = cachedDisk
            return@withContext cachedDisk
        }

        val client = TmdbClient(settings.apiKey)
        val language = settings.language.ifBlank { "pt-PT" }

        // Priority 1: Xtream tmdb_id
        val parsedTmdbId = tmdbId?.trim()?.toIntOrNull()?.takeIf { it > 0 }
        var result: TmdbMetadata? = if (parsedTmdbId != null) {
            client.getSeriesDetails(parsedTmdbId, language)
        } else null

        // Priority 2: Xtream imdb_id
        if (result == null && !imdbId.isNullOrBlank()) {
            result = client.findSeriesByImdb(imdbId, language)
        }

        // Priority 3: Search clean title and year
        if (result == null && rawTitle.isNotBlank()) {
            result = client.searchSeries(rawTitle, year, language)
        }

        memoryCache[cacheKey] = result
        if (result != null) {
            writeDiskCache(context, cacheKey, result)
        }

        result
    }

    private fun buildCacheKey(
        type: String,
        rawTitle: String,
        year: String?,
        tmdbId: String?,
        imdbId: String?,
        language: String
    ): String {
        val raw = "$type:${rawTitle.trim().lowercase()}:${year.orEmpty()}:${tmdbId.orEmpty()}:${imdbId.orEmpty()}:${language.lowercase()}"
        val digest = MessageDigest.getInstance("MD5").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun readDiskCache(context: Context, cacheKey: String): TmdbMetadata? = runCatching {
        val dir = File(context.filesDir, CACHE_DIR_NAME)
        val file = File(dir, "$cacheKey.json")
        if (!file.exists()) return null
        if (System.currentTimeMillis() - file.lastModified() > CACHE_TTL_MS) {
            file.delete()
            return null
        }
        val json = JSONObject(file.readText())
        val genresJson = json.optJSONArray("genres")
        val genres = mutableListOf<String>()
        if (genresJson != null) {
            for (i in 0 until genresJson.length()) {
                genres.add(genresJson.getString(i))
            }
        }
        val castJson = json.optJSONArray("cast")
        val cast = mutableListOf<String>()
        if (castJson != null) {
            for (i in 0 until castJson.length()) {
                cast.add(castJson.getString(i))
            }
        }

        TmdbMetadata(
            tmdbId = json.optInt("tmdb_id"),
            title = json.optString("title"),
            overview = json.optString("overview").takeIf(String::isNotEmpty),
            posterUrl = json.optString("poster_url").takeIf(String::isNotEmpty),
            backdropUrl = json.optString("backdrop_url").takeIf(String::isNotEmpty),
            rating = json.optString("rating").takeIf(String::isNotEmpty),
            releaseDate = json.optString("release_date").takeIf(String::isNotEmpty),
            genres = genres,
            director = json.optString("director").takeIf(String::isNotEmpty),
            cast = cast,
            trailerUrl = json.optString("trailer_url").takeIf(String::isNotEmpty)
        )
    }.getOrNull()

    private fun writeDiskCache(context: Context, cacheKey: String, metadata: TmdbMetadata) = runCatching {
        val dir = File(context.filesDir, CACHE_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        val temp = File(dir, "$cacheKey.tmp")
        val file = File(dir, "$cacheKey.json")

        val json = JSONObject().apply {
            put("tmdb_id", metadata.tmdbId)
            put("title", metadata.title)
            put("overview", metadata.overview.orEmpty())
            put("poster_url", metadata.posterUrl.orEmpty())
            put("backdrop_url", metadata.backdropUrl.orEmpty())
            put("rating", metadata.rating.orEmpty())
            put("release_date", metadata.releaseDate.orEmpty())
            put("genres", JSONArray(metadata.genres))
            put("director", metadata.director.orEmpty())
            put("cast", JSONArray(metadata.cast))
            put("trailer_url", metadata.trailerUrl.orEmpty())
            put("cached_at", System.currentTimeMillis())
        }

        temp.writeText(json.toString())
        if (file.exists()) file.delete()
        temp.renameTo(file)
    }
}

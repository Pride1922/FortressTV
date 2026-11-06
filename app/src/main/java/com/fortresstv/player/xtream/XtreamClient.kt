package com.fortresstv.player.xtream

import android.content.Context
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.source.HttpStatusException
import com.fortresstv.player.source.retryTransientNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLException

class XtreamClient(
    private val context: Context? = null,
    private val sourceId: String,
    private val source: ContentSource.Xtream
) {
    @Volatile
    private var baseUrl = normalizeServerUrl(source.value)

    fun resolvedServerUrl(): String = baseUrl

    suspend fun authenticate() {
        val root = JSONObject(request(null))
        val user = root.optJSONObject("user_info") ?: error("Invalid Xtream response")
        val authenticated = user.optInt("auth", user.optString("auth").toIntOrNull() ?: 0) == 1
        val status = user.optString("status")
        require(authenticated && (status.isBlank() || status.equals("Active", true))) {
            if (status.isBlank()) "Xtream authentication failed" else "Xtream account is $status"
        }
    }

    suspend fun loadCatalog(forceRefresh: Boolean = false): XtreamCatalog = coroutineScope {
        authenticate()
        val liveCategoriesDeferred = async { parseCategories(request("get_live_categories", forceRefresh = forceRefresh)) }
        val movieCategoriesDeferred = async { parseCategories(request("get_vod_categories", forceRefresh = forceRefresh)) }
        val seriesCategoriesDeferred = async { parseCategories(request("get_series_categories", forceRefresh = forceRefresh)) }
        val liveStreamsDeferred = async { request("get_live_streams", forceRefresh = forceRefresh) }
        val moviesDeferred = async { parseMovies(request("get_vod_streams", forceRefresh = forceRefresh)) }
        val seriesDeferred = async { parseSeries(request("get_series", forceRefresh = forceRefresh)) }

        val liveCategories = liveCategoriesDeferred.await()
        val liveStreamsJson = liveStreamsDeferred.await()
        val liveChannels = parseLiveChannels(liveStreamsJson, liveCategories)

        XtreamCatalog(
            sourceId = sourceId,
            sourceName = source.displayName,
            liveCategories = liveCategories,
            movieCategories = movieCategoriesDeferred.await(),
            seriesCategories = seriesCategoriesDeferred.await(),
            liveChannels = liveChannels,
            movies = moviesDeferred.await(),
            series = seriesDeferred.await()
        )
    }

    suspend fun loadEpisodes(seriesId: Int): List<XtreamEpisode> {
        return loadSeriesDetails(seriesId).episodes
    }

    suspend fun loadSeriesDetails(seriesId: Int): XtreamSeriesDetails {
        val json = request("get_series_info", mapOf("series_id" to seriesId.toString()))
        return parseSeriesDetails(json)
    }

    suspend fun loadMovieDetails(movieId: Int): XtreamMovieDetails {
        val json = request("get_vod_info", mapOf("vod_id" to movieId.toString()))
        return parseMovieDetails(json)
    }

    fun liveStreamUrl(streamId: Int, extension: String? = null): String {
        val ext = extension?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", true) } ?: "ts"
        return "$baseUrl/live/${path(source.username)}/${path(source.password)}/$streamId.$ext"
    }

    fun movieStreamUrl(movie: XtreamMovie): String =
        "$baseUrl/movie/${path(source.username)}/${path(source.password)}/${movie.id}.${path(movie.containerExtension)}"

    fun episodeStreamUrl(episode: XtreamEpisode): String =
        "$baseUrl/series/${path(source.username)}/${path(source.password)}/${path(episode.id)}.${path(episode.containerExtension)}"

    private suspend fun request(
        action: String?,
        extra: Map<String, String> = emptyMap(),
        forceRefresh: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val cacheFile = if (context != null && action != null) {
            val suffix = extra.entries.sortedBy { entry -> entry.key }.joinToString("_") { entry -> "${entry.key}_${entry.value}" }
            java.io.File(context.filesDir, "xtream_${sourceId}_${action}_${suffix}.json")
        } else null
        if (!forceRefresh && cacheFile != null && cacheFile.exists() && cacheFile.length() > 0L &&
            System.currentTimeMillis() - cacheFile.lastModified() < 24 * 60 * 60 * 1000L
        ) {
            require(cacheFile.length() <= MAX_RESPONSE_BYTES) { "Cached Xtream response exceeds the 64 MB safety limit" }
            return@withContext cacheFile.readText()
        }
        retryTransientNetwork {
            val parameters = linkedMapOf("username" to source.username, "password" to source.password)
            if (action != null) parameters["action"] = action
            parameters.putAll(extra)
            val query = parameters.entries.joinToString("&") { "${query(it.key)}=${query(it.value)}" }

            fun executeConnection(targetBaseUrl: String): String {
                val connection = URI("$targetBaseUrl/player_api.php?$query").toURL().openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connection.readTimeout = 45_000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "FortressTV")
                return try {
                    val code = connection.responseCode
                    if (code !in 200..299) throw HttpStatusException(code)
                    require(connection.contentLengthLong <= 0L || connection.contentLengthLong <= MAX_RESPONSE_BYTES) {
                        "Xtream response exceeds the 64 MB safety limit"
                    }
                    connection.inputStream.use { input -> readUtf8Limited(input, MAX_RESPONSE_BYTES) }.also { response ->
                        if (cacheFile != null) {
                            val sanitizedResponse = sanitizeCatalogResponse(response) ?: return@also
                            val temp = java.io.File(cacheFile.parentFile, "${cacheFile.name}.tmp")
                            temp.writeText(sanitizedResponse)
                            if (cacheFile.exists()) cacheFile.delete()
                            temp.renameTo(cacheFile)
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            }

            try {
                executeConnection(baseUrl)
            } catch (e: SSLException) {
                if (baseUrl.startsWith("https://", ignoreCase = true)) {
                    val httpUrl = baseUrl.replaceFirst("https://", "http://", ignoreCase = true)
                    baseUrl = httpUrl
                    executeConnection(httpUrl)
                } else {
                    throw e
                }
            }
        }
    }

    private fun readUtf8Limited(input: java.io.InputStream, limit: Long): String {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= limit) { "Xtream response exceeds the 64 MB safety limit" }
            output.write(buffer, 0, read)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    internal fun parseCategories(json: String): List<XtreamCategory> = JSONArray(json).objects().mapNotNull { item ->
        val id = item.optString("category_id")
        if (id.isBlank()) null else XtreamCategory(id, item.optString("category_name").ifBlank { id })
    }

    internal fun parseMovies(json: String): List<XtreamMovie> = JSONArray(json).objects().mapNotNull { item ->
        val id = item.optInt("stream_id", -1)
        if (id < 0) null else XtreamMovie(
            id = id,
            name = item.optString("name").ifBlank { "Movie $id" },
            categoryId = item.nullableString("category_id"),
            posterUrl = item.nullableString("stream_icon"),
            containerExtension = item.optString("container_extension").ifBlank { "mp4" },
            rating = item.nullableString("rating"),
            year = item.nullableString("year"),
            plot = item.nullableString("plot"),
            ageRating = item.nullableString("age") ?: item.nullableString("mpaa_rating")
        )
    }

    internal fun parseLiveChannels(
        json: String,
        categories: List<XtreamCategory> = emptyList()
    ): List<PlaylistChannel> {
        val categoryMap = categories.associate { it.id to it.name }
        return JSONArray(json).objects().mapIndexedNotNull { index, item ->
            val streamId = item.optInt("stream_id", item.optString("stream_id").toIntOrNull() ?: -1)
            if (streamId < 0) return@mapIndexedNotNull null
            val name = item.optString("name").ifBlank { "Channel $streamId" }
            val catId = item.nullableString("category_id")
            val group = catId?.let { categoryMap[it] ?: it }
            val logo = item.nullableString("stream_icon")
            val tvgId = item.nullableString("epg_channel_id") ?: item.nullableString("custom_sid")
            val tvgName = item.nullableString("name")
            val streamUrl = liveStreamUrl(streamId, item.nullableString("container_extension"))
            PlaylistChannel(
                position = index,
                name = name,
                streamUrl = streamUrl,
                tvgId = tvgId,
                tvgName = tvgName,
                logoUrl = logo,
                group = group,
                sourceId = sourceId,
                sourcePosition = index,
                sourceItemId = streamId.toString()
            )
        }
    }

    internal fun parseSeries(json: String): List<XtreamSeries> = JSONArray(json).objects().mapNotNull { item ->
        val id = item.optInt("series_id", -1)
        if (id < 0) null else XtreamSeries(
            id = id,
            name = item.optString("name").ifBlank { "Series $id" },
            categoryId = item.nullableString("category_id"),
            coverUrl = item.nullableString("cover"),
            rating = item.nullableString("rating"),
            year = item.nullableString("releaseDate") ?: item.nullableString("year"),
            plot = item.nullableString("plot"),
            ageRating = item.nullableString("age") ?: item.nullableString("mpaa_rating")
        )
    }

    internal fun parseMovieDetails(json: String): XtreamMovieDetails {
        val root = JSONObject(json)
        val info = root.optJSONObject("info") ?: JSONObject()
        val movieData = root.optJSONObject("movie_data") ?: JSONObject()
        fun value(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
            info.nullableString(key) ?: movieData.nullableString(key)
        }
        val backdrop = info.optJSONArray("backdrop_path")?.let { paths ->
            (0 until paths.length()).firstNotNullOfOrNull { index ->
                paths.optString(index).trim().takeIf(String::isNotEmpty)
            }
        } ?: value("backdrop_path")
        return XtreamMovieDetails(
            plot = value("plot", "description"),
            genre = value("genre"),
            duration = value("duration", "episode_run_time"),
            releaseDate = value("releasedate", "releaseDate", "year"),
            rating = value("rating", "rating_5based"),
            director = value("director"),
            cast = value("cast", "actors"),
            country = value("country"),
            ageRating = value("age", "mpaa_rating"),
            posterUrl = value("movie_image", "cover_big", "stream_icon"),
            backdropUrl = backdrop,
            trailerUrl = value("youtube_trailer", "trailer")
        )
    }

    internal fun parseSeriesDetails(json: String): XtreamSeriesDetails {
        val root = JSONObject(json)
        val info = root.optJSONObject("info") ?: JSONObject()
        fun value(vararg keys: String): String? = keys.firstNotNullOfOrNull { key -> info.nullableString(key) }
        val backdrop = info.optJSONArray("backdrop_path")?.let { paths ->
            (0 until paths.length()).firstNotNullOfOrNull { index ->
                paths.optString(index).trim().takeIf(String::isNotEmpty)
            }
        } ?: value("backdrop_path")
        return XtreamSeriesDetails(
            plot = value("plot", "description"),
            genre = value("genre"),
            releaseDate = value("releaseDate", "releasedate", "year"),
            rating = value("rating", "rating_5based"),
            director = value("director"),
            cast = value("cast", "actors"),
            duration = value("episode_run_time", "duration"),
            country = value("country"),
            ageRating = value("age", "mpaa_rating"),
            coverUrl = value("cover", "cover_big", "movie_image"),
            backdropUrl = backdrop,
            trailerUrl = value("youtube_trailer", "trailer"),
            episodes = parseEpisodes(json)
        )
    }

    internal fun parseEpisodes(json: String): List<XtreamEpisode> {
        val root = JSONObject(json)
        val episodes = root.optJSONObject("episodes") ?: return emptyList()
        return episodes.keys().asSequence().flatMap { seasonKey ->
            val season = seasonKey.toIntOrNull() ?: 0
            val values = episodes.optJSONArray(seasonKey) ?: JSONArray()
            (0 until values.length()).asSequence().mapNotNull { index ->
                val item = values.optJSONObject(index) ?: return@mapNotNull null
                val info = item.optJSONObject("info")
                val epId = item.optString("id").ifBlank { item.optString("stream_id") }
                if (epId.isBlank()) return@mapNotNull null
                XtreamEpisode(
                    id = epId,
                    title = item.optString("title").ifBlank { "Episode ${item.optInt("episode_num", index + 1)}" },
                    season = item.optInt("season", season),
                    episodeNumber = item.optInt("episode_num", index + 1),
                    containerExtension = item.optString("container_extension").ifBlank { "mp4" },
                    posterUrl = info?.nullableString("movie_image"),
                    plot = info?.nullableString("plot")
                )
            }
        }.sortedWith(compareBy<XtreamEpisode> { it.season }.thenBy { it.episodeNumber }).toList()
    }

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).mapNotNull(::optJSONObject)

    internal fun sanitizeCatalogResponse(response: String): String? = sanitizeCachedResponse(response)

    internal fun sanitizeCachedResponse(response: String): String? = runCatching {
        val root = JSONTokener(response).nextValue()
        removeSensitiveStreamFields(root)
        when (root) {
            is JSONObject, is JSONArray -> root.toString()
            else -> null
        }
    }.getOrNull()

    private fun removeSensitiveStreamFields(value: Any?) {
        when (value) {
            is JSONObject -> {
                value.remove("direct_source")
                value.keys().asSequence().toList().forEach { key ->
                    removeSensitiveStreamFields(value.opt(key))
                }
            }
            is JSONArray -> (0 until value.length()).forEach { index ->
                removeSensitiveStreamFields(value.opt(index))
            }
        }
    }

    private fun JSONObject.nullableString(key: String): String? =
        optString(key).trim().takeIf { it.isNotEmpty() && !it.equals("null", true) }

    private fun query(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    private fun path(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    companion object {
        private const val MAX_RESPONSE_BYTES = 64L * 1024L * 1024L

        fun normalizeServerUrl(value: String): String {
            val trimmed = value.trim().substringBefore('?').removeSuffix("/player_api.php").trimEnd('/')
            val withScheme = if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
                "http://$trimmed"
            } else trimmed
            val uri = URI(withScheme)
            require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
                "Xtream server must use HTTP or HTTPS"
            }
            require(!uri.host.isNullOrBlank()) { "Enter a valid Xtream server URL" }
            return withScheme
        }
    }
}

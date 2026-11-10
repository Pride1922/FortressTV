package com.fortresstv.player.xtream

import android.content.Context
import android.os.Trace
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.source.HttpStatusException
import com.fortresstv.player.source.retryTransientNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import javax.net.ssl.SSLException

class XtreamClient(
    private val context: Context? = null,
    private val sourceId: String,
    private val source: ContentSource.Xtream
) {
    @Volatile
    private var baseUrl = normalizeServerUrl(source.value)

    // Keep live caches from a previous endpoint or login out of an edited source.
    private val liveCacheKey by lazy {
        val identity = listOf(source.value, source.username, source.password).joinToString("\u0000")
        MessageDigest.getInstance("SHA-256").digest(identity.toByteArray()).joinToString("") { "%02x".format(it) }
    }

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

    suspend fun loadCatalog(
        forceRefresh: Boolean = false,
        onLiveLoaded: suspend (XtreamCatalog) -> Unit = {}
    ): XtreamCatalog {
        val cachedLive = if (forceRefresh) null else loadCachedLive()
        if (cachedLive != null && cachedLive.catalog.liveChannels.isNotEmpty()) {
            onLiveLoaded(cachedLive.catalog)
        }
        authenticate()
        val live = if (cachedLive?.fresh == true && cachedLive.streamBaseUrl == baseUrl) cachedLive.catalog else loadLive()
        if (live !== cachedLive?.catalog && live.liveChannels.isNotEmpty()) onLiveLoaded(live)

        return coroutineScope {
            val movieCategories = async { parseCategories(request("get_vod_categories", forceRefresh = forceRefresh)) }
            val seriesCategories = async { parseCategories(request("get_series_categories", forceRefresh = forceRefresh)) }
            val movies = async { parseMovies(request("get_vod_streams", forceRefresh = forceRefresh)) }
            val series = async { parseSeries(request("get_series", forceRefresh = forceRefresh)) }
            live.copy(
                movieCategories = movieCategories.await(),
                seriesCategories = seriesCategories.await(),
                movies = movies.await(),
                series = series.await()
            )
        }
    }

    private data class CachedLive(val catalog: XtreamCatalog, val streamBaseUrl: String, val fresh: Boolean)

    private suspend fun loadCachedLive(): CachedLive? = withContext(Dispatchers.IO) {
        val categoriesFile = cacheFile("get_live_categories", emptyMap()) ?: return@withContext null
        val streamsFile = cacheFile("get_live_streams", emptyMap()) ?: return@withContext null
        Trace.beginSection("FortressTV.restoreXtreamLive")
        try {
            val categories = readCache(categoriesFile, allowStale = true) ?: return@withContext null
            val response = readCache(streamsFile, allowStale = true) ?: return@withContext null
            val scheme = response.substringBefore('\n')
            if (scheme != "http" && scheme != "https") return@withContext null
            val cachedBaseUrl = "$scheme://${baseUrl.substringAfter("://")}"
            runCatching {
                CachedLive(
                    liveCatalog(categories, response.substringAfter('\n'), cachedBaseUrl),
                    cachedBaseUrl,
                    isFresh(categoriesFile) && isFresh(streamsFile)
                )
            }.getOrNull()
        } finally {
            Trace.endSection()
        }
    }

    private suspend fun loadLive(): XtreamCatalog = coroutineScope {
        val categories = async { request("get_live_categories", forceRefresh = true) }
        val streams = async { request("get_live_streams", forceRefresh = true) }
        liveCatalog(categories.await(), streams.await())
    }

    private fun liveCatalog(categoriesJson: String, streamsJson: String, streamBaseUrl: String = baseUrl): XtreamCatalog {
        val categories = parseCategories(categoriesJson)
        return XtreamCatalog(
            sourceId = sourceId,
            sourceName = source.displayName,
            liveCategories = categories,
            liveChannels = parseLiveChannels(streamsJson, categories, streamBaseUrl)
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

    fun liveStreamUrl(streamId: Int, extension: String? = null): String = liveStreamUrl(baseUrl, streamId, extension)

    private fun liveStreamUrl(streamBaseUrl: String, streamId: Int, extension: String?): String {
        val ext = extension?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", true) } ?: "ts"
        return "$streamBaseUrl/live/${path(source.username)}/${path(source.password)}/$streamId.$ext"
    }

    fun movieStreamUrl(movie: XtreamMovie): String =
        "$baseUrl/movie/${path(source.username)}/${path(source.password)}/${movie.id}.${path(movie.containerExtension)}"

    fun episodeStreamUrl(episode: XtreamEpisode): String =
        "$baseUrl/series/${path(source.username)}/${path(source.password)}/${path(episode.id)}.${path(episode.containerExtension)}"

    fun timeshiftStreamUrl(
        streamId: Int,
        durationMinutes: Int,
        startFormatted: String,
        extension: String? = null
    ): String {
        val ext = extension?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", true) } ?: "ts"
        return "$baseUrl/timeshift/${path(source.username)}/${path(source.password)}/$durationMinutes/$startFormatted/$streamId.$ext"
    }

    private suspend fun request(
        action: String?,
        extra: Map<String, String> = emptyMap(),
        forceRefresh: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val cacheFile = action?.let { cacheFile(it, extra) }
        if (!forceRefresh && cacheFile != null) readCache(cacheFile)?.let { return@withContext it }
        val requestContext = currentCoroutineContext()
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
                            requestContext.ensureActive()
                            val sanitizedResponse = sanitizeCatalogResponse(response) ?: return@also
                            runCatching {
                                val temp = File.createTempFile("xtream_", ".tmp", cacheFile.parentFile)
                                try {
                                    temp.bufferedWriter().use { writer ->
                                        if (action == "get_live_streams") {
                                            writer.appendLine(targetBaseUrl.substringBefore("://").lowercase())
                                        }
                                        writer.write(sanitizedResponse)
                                    }
                                    Files.move(temp.toPath(), cacheFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                                } finally {
                                    temp.delete()
                                }
                            }
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

    private fun cacheFile(action: String, extra: Map<String, String>): File? {
        val directory = context?.filesDir ?: return null
        val suffix = if (action == "get_live_categories" || action == "get_live_streams") {
            liveCacheKey
        } else {
            extra.toSortedMap().entries.joinToString("_") { "${it.key}_${it.value}" }
        }
        return File(directory, "xtream_${sourceId}_${action}_${suffix}.json")
    }

    private fun isFresh(file: File): Boolean =
        System.currentTimeMillis() - file.lastModified() < 24 * 60 * 60 * 1000L

    private fun readCache(file: File, allowStale: Boolean = false): String? {
        if ((!allowStale && !isFresh(file)) || file.length() !in 1L..MAX_CACHE_BYTES) return null
        return runCatching {
            file.inputStream().use { readUtf8Limited(it, MAX_CACHE_BYTES) }
        }.getOrNull()
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
            posterUrl = item.nullableString("stream_icon")
                ?: item.nullableString("cover_big")
                ?: item.nullableString("movie_image")
                ?: item.nullableString("cover"),
            containerExtension = item.optString("container_extension").ifBlank { "mp4" },
            rating = item.nullableString("rating"),
            year = item.nullableString("year"),
            plot = item.nullableString("plot"),
            ageRating = item.nullableString("age") ?: item.nullableString("mpaa_rating"),
            tmdbId = item.nullableString("tmdb_id") ?: item.nullableString("tmdb"),
            imdbId = item.nullableString("imdb_id") ?: item.nullableString("imdb")
        )
    }

    internal fun parseLiveChannels(
        json: String,
        categories: List<XtreamCategory> = emptyList(),
        streamBaseUrl: String = baseUrl
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
            val streamUrl = liveStreamUrl(streamBaseUrl, streamId, item.nullableString("container_extension"))
            val tvArchive = item.optInt("tv_archive", 0) == 1 || item.optString("tv_archive") == "1"
            val archiveDuration = item.optInt("tv_archive_duration", item.optString("tv_archive_duration").toIntOrNull() ?: 0)
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
                sourceItemId = streamId.toString(),
                hasCatchup = tvArchive,
                catchupDays = archiveDuration
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
            ageRating = item.nullableString("age") ?: item.nullableString("mpaa_rating"),
            tmdbId = item.nullableString("tmdb_id") ?: item.nullableString("tmdb"),
            imdbId = item.nullableString("imdb_id") ?: item.nullableString("imdb")
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
            trailerUrl = value("youtube_trailer", "trailer"),
            tmdbId = value("tmdb_id", "tmdb"),
            imdbId = value("imdb_id", "imdb")
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
        val seasonsArr = root.optJSONArray("seasons")
        val seasonOverviews = mutableMapOf<Int, String>()
        if (seasonsArr != null) {
            for (i in 0 until seasonsArr.length()) {
                val sObj = seasonsArr.optJSONObject(i) ?: continue
                val sNum = sObj.optInt("season_number", -1)
                val sOverview = sObj.nullableString("overview")
                    ?: sObj.nullableString("plot")
                    ?: sObj.nullableString("description")
                if (sNum >= 0 && !sOverview.isNullOrBlank()) {
                    seasonOverviews[sNum] = sOverview
                }
            }
        }
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
            episodes = parseEpisodes(json, seasonOverviews),
            tmdbId = value("tmdb_id", "tmdb"),
            imdbId = value("imdb_id", "imdb"),
            seasonOverviews = seasonOverviews
        )
    }

    internal fun parseEpisodes(json: String, seasonOverviews: Map<Int, String> = emptyMap()): List<XtreamEpisode> {
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
                val epPoster = info?.nullableString("movie_image")
                    ?: info?.nullableString("cover")
                    ?: info?.nullableString("image")
                    ?: info?.nullableString("screenshot_uri")
                    ?: item.nullableString("movie_image")
                    ?: item.nullableString("cover")
                val epPlot = info?.nullableString("plot")
                    ?: info?.nullableString("overview")
                    ?: info?.nullableString("description")
                    ?: info?.nullableString("synopsis")
                    ?: item.nullableString("plot")
                    ?: item.nullableString("overview")
                    ?: item.nullableString("description")
                    ?: item.nullableString("synopsis")
                    ?: seasonOverviews[season]
                val epDuration = info?.optInt("duration_secs", 0)?.takeIf { it > 0 }
                    ?: info?.optInt("duration", 0)?.takeIf { it > 0 }
                    ?: item.optInt("duration_secs", 0)?.takeIf { it > 0 }
                val epRating = info?.nullableString("rating")
                    ?: info?.nullableString("rating_5based")
                XtreamEpisode(
                    id = epId,
                    title = item.optString("title").ifBlank { "Episode ${item.optInt("episode_num", index + 1)}" },
                    season = item.optInt("season", season),
                    episodeNumber = item.optInt("episode_num", index + 1),
                    containerExtension = item.optString("container_extension").ifBlank { "mp4" },
                    posterUrl = epPoster,
                    plot = epPlot,
                    durationSecs = epDuration,
                    rating = epRating
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
        private const val MAX_CACHE_BYTES = MAX_RESPONSE_BYTES + 6 // Live streams include a protocol line.

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

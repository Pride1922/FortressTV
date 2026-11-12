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
    private val source: ContentSource.Xtream,
    private val cacheDirectory: File? = context?.filesDir
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

    /** A catalogue read only from this device's cache, with whether every part is under 24 h old. */
    data class CachedCatalog(val catalog: XtreamCatalog, val fresh: Boolean)

    /**
     * Reads the whole catalogue from the on-device cache without touching the network, or returns
     * null if any part has never been cached. Stale parts are returned too, so the UI can show
     * them immediately (also when offline or when the provider is down) while a refresh runs.
     */
    suspend fun loadCachedCatalog(): CachedCatalog? = withContext(Dispatchers.IO) {
        traced("FortressTV.restoreXtreamCatalog") {
            val live = loadCachedLive() ?: return@traced null
            val movieCategories = cachedList(ACTION_VOD_CATEGORIES, XtreamCatalogSnapshot::readCategories) ?: return@traced null
            val seriesCategories = cachedList(ACTION_SERIES_CATEGORIES, XtreamCatalogSnapshot::readCategories) ?: return@traced null
            val movies = cachedList(ACTION_VOD_STREAMS, XtreamCatalogSnapshot::readMovies) ?: return@traced null
            val series = cachedList(ACTION_SERIES, XtreamCatalogSnapshot::readSeries) ?: return@traced null
            CachedCatalog(
                catalog = live.catalog.copy(
                    movieCategories = movieCategories.first,
                    seriesCategories = seriesCategories.first,
                    movies = movies.first,
                    series = series.first
                ),
                fresh = live.fresh && live.streamBaseUrl == baseUrl &&
                    movieCategories.second && seriesCategories.second && movies.second && series.second
            )
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
            val movieCategories = async { categories(ACTION_VOD_CATEGORIES, forceRefresh) }
            val seriesCategories = async { categories(ACTION_SERIES_CATEGORIES, forceRefresh) }
            val movies = async {
                freshCache(ACTION_VOD_STREAMS, forceRefresh, XtreamCatalogSnapshot::readMovies)
                    ?: fetchList(ACTION_VOD_STREAMS, { parseMovies(it) }) { file, value, _ -> XtreamCatalogSnapshot.writeMovies(file, value) }
            }
            val series = async {
                freshCache(ACTION_SERIES, forceRefresh, XtreamCatalogSnapshot::readSeries)
                    ?: fetchList(ACTION_SERIES, { parseSeries(it) }) { file, value, _ -> XtreamCatalogSnapshot.writeSeries(file, value) }
            }
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
        traced("FortressTV.restoreXtreamLive") {
            val categories = cachedList(ACTION_LIVE_CATEGORIES, XtreamCatalogSnapshot::readCategories) ?: return@traced null
            val streamsFile = cacheFile(ACTION_LIVE_STREAMS) ?: return@traced null
            val (scheme, records) = XtreamCatalogSnapshot.readLive(streamsFile) ?: return@traced null
            val cachedBaseUrl = "$scheme://${baseUrl.substringAfter("://")}"
            CachedLive(
                liveCatalog(categories.first, records, cachedBaseUrl),
                cachedBaseUrl,
                categories.second && isFresh(streamsFile)
            )
        }
    }

    private suspend fun loadLive(): XtreamCatalog = coroutineScope {
        val categories = async { fetchCategories(ACTION_LIVE_CATEGORIES) }
        val streams = async {
            fetchList(ACTION_LIVE_STREAMS, { parseLiveRecords(it) }) { file, value, targetBaseUrl ->
                XtreamCatalogSnapshot.writeLive(file, targetBaseUrl.substringBefore("://").lowercase(), value)
            }
        }
        // baseUrl is read after both downloads: an https failure may have switched it to http.
        val categoryList = categories.await()
        val records = streams.await()
        liveCatalog(categoryList, records, baseUrl)
    }

    private fun liveCatalog(categories: List<XtreamCategory>, records: List<LiveStreamRecord>, streamBaseUrl: String): XtreamCatalog =
        XtreamCatalog(
            sourceId = sourceId,
            sourceName = source.displayName,
            liveCategories = categories,
            liveChannels = liveChannels(records, categories, streamBaseUrl)
        )

    private suspend fun categories(action: String, forceRefresh: Boolean): List<XtreamCategory> =
        freshCache(action, forceRefresh, XtreamCatalogSnapshot::readCategories) ?: fetchCategories(action)

    /** The cached value if it is under 24 h old and no refresh was forced; otherwise null (download it). */
    private fun <T> freshCache(action: String, forceRefresh: Boolean, reader: (File) -> T?): T? =
        if (forceRefresh) null else cachedList(action, reader, freshOnly = true)?.first

    private suspend fun fetchCategories(action: String): List<XtreamCategory> =
        fetchList(action, { parseCategories(it) }) { file, value, _ -> XtreamCatalogSnapshot.writeCategories(file, value) }

    /**
     * Reads a cached list snapshot. Returns the value and whether it is fresh, or null if there is no
     * usable cache (or, with [freshOnly], if it is older than 24 h).
     */
    private fun <T> cachedList(action: String, reader: (File) -> T?, freshOnly: Boolean = false): Pair<T, Boolean>? {
        val file = cacheFile(action) ?: return null
        val fresh = isFresh(file)
        if (freshOnly && !fresh) return null
        val value = reader(file) ?: return null
        return value to fresh
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
        val cacheFile = action?.let { detailCacheFile(it, extra) }
        if (!forceRefresh && cacheFile != null) readCache(cacheFile)?.let { return@withContext it }
        val requestContext = currentCoroutineContext()
        fetch(action, extra) { input, _ ->
            readUtf8Limited(input, MAX_RESPONSE_BYTES).also { response ->
                if (cacheFile != null) {
                    requestContext.ensureActive()
                    val sanitizedResponse = sanitizeCatalogResponse(response) ?: return@also
                    runCatching {
                        val temp = File.createTempFile("xtream_", ".tmp", cacheFile.parentFile)
                        try {
                            temp.bufferedWriter().use { writer -> writer.write(sanitizedResponse) }
                            Files.move(temp.toPath(), cacheFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                        } finally {
                            temp.delete()
                        }
                    }
                }
            }
        }
    }

    /**
     * Downloads a list endpoint and parses it while it streams in, then stores the parsed result as a
     * snapshot. The response is never held in memory as one string, and no raw JSON (which can contain
     * direct_source URLs) is written to disk.
     */
    private suspend fun <T> fetchList(
        action: String,
        parse: (java.io.Reader) -> T,
        save: (file: File, value: T, targetBaseUrl: String) -> Unit
    ): T = withContext(Dispatchers.IO) {
        val requestContext = currentCoroutineContext()
        fetch(action, emptyMap()) { input, targetBaseUrl ->
            val value = LimitedInputStream(input, MAX_RESPONSE_BYTES).reader(Charsets.UTF_8).buffered(64 * 1024).use(parse)
            requestContext.ensureActive()
            cacheFile(action)?.let { file ->
                runCatching { save(file, value, targetBaseUrl) }
                    .onSuccess { deleteOutdatedCaches(action, keep = file) }
            }
            value
        }
    }

    /** Performs the HTTP request (with retries and the https-to-http fallback) and hands over the body. */
    private suspend fun <T> fetch(
        action: String?,
        extra: Map<String, String>,
        consume: (input: java.io.InputStream, targetBaseUrl: String) -> T
    ): T = retryTransientNetwork {
        val parameters = linkedMapOf("username" to source.username, "password" to source.password)
        if (action != null) parameters["action"] = action
        parameters.putAll(extra)
        val query = parameters.entries.joinToString("&") { "${query(it.key)}=${query(it.value)}" }

        fun executeConnection(targetBaseUrl: String): T {
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
                connection.inputStream.use { input -> consume(input, targetBaseUrl) }
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

    /** Snapshot file for a list endpoint, bound to this server and login so an edited source never shows old data. */
    private fun cacheFile(action: String): File? {
        val directory = cacheDirectory ?: return null
        return File(directory, "xtream_${sourceId}_${action}_${liveCacheKey}.bin")
    }

    /** Raw-JSON cache for detail endpoints (one movie or series). */
    private fun detailCacheFile(action: String, extra: Map<String, String>): File? {
        val directory = cacheDirectory ?: return null
        val suffix = extra.toSortedMap().entries.joinToString("_") { "${it.key}_${it.value}" }
        return File(directory, "xtream_${sourceId}_${action}_${suffix}.json")
    }

    /**
     * Once a snapshot is saved, removes what it replaces: the multi-MB JSON list caches of earlier
     * versions, and snapshots made with a previous server address or login for this source.
     */
    private fun deleteOutdatedCaches(action: String, keep: File) {
        val directory = cacheDirectory ?: return
        val prefix = Regex.escape("xtream_${sourceId}_${action}_")
        // Old list caches were "<action>_.json", or "<action>_<64 hex>.json" for live lists. The exact
        // patterns keep detail caches such as get_series_info_series_id_5.json untouched.
        val outdated = Regex("^$prefix(([0-9a-f]{64})?\\.json|[0-9a-f]{64}\\.bin)$")
        directory.listFiles { file -> file.name != keep.name && outdated.matches(file.name) }?.forEach { it.delete() }
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

    internal fun parseCategories(json: String): List<XtreamCategory> = parseCategories(java.io.StringReader(json))

    internal fun parseCategories(reader: java.io.Reader): List<XtreamCategory> {
        val categories = ArrayList<XtreamCategory>()
        XtreamJsonStream.forEachObject(reader) { _, item ->
            val id = item.optString("category_id")
            if (id.isNotBlank()) categories += XtreamCategory(id, item.optString("category_name").ifBlank { id })
        }
        return categories
    }

    internal fun parseMovies(json: String): List<XtreamMovie> = parseMovies(java.io.StringReader(json))

    internal fun parseMovies(reader: java.io.Reader): List<XtreamMovie> {
        val movies = ArrayList<XtreamMovie>()
        XtreamJsonStream.forEachObject(reader) { _, item ->
            val id = item.optInt("stream_id", -1)
            if (id >= 0) movies += XtreamMovie(
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
        return movies
    }

    internal fun parseLiveChannels(
        json: String,
        categories: List<XtreamCategory> = emptyList(),
        streamBaseUrl: String = baseUrl
    ): List<PlaylistChannel> = liveChannels(parseLiveRecords(java.io.StringReader(json)), categories, streamBaseUrl)

    internal fun parseLiveRecords(reader: java.io.Reader): List<LiveStreamRecord> {
        val records = ArrayList<LiveStreamRecord>()
        XtreamJsonStream.forEachObject(reader) { index, item ->
            val streamId = item.optInt("stream_id", item.optString("stream_id").toIntOrNull() ?: -1)
            if (streamId < 0) return@forEachObject
            records += LiveStreamRecord(
                position = index,
                streamId = streamId,
                name = item.optString("name").ifBlank { "Channel $streamId" },
                categoryId = item.nullableString("category_id"),
                logoUrl = item.nullableString("stream_icon"),
                tvgId = item.nullableString("epg_channel_id") ?: item.nullableString("custom_sid"),
                tvgName = item.nullableString("name"),
                containerExtension = item.nullableString("container_extension"),
                hasCatchup = item.optInt("tv_archive", 0) == 1 || item.optString("tv_archive") == "1",
                catchupDays = item.optInt("tv_archive_duration", item.optString("tv_archive_duration").toIntOrNull() ?: 0)
            )
        }
        return records
    }

    private fun liveChannels(
        records: List<LiveStreamRecord>,
        categories: List<XtreamCategory>,
        streamBaseUrl: String
    ): List<PlaylistChannel> {
        val categoryMap = categories.associate { it.id to it.name }
        return records.map { record ->
            PlaylistChannel(
                position = record.position,
                name = record.name,
                streamUrl = liveStreamUrl(streamBaseUrl, record.streamId, record.containerExtension),
                tvgId = record.tvgId,
                tvgName = record.tvgName,
                logoUrl = record.logoUrl,
                group = record.categoryId?.let { categoryMap[it] ?: it },
                sourceId = sourceId,
                sourcePosition = record.position,
                sourceItemId = record.streamId.toString(),
                hasCatchup = record.hasCatchup,
                catchupDays = record.catchupDays
            )
        }
    }

    internal fun parseSeries(json: String): List<XtreamSeries> = parseSeries(java.io.StringReader(json))

    internal fun parseSeries(reader: java.io.Reader): List<XtreamSeries> {
        val series = ArrayList<XtreamSeries>()
        XtreamJsonStream.forEachObject(reader) { _, item ->
            val id = item.optInt("series_id", -1)
            if (id >= 0) series += XtreamSeries(
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
        return series
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
        private const val MAX_CACHE_BYTES = MAX_RESPONSE_BYTES
        private const val ACTION_LIVE_CATEGORIES = "get_live_categories"
        private const val ACTION_LIVE_STREAMS = "get_live_streams"
        private const val ACTION_VOD_CATEGORIES = "get_vod_categories"
        private const val ACTION_SERIES_CATEGORIES = "get_series_categories"
        private const val ACTION_VOD_STREAMS = "get_vod_streams"
        private const val ACTION_SERIES = "get_series"

        /** android.os.Trace, but harmless where it is unavailable (JVM unit tests). */
        private inline fun <T> traced(section: String, block: () -> T): T {
            val started = runCatching { Trace.beginSection(section) }.isSuccess
            try {
                return block()
            } finally {
                if (started) runCatching { Trace.endSection() }
            }
        }

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

/** Fails once more than [limit] bytes have been read, so a hostile server cannot exhaust storage or memory. */
private class LimitedInputStream(input: java.io.InputStream, private val limit: Long) : java.io.FilterInputStream(input) {
    private var total = 0L

    override fun read(): Int = super.read().also { if (it >= 0) count(1) }

    override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also { if (it > 0) count(it) }

    override fun skip(n: Long): Long = super.skip(n).also { count(it) }

    private fun count(bytes: Number) {
        total += bytes.toLong()
        if (total > limit) throw java.io.IOException("Xtream response exceeds the 64 MB safety limit")
    }
}

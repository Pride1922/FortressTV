package com.fortresstv.player.xtream

import android.content.Context
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.source.ContentSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class XtreamClient(
    private val context: Context? = null,
    private val sourceId: String,
    private val source: ContentSource.Xtream
) {
    private val baseUrl = normalizeServerUrl(source.value)

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
        val json = request("get_series_info", mapOf("series_id" to seriesId.toString()))
        return parseEpisodes(json)
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
        val cacheFile = if (context != null && action != null && action != "get_series_info") {
            val suffix = extra.entries.sortedBy { entry -> entry.key }.joinToString("_") { entry -> "${entry.key}_${entry.value}" }
            java.io.File(context.filesDir, "xtream_${sourceId}_${action}_${suffix}.json")
        } else null
        if (!forceRefresh && cacheFile != null && cacheFile.exists() && cacheFile.length() > 0L &&
            System.currentTimeMillis() - cacheFile.lastModified() < 24 * 60 * 60 * 1000L
        ) {
            return@withContext cacheFile.readText()
        }
        val parameters = linkedMapOf("username" to source.username, "password" to source.password)
        if (action != null) parameters["action"] = action
        parameters.putAll(extra)
        val query = parameters.entries.joinToString("&") { "${query(it.key)}=${query(it.value)}" }
        val connection = URI("$baseUrl/player_api.php?$query").toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 45_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "FortressTV")
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("Xtream server returned HTTP $code")
            connection.inputStream.bufferedReader().use { it.readText() }.also { response ->
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
            plot = item.nullableString("plot")
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
            plot = item.nullableString("plot")
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

    internal fun sanitizeCatalogResponse(response: String): String? = runCatching {
        val array = JSONArray(response)
        for (index in 0 until array.length()) {
            array.optJSONObject(index)?.apply {
                remove("direct_source")
            }
        }
        array.toString()
    }.getOrNull()

    private fun JSONObject.nullableString(key: String): String? =
        optString(key).trim().takeIf { it.isNotEmpty() && !it.equals("null", true) }

    private fun query(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    private fun path(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    companion object {
        fun normalizeServerUrl(value: String): String {
            val trimmed = value.trim().substringBefore('?').removeSuffix("/player_api.php").trimEnd('/')
            val uri = URI(trimmed)
            require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
                "Xtream server must use HTTP or HTTPS"
            }
            require(!uri.host.isNullOrBlank()) { "Enter a valid Xtream server URL" }
            return trimmed
        }
    }
}

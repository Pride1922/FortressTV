package com.fortresstv.player

import android.content.Context
import android.os.Trace
import com.fortresstv.player.library.CloudIdentityHasher
import com.fortresstv.player.library.UserLibraryStore
import com.fortresstv.player.library.UserLibraryStore.Companion.libraryId
import com.fortresstv.player.library.VodProgressEntry
import com.fortresstv.player.library.VodProgressKind
import com.fortresstv.player.library.VodProgressStore
import com.fortresstv.player.playlist.M3uParser
import com.fortresstv.player.playlist.Playlist
import com.fortresstv.player.playlist.PlaylistDiskCache
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.source.SourceReader
import com.fortresstv.player.source.SourceSettingsStore
import com.fortresstv.player.source.isTransientNetworkFailure
import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamClient
import com.fortresstv.player.xtream.XtreamMovie
import com.fortresstv.player.xtream.XtreamSeries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.File
import java.util.Locale

/**
 * Workflow owner for playlist parsing, Xtream catalog synchronization,
 * cached reading, and favorites/watchlist identity reconnection.
 */
internal class SourceSyncWorkflow(
    private val context: Context,
    private val settings: SourceSettingsStore,
    private val userLibrary: UserLibraryStore,
    private val vodProgressStore: VodProgressStore,
    private val scope: CoroutineScope
) {
    private val playlistRetryJobs = mutableMapOf<String, Job>()

    suspend fun parsePlaylist(stream: InputStream): Playlist = withContext(Dispatchers.Default) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        SizeLimitedInputStream(stream, 100L * 1024L * 1024L, "Playlist").bufferedReader().use {
            M3uParser.parse(it)
        }
    }

    suspend fun parsePlaylistWithCache(
        profile: PlaylistProfile,
        cacheType: String,
        stream: InputStream,
        allowCachedResult: Boolean
    ): Playlist = withContext(Dispatchers.IO) {
        val fingerprint = SourceReader.fingerprint(context, profile.source, cacheType)
        if (allowCachedResult) {
            Trace.beginSection("FortressTV.restoreM3u")
            val cached = try {
                PlaylistDiskCache.load(context, profile.id, fingerprint)
            } finally {
                Trace.endSection()
            }
            if (cached != null) {
                stream.close()
                return@withContext cached
            }
        }
        val parsed = parsePlaylist(stream)
        PlaylistDiskCache.save(context, profile.id, fingerprint, parsed)
        parsed
    }

    suspend fun reconnectFavouritesAndProgress(
        sourceId: String,
        catalog: XtreamCatalog,
        playlists: List<PlaylistProfile>,
        shouldApply: () -> Boolean,
        inferNextEpisodes: Boolean,
        onStateUpdated: () -> Unit = {}
    ) = withContext(Dispatchers.Default) {
        val originalFavs = userLibrary.favoriteChannelIds()
        val originalWatchlist = userLibrary.movieWatchlistIds()
        val originalFollowed = userLibrary.followedSeriesIds()
        val originalWatched = userLibrary.watchedItemIds()
        val favs = originalFavs.toMutableSet()
        val watchlist = originalWatchlist.toMutableSet()
        val followed = originalFollowed.toMutableSet()
        val watched = originalWatched.toMutableSet()

        val channelPrefix = "channel:$sourceId:"
        val moviePrefix = "movie:$sourceId:"
        val seriesPrefix = "series:$sourceId:"

        val hasHashedChannels = favs.any { it.startsWith("channel:") && it.length > 40 }
        if (hasHashedChannels) {
            for (channel in catalog.liveChannels) {
                val hash = CloudIdentityHasher.hashItemId(channel.sourceItemId)
                if (favs.contains("$channelPrefix$hash") || favs.any { it.endsWith(":$hash") }) {
                    favs.add("$channelPrefix${channel.sourceItemId}")
                }
            }
        }

        val hasHashedMovies = watchlist.any { it.startsWith("movie:") && it.length > 38 } ||
                watched.any { it.startsWith("movie:") && it.length > 38 }
        if (hasHashedMovies) {
            for (movie in catalog.movies) {
                val hash = CloudIdentityHasher.hashItemId(movie.id.toString())
                val movieKey = "$moviePrefix$hash"
                if (watchlist.contains(movieKey) || watchlist.any { it.endsWith(":$hash") }) {
                    watchlist.add("$moviePrefix${movie.id}")
                }
                if (watched.contains(movieKey) || watched.any { it.endsWith(":$hash") }) {
                    watched.add("$moviePrefix${movie.id}")
                }
            }
        }

        val hasHashedSeries = followed.any { it.startsWith("series:") && it.length > 39 }
        if (hasHashedSeries) {
            for (series in catalog.series) {
                val hash = CloudIdentityHasher.hashItemId(series.id.toString())
                if (followed.contains("$seriesPrefix$hash") || followed.any { it.endsWith(":$hash") }) {
                    followed.add("$seriesPrefix${series.id}")
                }
            }
        }

        val originalEntries = vodProgressStore.entries()
        val currentEntries = originalEntries.toMutableList()
        val movieMap = mutableMapOf<String, XtreamMovie>()
        for (movie in catalog.movies) {
            movieMap[movie.id.toString()] = movie
        }
        val seriesMap = mutableMapOf<String, XtreamSeries>()
        for (series in catalog.series) {
            seriesMap[series.id.toString()] = series
        }

        var progressChanged = false
        for (i in currentEntries.indices) {
            val entry = currentEntries[i]
            if (entry.sourceId != sourceId) continue
            if (entry.kind == VodProgressKind.MOVIE && (entry.title.isBlank() || entry.posterUrl.isNullOrBlank())) {
                var matchingMovie = movieMap[entry.itemId]
                if (matchingMovie == null) {
                    matchingMovie = catalog.movies.firstOrNull {
                        CloudIdentityHasher.hashItemId(it.id.toString()) == entry.itemId
                    }
                }
                if (matchingMovie != null) {
                    currentEntries[i] = entry.copy(
                        key = "movie:$sourceId:${matchingMovie.id}",
                        sourceId = sourceId,
                        itemId = matchingMovie.id.toString(),
                        title = matchingMovie.name,
                        posterUrl = matchingMovie.posterUrl,
                        containerExtension = matchingMovie.containerExtension.ifBlank { "mp4" }
                    )
                    progressChanged = true
                }
            } else if (entry.kind == VodProgressKind.EPISODE && (entry.title.isBlank() || entry.posterUrl.isNullOrBlank())) {
                val matchingSeries = entry.seriesId?.let { sId -> seriesMap[sId.toString()] }
                if (matchingSeries != null) {
                    currentEntries[i] = entry.copy(
                        sourceId = sourceId,
                        seriesId = matchingSeries.id,
                        title = if (entry.title.isNotBlank()) entry.title else matchingSeries.name,
                        posterUrl = entry.posterUrl ?: matchingSeries.coverUrl
                    )
                    progressChanged = true
                }
            }
        }

        val filesDir = context.applicationContext.filesDir
        val xtreamSource = playlists.firstOrNull { it.id == sourceId }?.source as? ContentSource.Xtream
        val client = XtreamClient(context.applicationContext, sourceId, xtreamSource ?: ContentSource.Xtream("", "", "", ""))

        val unresolvedEpisodes = currentEntries.indices.filter { index ->
            val entry = currentEntries[index]
            entry.kind == VodProgressKind.EPISODE && entry.sourceId == sourceId && entry.seriesId == null
        }.associateBy { index -> currentEntries[index].itemId }.toMutableMap()
        val cachePrefix = "xtream_${sourceId}_get_series_info_series_id_"
        if (unresolvedEpisodes.isNotEmpty()) {
            for (file in filesDir.listFiles { _, name -> name.startsWith(cachePrefix) && name.endsWith(".json") }.orEmpty()) {
                if (unresolvedEpisodes.isEmpty()) break
                val seriesId = file.name.removePrefix(cachePrefix).removeSuffix(".json")
                val series = seriesMap[seriesId] ?: continue
                val details = runCatching { client.parseSeriesDetails(file.readText()) }.getOrNull() ?: continue
                for (episode in details.episodes) {
                    val index = unresolvedEpisodes.remove(episode.id)
                        ?: unresolvedEpisodes.remove(CloudIdentityHasher.hashItemId(episode.id))
                        ?: continue
                    val entry = currentEntries[index]
                    currentEntries[index] = entry.copy(
                        seriesId = series.id,
                        title = entry.title.ifBlank { series.name },
                        posterUrl = entry.posterUrl ?: episode.posterUrl ?: series.coverUrl,
                        season = episode.season,
                        episode = episode.episodeNumber
                    )
                    progressChanged = true
                }
            }
        }
        val seriesWithProgress = currentEntries
            .filter { it.kind == VodProgressKind.EPISODE && it.sourceId == sourceId }
            .mapNotNull { it.seriesId }
            .toSet()

        val seriesToCheck = if (inferNextEpisodes && watched.any { it.startsWith("episode:$sourceId:") }) {
            catalog.series
        } else emptyList()
        for (series in seriesToCheck) {
            if (series.id in seriesWithProgress) continue
            if (vodProgressStore.isDismissed("episode:$sourceId:${series.id}", series.id.toString())) continue

            val cacheFile = File(filesDir, "xtream_${sourceId}_get_series_info_series_id_${series.id}.json")
            if (!cacheFile.exists() || cacheFile.length() <= 0) continue

            val details = runCatching { client.parseSeriesDetails(cacheFile.readText()) }.getOrNull() ?: continue
            val episodes = details.episodes
            if (episodes.isEmpty()) continue

            val watchedIndices = episodes.indices.filter { idx ->
                val ep = episodes[idx]
                val epKey = ep.libraryId(sourceId)
                epKey in watched || userLibrary.isItemWatched(epKey, watched)
            }
            if (watchedIndices.isEmpty()) continue

            val lastWatchedIndex = watchedIndices.maxOrNull() ?: continue
            val nextEpisode = episodes.getOrNull(lastWatchedIndex + 1) ?: continue

            val nextKey = nextEpisode.libraryId(sourceId)
            if (nextKey in watched || userLibrary.isItemWatched(nextKey, watched)) continue
            if (vodProgressStore.isDismissed(nextKey, series.id.toString())) continue

            val strings = if (Locale.getDefault().language.lowercase().startsWith("pt")) {
                com.fortresstv.player.ui.PortugueseStrings
            } else {
                com.fortresstv.player.ui.EnglishStrings
            }

            val nextEntry = VodProgressEntry(
                key = nextKey,
                kind = VodProgressKind.EPISODE,
                sourceId = sourceId,
                itemId = nextEpisode.id,
                seriesId = series.id,
                title = "${series.name} \u00b7 ${strings.episodeLabel(nextEpisode.season, nextEpisode.episodeNumber)} \u00b7 ${nextEpisode.title}",
                posterUrl = nextEpisode.posterUrl ?: series.coverUrl,
                containerExtension = nextEpisode.containerExtension.ifBlank { "mp4" },
                season = nextEpisode.season,
                episode = nextEpisode.episodeNumber,
                positionMs = 0L,
                durationMs = (nextEpisode.durationSecs ?: 0) * 1000L,
                updatedAtMs = cacheFile.lastModified().coerceAtLeast(System.currentTimeMillis() - 24 * 3600 * 1000L)
            )
            currentEntries.add(0, nextEntry)
            progressChanged = true
        }

        withContext(Dispatchers.Main) {
            if (!shouldApply()) return@withContext
            if (favs != originalFavs && userLibrary.favoriteChannelIds() == originalFavs) {
                userLibrary.setFavoriteChannelIds(favs)
            }
            if (watchlist != originalWatchlist && userLibrary.movieWatchlistIds() == originalWatchlist) {
                userLibrary.setMovieWatchlistIds(watchlist)
            }
            if (followed != originalFollowed && userLibrary.followedSeriesIds() == originalFollowed) {
                userLibrary.setFollowedSeriesIds(followed)
            }
            if (watched != originalWatched && userLibrary.watchedItemIds() == originalWatched) {
                userLibrary.setWatchedItemIds(watched)
            }
            if (progressChanged && userLibrary.watchedItemIds() == watched && vodProgressStore.entries() == originalEntries) {
                vodProgressStore.setAll(currentEntries)
            }
            onStateUpdated()
        }
    }

    fun cancelRetry(profileId: String) {
        playlistRetryJobs.remove(profileId)?.cancel()
    }
}

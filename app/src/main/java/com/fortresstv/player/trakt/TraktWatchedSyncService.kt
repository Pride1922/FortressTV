package com.fortresstv.player.trakt

import android.content.Context
import com.fortresstv.player.CatalogMovie
import com.fortresstv.player.library.UserLibraryStore
import com.fortresstv.player.library.UserLibraryStore.Companion.libraryId
import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamEpisode
import com.fortresstv.player.xtream.XtreamMovie
import com.fortresstv.player.xtream.XtreamSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object TraktWatchedSyncService {

    suspend fun syncWatchedHistory(
        context: Context,
        profileNamespace: String,
        catalogs: List<XtreamCatalog> = emptyList(),
        userLibrary: UserLibraryStore? = null,
        client: TraktClient = TraktClient()
    ): Result<Int> = withContext(Dispatchers.IO) {
        val traktStore = TraktSettingsStore(context, profileNamespace)
        val clientId = traktStore.loadClientId()
        if (clientId.isBlank() || !traktStore.isConnected()) {
            return@withContext Result.failure(IllegalStateException("Trakt is not connected"))
        }

        val accessToken = traktStore.getValidAccessToken(clientId, client)
            ?: return@withContext Result.failure(IllegalStateException("Unable to obtain valid access token"))

        val moviesResult = client.fetchWatchedMovies(accessToken, clientId)
        val showsResult = client.fetchWatchedShows(accessToken, clientId)

        if (moviesResult is TraktAuthResult.Failure && showsResult is TraktAuthResult.Failure) {
            return@withContext Result.failure(Exception(moviesResult.message))
        }

        val watchedMovies = (moviesResult as? TraktAuthResult.Success)?.data.orEmpty()
        val watchedShows = (showsResult as? TraktAuthResult.Success)?.data.orEmpty()

        val watchedStore = TraktWatchedStore(context, profileNamespace)
        watchedStore.saveWatchedMovies(watchedMovies)
        watchedStore.saveWatchedShows(watchedShows)
        watchedStore.setLastSyncedEpochMs(System.currentTimeMillis())

        var matchedCount = 0

        if (userLibrary != null && catalogs.isNotEmpty() && watchedMovies.isNotEmpty()) {
            val targetTmdb = watchedMovies.mapNotNullTo(HashSet()) { it.tmdbId?.takeIf(String::isNotBlank) }
            val targetImdb = watchedMovies.mapNotNullTo(HashSet()) { it.imdbId?.takeIf(String::isNotBlank)?.lowercase() }
            val targetTitles = watchedMovies.mapNotNullTo(HashSet()) { normalizeTitle(it.title).takeIf(String::isNotEmpty) }

            val movieByTmdb = mutableMapOf<String, CatalogMovie>()
            val movieByImdb = mutableMapOf<String, CatalogMovie>()
            val movieByTitleYear = mutableMapOf<String, CatalogMovie>()

            for (cat in catalogs) {
                for (movie in cat.movies) {
                    val tmdb = movie.tmdbId?.takeIf { it.isNotBlank() }
                    val imdb = movie.imdbId?.takeIf { it.isNotBlank() }?.lowercase()
                    val tmdbHit = tmdb != null && tmdb in targetTmdb
                    val imdbHit = imdb != null && imdb in targetImdb
                    val norm = if (targetTitles.isNotEmpty()) normalizeTitle(movie.name) else ""
                    val titleHit = norm.isNotEmpty() && norm in targetTitles
                    if (!tmdbHit && !imdbHit && !titleHit) continue
                    val cm = CatalogMovie(cat, movie)
                    if (tmdbHit) movieByTmdb.putIfAbsent(tmdb!!, cm)
                    if (imdbHit) movieByImdb.putIfAbsent(imdb!!, cm)
                    if (titleHit) movieByTitleYear.putIfAbsent(norm + "_" + (movie.year ?: ""), cm)
                }
            }

            val currentWatched = userLibrary.watchedItemIds().toMutableSet()
            for (wm in watchedMovies) {
                val match = (wm.tmdbId?.let { movieByTmdb[it] })
                    ?: (wm.imdbId?.lowercase()?.let { movieByImdb[it] })
                    ?: (wm.year?.let { movieByTitleYear[normalizeTitle(wm.title) + "_$it"] })

                if (match != null) {
                    val libId = match.movie.libraryId(match.catalog.sourceId)
                    if (currentWatched.add(libId)) {
                        matchedCount++
                    }
                }
            }

            if (matchedCount > 0) {
                userLibrary.setWatchedItemIds(currentWatched)
            }
        } else {
            matchedCount = watchedMovies.size + watchedShows.size
        }

        Result.success(matchedCount)
    }

    suspend fun syncMovieWatched(
        context: Context,
        profileNamespace: String,
        movie: XtreamMovie,
        watched: Boolean,
        client: TraktClient = TraktClient()
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val traktStore = TraktSettingsStore(context, profileNamespace)
        val clientId = traktStore.loadClientId()
        if (clientId.isBlank() || !traktStore.isConnected()) {
            return@withContext Result.failure(IllegalStateException("Trakt is not connected"))
        }

        val accessToken = traktStore.getValidAccessToken(clientId, client)
            ?: return@withContext Result.failure(IllegalStateException("No valid access token"))

        val payload = TraktSyncPayloadBuilder.moviePayload(
            title = movie.name,
            year = movie.year?.toIntOrNull(),
            tmdbId = movie.tmdbId,
            imdbId = movie.imdbId
        )

        val result = if (watched) {
            client.addToHistory(payload, accessToken, clientId)
        } else {
            client.removeFromHistory(payload, accessToken, clientId)
        }

        when (result) {
            is TraktAuthResult.Success -> {
                val watchedStore = TraktWatchedStore(context, profileNamespace)
                if (watched) {
                    watchedStore.addWatchedMovie(
                        TraktWatchedMovie(
                            title = movie.name,
                            year = movie.year?.toIntOrNull(),
                            tmdbId = movie.tmdbId,
                            imdbId = movie.imdbId,
                            traktId = null
                        )
                    )
                } else {
                    watchedStore.removeWatchedMovie(
                        title = movie.name,
                        year = movie.year?.toIntOrNull(),
                        tmdbId = movie.tmdbId,
                        imdbId = movie.imdbId
                    )
                }
                Result.success(true)
            }
            is TraktAuthResult.Failure -> Result.failure(Exception(result.message))
        }
    }

    suspend fun syncEpisodeWatched(
        context: Context,
        profileNamespace: String,
        series: XtreamSeries?,
        episode: XtreamEpisode,
        watched: Boolean,
        client: TraktClient = TraktClient()
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (series == null) {
            return@withContext Result.failure(IllegalArgumentException("Series context required for episode history sync"))
        }

        val traktStore = TraktSettingsStore(context, profileNamespace)
        val clientId = traktStore.loadClientId()
        if (clientId.isBlank() || !traktStore.isConnected()) {
            return@withContext Result.failure(IllegalStateException("Trakt is not connected"))
        }

        val accessToken = traktStore.getValidAccessToken(clientId, client)
            ?: return@withContext Result.failure(IllegalStateException("No valid access token"))

        val payload = TraktSyncPayloadBuilder.episodePayload(
            showTitle = series.name,
            showYear = series.year?.toIntOrNull(),
            showTmdbId = series.tmdbId,
            showImdbId = series.imdbId,
            seasonNumber = episode.season,
            episodeNumber = episode.episodeNumber
        )

        val result = if (watched) {
            client.addToHistory(payload, accessToken, clientId)
        } else {
            client.removeFromHistory(payload, accessToken, clientId)
        }

        when (result) {
            is TraktAuthResult.Success -> {
                val watchedStore = TraktWatchedStore(context, profileNamespace)
                if (watched) {
                    watchedStore.addWatchedEpisode(
                        seriesTitle = series.name,
                        seriesYear = series.year?.toIntOrNull(),
                        seriesTmdbId = series.tmdbId,
                        seriesImdbId = series.imdbId,
                        season = episode.season,
                        episodeNumber = episode.episodeNumber
                    )
                } else {
                    watchedStore.removeWatchedEpisode(
                        seriesTitle = series.name,
                        seriesYear = series.year?.toIntOrNull(),
                        seriesTmdbId = series.tmdbId,
                        seriesImdbId = series.imdbId,
                        season = episode.season,
                        episodeNumber = episode.episodeNumber
                    )
                }
                Result.success(true)
            }
            is TraktAuthResult.Failure -> Result.failure(Exception(result.message))
        }
    }

    fun matchWatchedEpisodesForSeries(
        context: Context,
        profileNamespace: String,
        series: XtreamSeries,
        episodes: List<XtreamEpisode>
    ): List<XtreamEpisode> {
        if (episodes.isEmpty()) return emptyList()

        val watchedStore = TraktWatchedStore(context, profileNamespace)
        val watchedShows = watchedStore.loadWatchedShows()
        if (watchedShows.isEmpty()) return emptyList()

        val matchedShow = watchedShows.firstOrNull { ws ->
            (!series.tmdbId.isNullOrBlank() && !ws.tmdbId.isNullOrBlank() && ws.tmdbId == series.tmdbId) ||
            (!series.imdbId.isNullOrBlank() && !ws.imdbId.isNullOrBlank() && ws.imdbId.equals(series.imdbId, ignoreCase = true)) ||
            (ws.title.equals(series.name, ignoreCase = true) && ws.year == series.year?.toIntOrNull()) ||
            (normalizeTitle(ws.title) == normalizeTitle(series.name))
        } ?: return emptyList()

        val matchedEpisodes = mutableListOf<XtreamEpisode>()
        for (ep in episodes) {
            val seasonWatchedEps = matchedShow.seasons[ep.season]
            if (seasonWatchedEps?.contains(ep.episodeNumber) == true) {
                matchedEpisodes.add(ep)
            }
        }
        return matchedEpisodes
    }

    private val NON_ALNUM_REGEX = Regex("[^a-z0-9]")

    fun normalizeTitle(title: String): String {
        return title.lowercase().replace(NON_ALNUM_REGEX, "")
    }
}

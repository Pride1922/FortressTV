package com.fortresstv.player.trakt

import android.content.Context
import com.fortresstv.player.CatalogMovie
import com.fortresstv.player.CatalogSeries
import com.fortresstv.player.library.UserLibraryStore.Companion.libraryId
import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamMovie
import com.fortresstv.player.xtream.XtreamSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class TraktMatchedWatchlist(
    val movies: List<CatalogMovie> = emptyList(),
    val series: List<CatalogSeries> = emptyList()
) {
    val totalCount: Int get() = movies.size + series.size
    val isEmpty: Boolean get() = movies.isEmpty() && series.isEmpty()
}

internal object TraktWatchlistService {

    suspend fun syncWatchlist(
        context: Context,
        profileNamespace: String,
        client: TraktClient = TraktClient()
    ): Result<List<TraktWatchlistItem>> = withContext(Dispatchers.IO) {
        val traktStore = TraktSettingsStore(context, profileNamespace)
        val clientId = traktStore.loadClientId()
        if (clientId.isBlank() || !traktStore.isConnected()) {
            return@withContext Result.failure(IllegalStateException("Trakt is not connected"))
        }

        val accessToken = traktStore.getValidAccessToken(clientId, client)
            ?: return@withContext Result.failure(IllegalStateException("Unable to obtain valid access token"))

        val moviesResult = client.fetchWatchlistMovies(accessToken, clientId)
        val showsResult = client.fetchWatchlistShows(accessToken, clientId)

        val items = mutableListOf<TraktWatchlistItem>()
        when (moviesResult) {
            is TraktAuthResult.Success -> items.addAll(moviesResult.data)
            is TraktAuthResult.Failure -> {}
        }
        when (showsResult) {
            is TraktAuthResult.Success -> items.addAll(showsResult.data)
            is TraktAuthResult.Failure -> {}
        }

        if (moviesResult is TraktAuthResult.Failure && showsResult is TraktAuthResult.Failure) {
            return@withContext Result.failure(Exception(moviesResult.message))
        }

        val watchlistStore = TraktWatchlistStore(context, profileNamespace)
        watchlistStore.saveWatchlist(items)
        Result.success(items)
    }

    /**
     * Matches Trakt Watchlist items against local enabled VOD catalogs.
     */
    fun matchWatchlist(
        watchlist: List<TraktWatchlistItem>,
        catalogs: List<XtreamCatalog>
    ): TraktMatchedWatchlist {
        if (watchlist.isEmpty() || catalogs.isEmpty()) {
            return TraktMatchedWatchlist()
        }

        val allMovies = catalogs.flatMap { cat -> cat.movies.map { CatalogMovie(cat, it) } }
        val allSeries = catalogs.flatMap { cat -> cat.series.map { CatalogSeries(cat, it) } }

        val movieByTmdb = mutableMapOf<String, CatalogMovie>()
        val movieByImdb = mutableMapOf<String, CatalogMovie>()
        val movieByTitleYear = mutableMapOf<String, CatalogMovie>()

        for (cm in allMovies) {
            cm.movie.tmdbId?.takeIf { it.isNotBlank() }?.let { movieByTmdb.putIfAbsent(it, cm) }
            cm.movie.imdbId?.takeIf { it.isNotBlank() }?.let { movieByImdb.putIfAbsent(it.lowercase(), cm) }
            val key = normalizeTitle(cm.movie.name) + "_" + (cm.movie.year ?: "")
            movieByTitleYear.putIfAbsent(key, cm)
        }

        val seriesByTmdb = mutableMapOf<String, CatalogSeries>()
        val seriesByImdb = mutableMapOf<String, CatalogSeries>()
        val seriesByTitleYear = mutableMapOf<String, CatalogSeries>()

        for (cs in allSeries) {
            cs.series.tmdbId?.takeIf { it.isNotBlank() }?.let { seriesByTmdb.putIfAbsent(it, cs) }
            cs.series.imdbId?.takeIf { it.isNotBlank() }?.let { seriesByImdb.putIfAbsent(it.lowercase(), cs) }
            val key = normalizeTitle(cs.series.name) + "_" + (cs.series.year ?: "")
            seriesByTitleYear.putIfAbsent(key, cs)
        }

        val matchedMovies = mutableListOf<CatalogMovie>()
        val matchedSeries = mutableListOf<CatalogSeries>()
        val matchedMovieIds = mutableSetOf<String>()
        val matchedSeriesIds = mutableSetOf<String>()

        for (item in watchlist) {
            when (item.kind) {
                TraktWatchlistKind.MOVIE -> {
                    val match = (item.tmdbId?.let { movieByTmdb[it] })
                        ?: (item.imdbId?.lowercase()?.let { movieByImdb[it] })
                        ?: (item.year?.let { movieByTitleYear[normalizeTitle(item.title) + "_$it"] })

                    if (match != null) {
                        val libId = match.movie.libraryId(match.catalog.sourceId)
                        if (matchedMovieIds.add(libId)) {
                            matchedMovies.add(match)
                        }
                    }
                }
                TraktWatchlistKind.SHOW -> {
                    val match = (item.tmdbId?.let { seriesByTmdb[it] })
                        ?: (item.imdbId?.lowercase()?.let { seriesByImdb[it] })
                        ?: (item.year?.let { seriesByTitleYear[normalizeTitle(item.title) + "_$it"] })

                    if (match != null) {
                        val libId = match.series.libraryId(match.catalog.sourceId)
                        if (matchedSeriesIds.add(libId)) {
                            matchedSeries.add(match)
                        }
                    }
                }
            }
        }

        return TraktMatchedWatchlist(movies = matchedMovies, series = matchedSeries)
    }

    suspend fun syncItemWatchlist(
        context: Context,
        profileNamespace: String,
        isAdd: Boolean,
        kind: TraktWatchlistKind,
        title: String,
        year: Int?,
        tmdbId: String?,
        imdbId: String?,
        client: TraktClient = TraktClient()
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val traktStore = TraktSettingsStore(context, profileNamespace)
        val clientId = traktStore.loadClientId()
        if (clientId.isBlank() || !traktStore.isConnected()) {
            return@withContext Result.failure(IllegalStateException("Trakt is not connected"))
        }

        val accessToken = traktStore.getValidAccessToken(clientId, client)
            ?: return@withContext Result.failure(IllegalStateException("No valid access token"))

        val item = TraktWatchlistItem(
            kind = kind,
            title = title,
            year = year,
            tmdbId = tmdbId,
            imdbId = imdbId,
            traktId = null,
            slug = null,
            listedAt = null
        )

        val result = if (isAdd) {
            client.addToWatchlist(item, accessToken, clientId)
        } else {
            client.removeFromWatchlist(item, accessToken, clientId)
        }

        when (result) {
            is TraktAuthResult.Success -> {
                val store = TraktWatchlistStore(context, profileNamespace)
                val current = store.loadWatchlist().toMutableList()
                if (isAdd) {
                    current.removeAll { it.kind == kind && ((it.tmdbId != null && it.tmdbId == tmdbId) || (it.imdbId != null && it.imdbId.equals(imdbId, ignoreCase = true))) }
                    current.add(0, item)
                } else {
                    current.removeAll { it.kind == kind && ((it.tmdbId != null && it.tmdbId == tmdbId) || (it.imdbId != null && it.imdbId.equals(imdbId, ignoreCase = true)) || (it.title.equals(title, ignoreCase = true) && it.year == year)) }
                }
                store.saveWatchlist(current)
                Result.success(true)
            }
            is TraktAuthResult.Failure -> Result.failure(Exception(result.message))
        }
    }

    fun normalizeTitle(title: String): String {
        return title.lowercase().replace(Regex("[^a-z0-9]"), "")
    }
}

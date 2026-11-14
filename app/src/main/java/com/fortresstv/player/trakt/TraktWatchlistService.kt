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

        val movieItems = watchlist.filter { it.kind == TraktWatchlistKind.MOVIE }
        val showItems = watchlist.filter { it.kind == TraktWatchlistKind.SHOW }

        val targetMovieTmdb = movieItems.mapNotNullTo(HashSet()) { it.tmdbId?.takeIf(String::isNotBlank) }
        val targetMovieImdb = movieItems.mapNotNullTo(HashSet()) { it.imdbId?.takeIf(String::isNotBlank)?.lowercase() }
        val targetMovieTitles = movieItems.mapNotNullTo(HashSet()) { normalizeTitle(it.title).takeIf(String::isNotEmpty) }

        val targetShowTmdb = showItems.mapNotNullTo(HashSet()) { it.tmdbId?.takeIf(String::isNotBlank) }
        val targetShowImdb = showItems.mapNotNullTo(HashSet()) { it.imdbId?.takeIf(String::isNotBlank)?.lowercase() }
        val targetShowTitles = showItems.mapNotNullTo(HashSet()) { normalizeTitle(it.title).takeIf(String::isNotEmpty) }

        val movieByTmdb = mutableMapOf<String, CatalogMovie>()
        val movieByImdb = mutableMapOf<String, CatalogMovie>()
        val movieByTitleYear = mutableMapOf<String, CatalogMovie>()

        if (movieItems.isNotEmpty()) {
            for (cat in catalogs) {
                for (movie in cat.movies) {
                    val tmdb = movie.tmdbId?.takeIf { it.isNotBlank() }
                    val imdb = movie.imdbId?.takeIf { it.isNotBlank() }?.lowercase()
                    val tmdbHit = tmdb != null && tmdb in targetMovieTmdb
                    val imdbHit = imdb != null && imdb in targetMovieImdb
                    val norm = if (targetMovieTitles.isNotEmpty()) normalizeTitle(movie.name) else ""
                    val titleHit = norm.isNotEmpty() && norm in targetMovieTitles
                    if (!tmdbHit && !imdbHit && !titleHit) continue
                    val cm = CatalogMovie(cat, movie)
                    if (tmdbHit) movieByTmdb.putIfAbsent(tmdb!!, cm)
                    if (imdbHit) movieByImdb.putIfAbsent(imdb!!, cm)
                    if (titleHit) movieByTitleYear.putIfAbsent(norm + "_" + (movie.year ?: ""), cm)
                }
            }
        }

        val seriesByTmdb = mutableMapOf<String, CatalogSeries>()
        val seriesByImdb = mutableMapOf<String, CatalogSeries>()
        val seriesByTitleYear = mutableMapOf<String, CatalogSeries>()

        if (showItems.isNotEmpty()) {
            for (cat in catalogs) {
                for (series in cat.series) {
                    val tmdb = series.tmdbId?.takeIf { it.isNotBlank() }
                    val imdb = series.imdbId?.takeIf { it.isNotBlank() }?.lowercase()
                    val tmdbHit = tmdb != null && tmdb in targetShowTmdb
                    val imdbHit = imdb != null && imdb in targetShowImdb
                    val norm = if (targetShowTitles.isNotEmpty()) normalizeTitle(series.name) else ""
                    val titleHit = norm.isNotEmpty() && norm in targetShowTitles
                    if (!tmdbHit && !imdbHit && !titleHit) continue
                    val cs = CatalogSeries(cat, series)
                    if (tmdbHit) seriesByTmdb.putIfAbsent(tmdb!!, cs)
                    if (imdbHit) seriesByImdb.putIfAbsent(imdb!!, cs)
                    if (titleHit) seriesByTitleYear.putIfAbsent(norm + "_" + (series.year ?: ""), cs)
                }
            }
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

    private val NON_ALNUM_REGEX = Regex("[^a-z0-9]")

    fun normalizeTitle(title: String): String {
        return title.lowercase().replace(NON_ALNUM_REGEX, "")
    }
}

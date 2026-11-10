package com.fortresstv.player.trakt

import android.content.Context
import com.fortresstv.player.CatalogMovie
import com.fortresstv.player.CatalogSeries
import com.fortresstv.player.library.UserLibraryStore.Companion.libraryId
import com.fortresstv.player.profile.ParentalControls
import com.fortresstv.player.xtream.XtreamCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class TraktMatchedRecommendations(
    val movies: List<CatalogMovie> = emptyList(),
    val series: List<CatalogSeries> = emptyList()
) {
    val totalCount: Int get() = movies.size + series.size
    val isEmpty: Boolean get() = movies.isEmpty() && series.isEmpty()
}

internal object TraktRecommendationsService {

    suspend fun syncRecommendations(
        context: Context,
        profileNamespace: String,
        client: TraktClient = TraktClient()
    ): Result<List<TraktRecommendationItem>> = withContext(Dispatchers.IO) {
        val traktStore = TraktSettingsStore(context, profileNamespace)
        val clientId = traktStore.loadClientId()
        if (clientId.isBlank() || !traktStore.isConnected()) {
            return@withContext Result.failure(IllegalStateException("Trakt is not connected"))
        }

        val accessToken = traktStore.getValidAccessToken(clientId, client)
            ?: return@withContext Result.failure(IllegalStateException("Unable to obtain valid access token"))

        val moviesResult = client.fetchRecommendedMovies(accessToken, clientId, limit = 40)
        val showsResult = client.fetchRecommendedShows(accessToken, clientId, limit = 40)

        val items = mutableListOf<TraktRecommendationItem>()
        when (moviesResult) {
            is TraktAuthResult.Success -> items.addAll(moviesResult.data)
            is TraktAuthResult.Failure -> {}
        }
        when (showsResult) {
            is TraktAuthResult.Success -> items.addAll(showsResult.data)
            is TraktAuthResult.Failure -> {}
        }

        // If personalized recommendations are empty (e.g. brand new Trakt account with no watch history),
        // fall back to Trakt Trending movies and shows so the shelf is immediately populated.
        val trendingFallback = items.isEmpty()
        if (trendingFallback) {
            val trendingMovies = client.fetchTrendingMovies(clientId = clientId, accessToken = accessToken, limit = 40)
            if (trendingMovies is TraktAuthResult.Success) {
                items.addAll(trendingMovies.data)
            }
            val trendingShows = client.fetchTrendingShows(clientId = clientId, accessToken = accessToken, limit = 40)
            if (trendingShows is TraktAuthResult.Success) {
                items.addAll(trendingShows.data)
            }
        }

        if (moviesResult is TraktAuthResult.Failure && showsResult is TraktAuthResult.Failure && items.isEmpty()) {
            return@withContext Result.failure(Exception(moviesResult.message))
        }

        val recStore = TraktRecommendationsStore(context, profileNamespace)
        recStore.saveRecommendations(items, trendingFallback)
        Result.success(recStore.loadRecommendations())
    }

    suspend fun dismissRecommendation(
        context: Context,
        profileNamespace: String,
        item: TraktRecommendationItem
    ): Result<List<TraktRecommendationItem>> = withTraktAccount(context, profileNamespace) { client, token, clientId ->
        val identifier = item.traktId?.toString() ?: item.slug ?: item.imdbId ?: item.tmdbId
            ?: return@withTraktAccount Result.failure(IllegalStateException("Recommendation has no Trakt identifier"))
        when (val result = client.dismissRecommendation(item.kind, identifier, token, clientId)) {
            is TraktAuthResult.Failure -> Result.failure(IllegalStateException(result.message))
            is TraktAuthResult.Success -> {
                val store = TraktRecommendationsStore(context, profileNamespace)
                store.dismissRecommendation(item)
                Result.success(store.loadRecommendations())
            }
        }
    }

    suspend fun fetchHiddenRecommendations(
        context: Context,
        profileNamespace: String,
        page: Int
    ): Result<TraktHiddenRecommendationsPage> = withTraktAccount(context, profileNamespace) { client, token, clientId ->
        when (val result = client.fetchHiddenRecommendations(page, token, clientId)) {
            is TraktAuthResult.Failure -> Result.failure(IllegalStateException(result.message))
            is TraktAuthResult.Success -> Result.success(result.data)
        }
    }

    suspend fun restoreRecommendation(
        context: Context,
        profileNamespace: String,
        item: TraktRecommendationItem
    ): Result<List<TraktRecommendationItem>> = withTraktAccount(context, profileNamespace) { client, token, clientId ->
        when (val result = client.restoreRecommendation(item, token, clientId)) {
            is TraktAuthResult.Failure -> Result.failure(IllegalStateException(result.message))
            is TraktAuthResult.Success -> {
                val store = TraktRecommendationsStore(context, profileNamespace)
                store.restoreRecommendation(item)
                Result.success(store.loadRecommendations())
            }
        }
    }

    private suspend fun <T> withTraktAccount(
        context: Context,
        profileNamespace: String,
        operation: (TraktClient, String, String) -> Result<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        val client = TraktClient()
        val store = TraktSettingsStore(context, profileNamespace)
        val clientId = store.loadClientId()
        val token = if (clientId.isNotBlank() && store.isConnected()) {
            store.getValidAccessToken(clientId, client)
        } else null
        if (token == null) Result.failure(IllegalStateException("Trakt is not connected"))
        else operation(client, token, clientId)
    }

    fun matchRecommendations(
        recommendations: List<TraktRecommendationItem>,
        catalogs: List<XtreamCatalog>,
        hideAdultContent: Boolean = false,
        maximumAge: Int? = null,
        blockUnrated: Boolean = false
    ): TraktMatchedRecommendations {
        if (recommendations.isEmpty() || catalogs.isEmpty()) {
            return TraktMatchedRecommendations()
        }

        val allMovies = catalogs.flatMap { cat ->
            val catNames = cat.movieCategories.associate { it.id to it.name }
            cat.movies
                .filter { movie ->
                    ParentalControls.isItemPermitted(
                        name = movie.name,
                        categoryOrGroup = catNames[movie.categoryId],
                        ageRating = movie.ageRating,
                        hideAdultContent = hideAdultContent,
                        maximumAge = maximumAge,
                        blockUnrated = blockUnrated
                    )
                }
                .map { CatalogMovie(cat, it) }
        }

        val allSeries = catalogs.flatMap { cat ->
            val catNames = cat.seriesCategories.associate { it.id to it.name }
            cat.series
                .filter { series ->
                    ParentalControls.isItemPermitted(
                        name = series.name,
                        categoryOrGroup = catNames[series.categoryId],
                        ageRating = series.ageRating,
                        hideAdultContent = hideAdultContent,
                        maximumAge = maximumAge,
                        blockUnrated = blockUnrated
                    )
                }
                .map { CatalogSeries(cat, it) }
        }

        val movieByTmdb = mutableMapOf<String, CatalogMovie>()
        val movieByImdb = mutableMapOf<String, CatalogMovie>()
        val movieByTitleYear = mutableMapOf<String, CatalogMovie>()

        for (cm in allMovies) {
            cm.movie.tmdbId?.takeIf { it.isNotBlank() }?.let { movieByTmdb.putIfAbsent(it, cm) }
            cm.movie.imdbId?.takeIf { it.isNotBlank() }?.let { movieByImdb.putIfAbsent(it.lowercase(), cm) }
            val rawKey = normalizeTitle(cm.movie.name) + "_" + (cm.movie.year ?: "")
            movieByTitleYear.putIfAbsent(rawKey, cm)

            val (cleanTitle, extractedYear) = com.fortresstv.player.tmdb.TmdbClient.cleanMediaTitle(cm.movie.name)
            val yr = extractedYear ?: cm.movie.year
            if (cleanTitle.isNotBlank()) {
                val cleanKey = normalizeTitle(cleanTitle) + "_" + (yr ?: "")
                movieByTitleYear.putIfAbsent(cleanKey, cm)
                if (yr != null) {
                    movieByTitleYear.putIfAbsent(normalizeTitle(cleanTitle) + "_$yr", cm)
                }
            }
        }

        val seriesByTmdb = mutableMapOf<String, CatalogSeries>()
        val seriesByImdb = mutableMapOf<String, CatalogSeries>()
        val seriesByTitleYear = mutableMapOf<String, CatalogSeries>()

        for (cs in allSeries) {
            cs.series.tmdbId?.takeIf { it.isNotBlank() }?.let { seriesByTmdb.putIfAbsent(it, cs) }
            cs.series.imdbId?.takeIf { it.isNotBlank() }?.let { seriesByImdb.putIfAbsent(it.lowercase(), cs) }
            val rawKey = normalizeTitle(cs.series.name) + "_" + (cs.series.year ?: "")
            seriesByTitleYear.putIfAbsent(rawKey, cs)

            val (cleanTitle, extractedYear) = com.fortresstv.player.tmdb.TmdbClient.cleanMediaTitle(cs.series.name)
            val yr = extractedYear ?: cs.series.year
            if (cleanTitle.isNotBlank()) {
                val cleanKey = normalizeTitle(cleanTitle) + "_" + (yr ?: "")
                seriesByTitleYear.putIfAbsent(cleanKey, cs)
                if (yr != null) {
                    seriesByTitleYear.putIfAbsent(normalizeTitle(cleanTitle) + "_$yr", cs)
                }
            }
        }

        val matchedMovies = mutableListOf<CatalogMovie>()
        val matchedSeries = mutableListOf<CatalogSeries>()
        val matchedMovieIds = mutableSetOf<String>()
        val matchedSeriesIds = mutableSetOf<String>()

        for (item in recommendations) {
            val normTitle = normalizeTitle(item.title)
            when (item.kind) {
                TraktWatchlistKind.MOVIE -> {
                    val match = (item.tmdbId?.let { movieByTmdb[it] })
                        ?: (item.imdbId?.lowercase()?.let { movieByImdb[it] })
                        ?: (item.year?.let { movieByTitleYear["${normTitle}_$it"] })
                        ?: movieByTitleYear["${normTitle}_"]

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
                        ?: (item.year?.let { seriesByTitleYear["${normTitle}_$it"] })
                        ?: seriesByTitleYear["${normTitle}_"]

                    if (match != null) {
                        val libId = match.series.libraryId(match.catalog.sourceId)
                        if (matchedSeriesIds.add(libId)) {
                            matchedSeries.add(match)
                        }
                    }
                }
            }
        }

        return TraktMatchedRecommendations(movies = matchedMovies, series = matchedSeries)
    }

    fun normalizeTitle(title: String): String {
        return title.lowercase().replace(Regex("[^a-z0-9]"), "")
    }
}

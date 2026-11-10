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

        if (moviesResult is TraktAuthResult.Failure && showsResult is TraktAuthResult.Failure) {
            return@withContext Result.failure(Exception(moviesResult.message))
        }

        val recStore = TraktRecommendationsStore(context, profileNamespace)
        recStore.saveRecommendations(items)
        Result.success(items)
    }

    suspend fun dismissRecommendation(
        context: Context,
        profileNamespace: String,
        item: TraktRecommendationItem,
        client: TraktClient = TraktClient()
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val traktStore = TraktSettingsStore(context, profileNamespace)
        val clientId = traktStore.loadClientId()
        if (clientId.isBlank() || !traktStore.isConnected()) {
            val recStore = TraktRecommendationsStore(context, profileNamespace)
            recStore.dismissRecommendation(item)
            return@withContext Result.success(true)
        }

        val accessToken = traktStore.getValidAccessToken(clientId, client)
            ?: run {
                val recStore = TraktRecommendationsStore(context, profileNamespace)
                recStore.dismissRecommendation(item)
                return@withContext Result.success(true)
            }

        val idOrSlug = item.traktId?.toString() ?: item.slug ?: item.imdbId ?: item.tmdbId
        if (!idOrSlug.isNullOrBlank()) {
            val result = client.dismissRecommendation(item.kind, idOrSlug, accessToken, clientId)
            if (result is TraktAuthResult.Failure) {
                if (result.statusCode != 404) {
                    return@withContext Result.failure(Exception(result.message))
                }
            }
        }

        val recStore = TraktRecommendationsStore(context, profileNamespace)
        recStore.dismissRecommendation(item)
        Result.success(true)
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

        for (item in recommendations) {
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

        return TraktMatchedRecommendations(movies = matchedMovies, series = matchedSeries)
    }

    fun normalizeTitle(title: String): String {
        return title.lowercase().replace(Regex("[^a-z0-9]"), "")
    }
}

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
    val series: List<CatalogSeries> = emptyList(),
    val includesTrending: Boolean = false,
    val seriesAreTrending: Boolean = false
) {
    val totalCount: Int get() = movies.size + series.size
    val isEmpty: Boolean get() = movies.isEmpty() && series.isEmpty()
}

internal object TraktRecommendationsService {
    private const val CANDIDATE_LIMIT = 100
    private val COMBINING_DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val NON_ALNUM_REGEX = Regex("[^a-z0-9]")

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
        val recStore = TraktRecommendationsStore(context, profileNamespace)

        fun refreshCategory(
            kind: TraktWatchlistKind,
            personalized: TraktAuthResult<List<TraktRecommendationItem>>,
            fetchTrending: () -> TraktAuthResult<List<TraktRecommendationItem>>
        ): TraktAuthResult<List<TraktRecommendationItem>> {
            val cached = recStore.loadRecommendations().filter { it.kind == kind }
            // Keep the last usable list when Trakt fails temporarily instead of replacing it.
            if (personalized is TraktAuthResult.Failure && cached.isNotEmpty()) {
                return TraktAuthResult.Success(cached)
            }
            val fallbackResult = withTrendingFallback(personalized, includeBackup = true) {
                fetchTrending()
            }
            return if (fallbackResult is TraktAuthResult.Failure && cached.isNotEmpty()) {
                TraktAuthResult.Success(cached)
            } else {
                fallbackResult
            }
        }
        val moviesResult = refreshCategory(
            TraktWatchlistKind.MOVIE,
            client.fetchRecommendedMovies(accessToken, clientId, limit = CANDIDATE_LIMIT)
        ) {
            client.fetchTrendingMovies(clientId = clientId, limit = CANDIDATE_LIMIT)
        }
        val showsResult = refreshCategory(
            TraktWatchlistKind.SHOW,
            client.fetchRecommendedShows(accessToken, clientId, limit = CANDIDATE_LIMIT)
        ) {
            client.fetchTrendingShows(clientId = clientId, limit = CANDIDATE_LIMIT)
        }

        if (moviesResult is TraktAuthResult.Failure && showsResult is TraktAuthResult.Failure) {
            val cached = recStore.loadRecommendations()
            if (cached.isNotEmpty()) {
                return@withContext Result.success(cached)
            }
            return@withContext Result.failure(Exception(moviesResult.message))
        }

        val updates = buildMap {
            if (moviesResult is TraktAuthResult.Success) put(TraktWatchlistKind.MOVIE, moviesResult.data)
            if (showsResult is TraktAuthResult.Success) put(TraktWatchlistKind.SHOW, showsResult.data)
        }
        recStore.saveRecommendations(updates)
        Result.success(recStore.loadRecommendations())
    }

    internal fun withTrendingFallback(
        personalized: TraktAuthResult<List<TraktRecommendationItem>>,
        includeBackup: Boolean = false,
        fetchTrending: () -> TraktAuthResult<List<TraktRecommendationItem>>
    ): TraktAuthResult<List<TraktRecommendationItem>> {
        val items = when (personalized) {
            is TraktAuthResult.Success -> {
                if (personalized.data.isNotEmpty() && !includeBackup) return personalized
                personalized.data
            }
            is TraktAuthResult.Failure -> {
                if (personalized.statusCode == 429) return personalized
                emptyList()
            }
        }
        return when (val trending = fetchTrending()) {
            is TraktAuthResult.Success -> TraktAuthResult.Success(items + trending.data.map { it.copy(isTrending = true) })
            is TraktAuthResult.Failure -> if (items.isNotEmpty()) TraktAuthResult.Success(items) else trending
        }
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

        val movieRecs = recommendations.filter { it.kind == TraktWatchlistKind.MOVIE }
        val showRecs = recommendations.filter { it.kind == TraktWatchlistKind.SHOW }

        val recMovieTmdbIds = movieRecs.mapNotNullTo(HashSet()) { it.tmdbId?.takeIf(String::isNotBlank) }
        val recMovieImdbIds = movieRecs.mapNotNullTo(HashSet()) { it.imdbId?.takeIf(String::isNotBlank)?.lowercase() }
        val recMovieNormTitles = movieRecs.mapNotNullTo(HashSet()) { normalizeTitle(it.title).takeIf(String::isNotEmpty) }

        val recShowTmdbIds = showRecs.mapNotNullTo(HashSet()) { it.tmdbId?.takeIf(String::isNotBlank) }
        val recShowImdbIds = showRecs.mapNotNullTo(HashSet()) { it.imdbId?.takeIf(String::isNotBlank)?.lowercase() }
        val recShowNormTitles = showRecs.mapNotNullTo(HashSet()) { normalizeTitle(it.title).takeIf(String::isNotEmpty) }

        val movieByTmdb = mutableMapOf<String, CatalogMovie>()
        val movieByImdb = mutableMapOf<String, CatalogMovie>()
        val movieByTitleYear = mutableMapOf<String, CatalogMovie>()

        if (movieRecs.isNotEmpty()) {
            for (cat in catalogs) {
                val catNames = cat.movieCategories.associate { it.id to it.name }
                for (movie in cat.movies) {
                    val tmdb = movie.tmdbId?.takeIf { it.isNotBlank() }
                    val imdb = movie.imdbId?.takeIf { it.isNotBlank() }?.lowercase()
                    val tmdbHit = tmdb != null && tmdb in recMovieTmdbIds
                    val imdbHit = imdb != null && imdb in recMovieImdbIds
                    val normName = if (recMovieNormTitles.isNotEmpty()) normalizeTitle(movie.name) else ""
                    val rawTitleHit = normName.isNotEmpty() && normName in recMovieNormTitles
                    var cleanNorm = ""
                    var extractedYear: String? = null
                    var cleanTitleHit = false
                    if (!rawTitleHit && recMovieNormTitles.isNotEmpty()) {
                        val (cleanTitle, yr) = com.fortresstv.player.tmdb.TmdbClient.cleanMediaTitle(movie.name)
                        extractedYear = yr
                        cleanNorm = normalizeTitle(cleanTitle)
                        cleanTitleHit = cleanNorm.isNotEmpty() && cleanNorm in recMovieNormTitles
                    }
                    if (!tmdbHit && !imdbHit && !rawTitleHit && !cleanTitleHit) continue
                    if (!ParentalControls.isItemPermitted(
                            name = movie.name,
                            categoryOrGroup = catNames[movie.categoryId],
                            ageRating = movie.ageRating,
                            hideAdultContent = hideAdultContent,
                            maximumAge = maximumAge,
                            blockUnrated = blockUnrated
                        )
                    ) continue
                    val cm = CatalogMovie(cat, movie)
                    if (tmdbHit) movieByTmdb.putIfAbsent(tmdb!!, cm)
                    if (imdbHit) movieByImdb.putIfAbsent(imdb!!, cm)
                    if (rawTitleHit) {
                        val rawKey = normName + "_" + (movie.year ?: "")
                        movieByTitleYear.putIfAbsent(rawKey, cm)
                        movieByTitleYear.putIfAbsent("${normName}_", cm)
                    }
                    if (cleanTitleHit) {
                        val yr = extractedYear ?: movie.year
                        val cleanKey = cleanNorm + "_" + (yr ?: "")
                        movieByTitleYear.putIfAbsent(cleanKey, cm)
                        if (yr != null) {
                            movieByTitleYear.putIfAbsent("${cleanNorm}_$yr", cm)
                        }
                        movieByTitleYear.putIfAbsent("${cleanNorm}_", cm)
                    }
                }
            }
        }

        val seriesByTmdb = mutableMapOf<String, CatalogSeries>()
        val seriesByImdb = mutableMapOf<String, CatalogSeries>()
        val seriesByTitleYear = mutableMapOf<String, CatalogSeries>()

        if (showRecs.isNotEmpty()) {
            for (cat in catalogs) {
                val catNames = cat.seriesCategories.associate { it.id to it.name }
                for (series in cat.series) {
                    val tmdb = series.tmdbId?.takeIf { it.isNotBlank() }
                    val imdb = series.imdbId?.takeIf { it.isNotBlank() }?.lowercase()
                    val tmdbHit = tmdb != null && tmdb in recShowTmdbIds
                    val imdbHit = imdb != null && imdb in recShowImdbIds
                    val normName = if (recShowNormTitles.isNotEmpty()) normalizeTitle(series.name) else ""
                    val rawTitleHit = normName.isNotEmpty() && normName in recShowNormTitles
                    var cleanNorm = ""
                    var extractedYear: String? = null
                    var cleanTitleHit = false
                    if (!rawTitleHit && recShowNormTitles.isNotEmpty()) {
                        val (cleanTitle, yr) = com.fortresstv.player.tmdb.TmdbClient.cleanMediaTitle(series.name)
                        extractedYear = yr
                        cleanNorm = normalizeTitle(cleanTitle)
                        cleanTitleHit = cleanNorm.isNotEmpty() && cleanNorm in recShowNormTitles
                    }
                    if (!tmdbHit && !imdbHit && !rawTitleHit && !cleanTitleHit) continue
                    if (!ParentalControls.isItemPermitted(
                            name = series.name,
                            categoryOrGroup = catNames[series.categoryId],
                            ageRating = series.ageRating,
                            hideAdultContent = hideAdultContent,
                            maximumAge = maximumAge,
                            blockUnrated = blockUnrated
                        )
                    ) continue
                    val cs = CatalogSeries(cat, series)
                    if (tmdbHit) seriesByTmdb.putIfAbsent(tmdb!!, cs)
                    if (imdbHit) seriesByImdb.putIfAbsent(imdb!!, cs)
                    if (rawTitleHit) {
                        val rawKey = normName + "_" + (series.year ?: "")
                        seriesByTitleYear.putIfAbsent(rawKey, cs)
                        seriesByTitleYear.putIfAbsent("${normName}_", cs)
                    }
                    if (cleanTitleHit) {
                        val yr = extractedYear ?: series.year
                        val cleanKey = cleanNorm + "_" + (yr ?: "")
                        seriesByTitleYear.putIfAbsent(cleanKey, cs)
                        if (yr != null) {
                            seriesByTitleYear.putIfAbsent("${cleanNorm}_$yr", cs)
                        }
                        seriesByTitleYear.putIfAbsent("${cleanNorm}_", cs)
                    }
                }
            }
        }

        val matchedMovies = mutableListOf<CatalogMovie>()
        val matchedSeries = mutableListOf<CatalogSeries>()
        val matchedMovieIds = mutableSetOf<String>()
        val matchedSeriesIds = mutableSetOf<String>()
        var includesTrending = false

        fun matchMovie(item: TraktRecommendationItem) {
            val normTitle = normalizeTitle(item.title)
            val match = (item.tmdbId?.let { movieByTmdb[it] })
                ?: (item.imdbId?.lowercase()?.let { movieByImdb[it] })
                ?: (item.year?.let { movieByTitleYear["${normTitle}_$it"] })
                ?: (item.year?.let { movieByTitleYear["${normTitle}_${it - 1}"] })
                ?: (item.year?.let { movieByTitleYear["${normTitle}_${it + 1}"] })
                ?: movieByTitleYear["${normTitle}_"]
                ?: return
            if (matchedMovieIds.add(match.movie.libraryId(match.catalog.sourceId))) {
                matchedMovies.add(match)
                includesTrending = includesTrending || item.isTrending
            }
        }

        fun matchSeries(item: TraktRecommendationItem) {
            val normTitle = normalizeTitle(item.title)
            val match = (item.tmdbId?.let { seriesByTmdb[it] })
                ?: (item.imdbId?.lowercase()?.let { seriesByImdb[it] })
                ?: (item.year?.let { seriesByTitleYear["${normTitle}_$it"] })
                ?: (item.year?.let { seriesByTitleYear["${normTitle}_${it - 1}"] })
                ?: (item.year?.let { seriesByTitleYear["${normTitle}_${it + 1}"] })
                ?: seriesByTitleYear["${normTitle}_"]
                ?: return
            if (matchedSeriesIds.add(match.series.libraryId(match.catalog.sourceId))) {
                matchedSeries.add(match)
                includesTrending = includesTrending || item.isTrending
            }
        }

        for (item in recommendations) {
            if (item.isTrending) continue
            when (item.kind) {
                TraktWatchlistKind.MOVIE -> matchMovie(item)
                TraktWatchlistKind.SHOW -> matchSeries(item)
            }
        }
        if (matchedMovies.isEmpty()) {
            recommendations.filter { it.kind == TraktWatchlistKind.MOVIE && it.isTrending }.forEach(::matchMovie)
        }
        val useTrendingSeries = matchedSeries.isEmpty()
        if (useTrendingSeries) {
            recommendations.filter { it.kind == TraktWatchlistKind.SHOW && it.isTrending }.forEach(::matchSeries)
        }

        return TraktMatchedRecommendations(
            movies = matchedMovies,
            series = matchedSeries,
            includesTrending = includesTrending,
            seriesAreTrending = useTrendingSeries && matchedSeries.isNotEmpty()
        )
    }

    fun normalizeTitle(title: String): String {
        return java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD)
            .replace(COMBINING_DIACRITICS_REGEX, "")
            .lowercase()
            .replace(NON_ALNUM_REGEX, "")
    }
}

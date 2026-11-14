package com.fortresstv.player.trakt

import android.content.Context
import com.fortresstv.player.CatalogMovie
import com.fortresstv.player.CatalogSeries
import com.fortresstv.player.library.UserLibraryStore.Companion.libraryId
import com.fortresstv.player.profile.ParentalControls
import com.fortresstv.player.tmdb.TmdbClient
import com.fortresstv.player.xtream.XtreamCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.PriorityQueue

internal enum class FeaturedSeriesMode {
    HIDDEN,
    TRAKT_POPULAR,
    RECENTLY_ADDED
}

internal data class FeaturedSeriesShelf(
    val mode: FeaturedSeriesMode = FeaturedSeriesMode.HIDDEN,
    val items: List<CatalogSeries> = emptyList()
)

internal data class FeaturedMoviesShelf(
    val mode: FeaturedSeriesMode = FeaturedSeriesMode.HIDDEN,
    val items: List<CatalogMovie> = emptyList()
)

private suspend fun syncPopularItems(
    context: Context,
    profileNamespace: String,
    store: TraktPopularStore,
    forceRefresh: Boolean,
    fetch: (clientId: String, accessToken: String?) -> TraktAuthResult<List<TraktRecommendationItem>>
): Result<List<TraktRecommendationItem>> = withContext(Dispatchers.IO) {
    val traktStore = TraktSettingsStore(context, profileNamespace)
    val clientId = traktStore.loadClientId()
    if (clientId.isBlank() || !traktStore.isConnected()) {
        return@withContext Result.failure(IllegalStateException("Trakt is not connected"))
    }

    val cached = store.loadItems()
    if (!forceRefresh && cached.isNotEmpty() && store.isCacheFresh()) {
        return@withContext Result.success(cached)
    }

    val accessToken = traktStore.loadToken()?.accessToken?.takeIf { it.isNotBlank() }
    when (val result = fetch(clientId, accessToken)) {
        is TraktAuthResult.Success -> {
            store.saveItems(result.data)
            Result.success(result.data)
        }
        is TraktAuthResult.Failure -> {
            if (cached.isNotEmpty()) Result.success(cached)
            else Result.failure(IllegalStateException(result.message))
        }
    }
}

internal object TraktPopularShowsService {
    private const val CANDIDATE_LIMIT = 100

    suspend fun syncPopularShows(
        context: Context,
        profileNamespace: String,
        client: TraktClient = TraktClient(),
        forceRefresh: Boolean = false
    ): Result<List<TraktRecommendationItem>> = syncPopularItems(
        context = context,
        profileNamespace = profileNamespace,
        store = TraktPopularShowsStore(context, profileNamespace),
        forceRefresh = forceRefresh
    ) { clientId, accessToken ->
        client.fetchPopularShows(clientId = clientId, accessToken = accessToken, limit = CANDIDATE_LIMIT)
    }
}

internal object TraktPopularMoviesService {
    private const val CANDIDATE_LIMIT = 100

    suspend fun syncPopularMovies(
        context: Context,
        profileNamespace: String,
        client: TraktClient = TraktClient(),
        forceRefresh: Boolean = false
    ): Result<List<TraktRecommendationItem>> = syncPopularItems(
        context = context,
        profileNamespace = profileNamespace,
        store = TraktPopularMoviesStore(context, profileNamespace),
        forceRefresh = forceRefresh
    ) { clientId, accessToken ->
        client.fetchPopularMovies(clientId = clientId, accessToken = accessToken, limit = CANDIDATE_LIMIT)
    }
}

internal object FeaturedShelfMatcher {
    const val DEFAULT_SHELF_LIMIT = 25

    data class IndexedItem<T>(
        val entry: T,
        val libraryId: String,
        val itemId: Int,
        val name: String,
        val categoryName: String?,
        val ageRating: String?,
        val tmdbId: String?,
        val imdbId: String?,
        val addedEpochMs: Long?,
        val year: String? = null
    )

    fun <T> matchPopular(
        popularItems: List<TraktRecommendationItem>,
        expectedKind: TraktWatchlistKind,
        items: Sequence<IndexedItem<T>>,
        hideAdultContent: Boolean,
        maximumAge: Int?,
        blockUnrated: Boolean,
        limit: Int
    ): List<T> {
        if (popularItems.isEmpty() || limit <= 0) return emptyList()

        val targetPopular = popularItems.filter { it.kind == expectedKind }
        if (targetPopular.isEmpty()) return emptyList()

        val targetTmdbIds = targetPopular.mapNotNullTo(HashSet()) { normalizeTmdbId(it.tmdbId) }
        val targetImdbIds = targetPopular.mapNotNullTo(HashSet()) { normalizeImdbId(it.imdbId) }
        val targetTitles = targetPopular.mapNotNullTo(HashSet()) {
            TraktRecommendationsService.normalizeTitle(it.title).takeIf(String::isNotEmpty)
        }

        val byTmdb = HashMap<String, IndexedItem<T>>()
        val byImdb = HashMap<String, IndexedItem<T>>()
        val byTitleYear = HashMap<String, IndexedItem<T>>()

        for (item in items) {
            val tmdb = normalizeTmdbId(item.tmdbId)
            val imdb = normalizeImdbId(item.imdbId)
            val tmdbHit = tmdb != null && tmdb in targetTmdbIds
            val imdbHit = imdb != null && imdb in targetImdbIds
            val normName = if (targetTitles.isNotEmpty()) TraktRecommendationsService.normalizeTitle(item.name) else ""
            val rawTitleHit = normName.isNotEmpty() && normName in targetTitles
            var cleanNorm = ""
            var extractedYear: String? = null
            var cleanTitleHit = false
            if (!rawTitleHit && targetTitles.isNotEmpty()) {
                val (cleanTitle, yr) = TmdbClient.cleanMediaTitle(item.name)
                extractedYear = yr
                cleanNorm = TraktRecommendationsService.normalizeTitle(cleanTitle)
                cleanTitleHit = cleanNorm.isNotEmpty() && cleanNorm in targetTitles
            }
            if (!tmdbHit && !imdbHit && !rawTitleHit && !cleanTitleHit) continue

            val permitted = ParentalControls.isItemPermitted(
                name = item.name,
                categoryOrGroup = item.categoryName,
                ageRating = item.ageRating,
                hideAdultContent = hideAdultContent,
                maximumAge = maximumAge,
                blockUnrated = blockUnrated
            )
            if (!permitted) continue

            if (tmdbHit) byTmdb.putIfAbsent(tmdb!!, item)
            if (imdbHit) byImdb.putIfAbsent(imdb!!, item)
            if (rawTitleHit) {
                byTitleYear.putIfAbsent("${normName}_${item.year ?: ""}", item)
                byTitleYear.putIfAbsent("${normName}_", item)
            }
            if (cleanTitleHit) {
                val yr = extractedYear ?: item.year
                byTitleYear.putIfAbsent("${cleanNorm}_${yr ?: ""}", item)
                if (yr != null) {
                    byTitleYear.putIfAbsent("${cleanNorm}_$yr", item)
                }
                byTitleYear.putIfAbsent("${cleanNorm}_", item)
            }
        }

        if (byTmdb.isEmpty() && byImdb.isEmpty() && byTitleYear.isEmpty()) return emptyList()

        // Only fall back to title+year matching when the provider catalogue omits TMDB/IMDb IDs altogether.
        val allowTitleFallback = byTmdb.isEmpty() && byImdb.isEmpty()
        val matched = ArrayList<T>(minOf(limit, popularItems.size))
        val matchedLibraryIds = HashSet<String>()
        val matchedTmdbIds = HashSet<String>()
        val matchedImdbIds = HashSet<String>()

        for (popular in popularItems) {
            if (popular.kind != expectedKind) continue
            val itemTmdb = normalizeTmdbId(popular.tmdbId)
            val itemImdb = normalizeImdbId(popular.imdbId)
            val normTitle = TraktRecommendationsService.normalizeTitle(popular.title)
            if (itemTmdb == null && itemImdb == null && (!allowTitleFallback || normTitle.isBlank())) continue

            val match = (itemTmdb?.let { byTmdb[it] })
                ?: (itemImdb?.let { byImdb[it] })
                ?: if (allowTitleFallback) {
                    (popular.year?.let { byTitleYear["${normTitle}_$it"] })
                        ?: (popular.year?.let { byTitleYear["${normTitle}_${it - 1}"] })
                        ?: (popular.year?.let { byTitleYear["${normTitle}_${it + 1}"] })
                        ?: normTitle.takeIf { it.isNotBlank() }?.let { byTitleYear["${it}_"] }
                } else {
                    null
                }
                ?: continue

            val matchTmdb = normalizeTmdbId(match.tmdbId)
            val matchImdb = normalizeImdbId(match.imdbId)

            val alreadyMatched = match.libraryId in matchedLibraryIds ||
                (itemTmdb != null && itemTmdb in matchedTmdbIds) ||
                (itemImdb != null && itemImdb in matchedImdbIds) ||
                (matchTmdb != null && matchTmdb in matchedTmdbIds) ||
                (matchImdb != null && matchImdb in matchedImdbIds)
            if (alreadyMatched) continue

            matchedLibraryIds.add(match.libraryId)
            itemTmdb?.let { matchedTmdbIds.add(it) }
            itemImdb?.let { matchedImdbIds.add(it) }
            matchTmdb?.let { matchedTmdbIds.add(it) }
            matchImdb?.let { matchedImdbIds.add(it) }

            matched.add(match.entry)
            if (matched.size >= limit) break
        }

        return matched
    }

    fun <T> rankRecentlyAdded(
        catalogs: List<XtreamCatalog>,
        firstSeenBySource: Map<String, Map<Int, Long>>,
        extract: (XtreamCatalog) -> Sequence<IndexedItem<T>>,
        hideAdultContent: Boolean,
        maximumAge: Int?,
        blockUnrated: Boolean,
        limit: Int
    ): List<T> {
        if (catalogs.isEmpty() || limit <= 0) return emptyList()

        val comparator = Comparator<RankedEntry<T>> { a, b ->
            val timeCmp = a.recencyMs.compareTo(b.recencyMs)
            if (timeCmp != 0) timeCmp else b.order.compareTo(a.order)
        }
        val minHeap = PriorityQueue(limit + 1, comparator)
        var order = 0

        for (catalog in catalogs) {
            val sourceFirstSeen = firstSeenBySource[catalog.sourceId]
            for (item in extract(catalog)) {
                val permitted = ParentalControls.isItemPermitted(
                    name = item.name,
                    categoryOrGroup = item.categoryName,
                    ageRating = item.ageRating,
                    hideAdultContent = hideAdultContent,
                    maximumAge = maximumAge,
                    blockUnrated = blockUnrated
                )
                if (!permitted) continue

                val recencyMs = item.addedEpochMs?.takeIf { it > 0L }
                    ?: sourceFirstSeen?.get(item.itemId)?.takeIf { it > 0L }
                    ?: continue

                val candidate = RankedEntry(
                    entry = item.entry,
                    recencyMs = recencyMs,
                    order = order++
                )
                if (minHeap.size < limit) {
                    minHeap.offer(candidate)
                } else {
                    val weakest = minHeap.peek()
                    if (weakest != null && comparator.compare(candidate, weakest) > 0) {
                        minHeap.poll()
                        minHeap.offer(candidate)
                    }
                }
            }
        }

        if (minHeap.isEmpty()) return emptyList()
        return minHeap.sortedWith(
            compareByDescending<RankedEntry<T>> { it.recencyMs }.thenBy { it.order }
        ).map { it.entry }
    }

    private data class RankedEntry<T>(
        val entry: T,
        val recencyMs: Long,
        val order: Int
    )

    private fun normalizeTmdbId(raw: String?): String? =
        raw?.trim()?.takeIf { it.isNotEmpty() && it != "0" }

    private fun normalizeImdbId(raw: String?): String? =
        raw?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
}

internal object FeaturedSeriesShelfResolver {
    const val DEFAULT_SHELF_LIMIT = FeaturedShelfMatcher.DEFAULT_SHELF_LIMIT

    private fun catalogSeriesItems(catalog: XtreamCatalog): Sequence<FeaturedShelfMatcher.IndexedItem<CatalogSeries>> {
        val categoryNames = catalog.seriesCategories.associate { it.id to it.name }
        return catalog.series.asSequence().map { series ->
            FeaturedShelfMatcher.IndexedItem(
                entry = CatalogSeries(catalog, series),
                libraryId = series.libraryId(catalog.sourceId),
                itemId = series.id,
                name = series.name,
                categoryName = categoryNames[series.categoryId],
                ageRating = series.ageRating,
                tmdbId = series.tmdbId,
                imdbId = series.imdbId,
                addedEpochMs = series.addedEpochMs,
                year = series.year
            )
        }
    }

    fun resolve(
        traktConnected: Boolean,
        popularShows: List<TraktRecommendationItem>,
        catalogs: List<XtreamCatalog>,
        firstSeenBySource: Map<String, Map<Int, Long>> = emptyMap(),
        hideAdultContent: Boolean = false,
        maximumAge: Int? = null,
        blockUnrated: Boolean = false,
        limit: Int = DEFAULT_SHELF_LIMIT
    ): FeaturedSeriesShelf {
        if (catalogs.isEmpty() || limit <= 0) return FeaturedSeriesShelf()

        if (traktConnected && popularShows.isNotEmpty()) {
            val popularMatches = matchPopularShows(
                popularShows = popularShows,
                catalogs = catalogs,
                hideAdultContent = hideAdultContent,
                maximumAge = maximumAge,
                blockUnrated = blockUnrated,
                limit = limit
            )
            if (popularMatches.isNotEmpty()) {
                return FeaturedSeriesShelf(mode = FeaturedSeriesMode.TRAKT_POPULAR, items = popularMatches)
            }
        }

        val recentlyAdded = rankRecentlyAddedSeries(
            catalogs = catalogs,
            firstSeenBySource = firstSeenBySource,
            hideAdultContent = hideAdultContent,
            maximumAge = maximumAge,
            blockUnrated = blockUnrated,
            limit = limit
        )
        return if (recentlyAdded.isNotEmpty()) {
            FeaturedSeriesShelf(mode = FeaturedSeriesMode.RECENTLY_ADDED, items = recentlyAdded)
        } else {
            FeaturedSeriesShelf()
        }
    }

    fun matchPopularShows(
        popularShows: List<TraktRecommendationItem>,
        catalogs: List<XtreamCatalog>,
        hideAdultContent: Boolean = false,
        maximumAge: Int? = null,
        blockUnrated: Boolean = false,
        limit: Int = DEFAULT_SHELF_LIMIT
    ): List<CatalogSeries> = FeaturedShelfMatcher.matchPopular(
        popularItems = popularShows,
        expectedKind = TraktWatchlistKind.SHOW,
        items = catalogs.asSequence().flatMap(::catalogSeriesItems),
        hideAdultContent = hideAdultContent,
        maximumAge = maximumAge,
        blockUnrated = blockUnrated,
        limit = limit
    )

    fun rankRecentlyAddedSeries(
        catalogs: List<XtreamCatalog>,
        firstSeenBySource: Map<String, Map<Int, Long>> = emptyMap(),
        hideAdultContent: Boolean = false,
        maximumAge: Int? = null,
        blockUnrated: Boolean = false,
        limit: Int = DEFAULT_SHELF_LIMIT
    ): List<CatalogSeries> = FeaturedShelfMatcher.rankRecentlyAdded(
        catalogs = catalogs,
        firstSeenBySource = firstSeenBySource,
        extract = ::catalogSeriesItems,
        hideAdultContent = hideAdultContent,
        maximumAge = maximumAge,
        blockUnrated = blockUnrated,
        limit = limit
    )
}

internal object FeaturedMoviesShelfResolver {
    const val DEFAULT_SHELF_LIMIT = FeaturedShelfMatcher.DEFAULT_SHELF_LIMIT

    private fun catalogMovieItems(catalog: XtreamCatalog): Sequence<FeaturedShelfMatcher.IndexedItem<CatalogMovie>> {
        val categoryNames = catalog.movieCategories.associate { it.id to it.name }
        return catalog.movies.asSequence().map { movie ->
            FeaturedShelfMatcher.IndexedItem(
                entry = CatalogMovie(catalog, movie),
                libraryId = movie.libraryId(catalog.sourceId),
                itemId = movie.id,
                name = movie.name,
                categoryName = categoryNames[movie.categoryId],
                ageRating = movie.ageRating,
                tmdbId = movie.tmdbId,
                imdbId = movie.imdbId,
                addedEpochMs = movie.addedEpochMs,
                year = movie.year
            )
        }
    }

    fun resolve(
        traktConnected: Boolean,
        popularMovies: List<TraktRecommendationItem>,
        catalogs: List<XtreamCatalog>,
        firstSeenBySource: Map<String, Map<Int, Long>> = emptyMap(),
        hideAdultContent: Boolean = false,
        maximumAge: Int? = null,
        blockUnrated: Boolean = false,
        limit: Int = DEFAULT_SHELF_LIMIT
    ): FeaturedMoviesShelf {
        if (catalogs.isEmpty() || limit <= 0) return FeaturedMoviesShelf()

        if (traktConnected && popularMovies.isNotEmpty()) {
            val popularMatches = matchPopularMovies(
                popularMovies = popularMovies,
                catalogs = catalogs,
                hideAdultContent = hideAdultContent,
                maximumAge = maximumAge,
                blockUnrated = blockUnrated,
                limit = limit
            )
            if (popularMatches.isNotEmpty()) {
                return FeaturedMoviesShelf(mode = FeaturedSeriesMode.TRAKT_POPULAR, items = popularMatches)
            }
        }

        val recentlyAdded = rankRecentlyAddedMovies(
            catalogs = catalogs,
            firstSeenBySource = firstSeenBySource,
            hideAdultContent = hideAdultContent,
            maximumAge = maximumAge,
            blockUnrated = blockUnrated,
            limit = limit
        )
        return if (recentlyAdded.isNotEmpty()) {
            FeaturedMoviesShelf(mode = FeaturedSeriesMode.RECENTLY_ADDED, items = recentlyAdded)
        } else {
            FeaturedMoviesShelf()
        }
    }

    fun matchPopularMovies(
        popularMovies: List<TraktRecommendationItem>,
        catalogs: List<XtreamCatalog>,
        hideAdultContent: Boolean = false,
        maximumAge: Int? = null,
        blockUnrated: Boolean = false,
        limit: Int = DEFAULT_SHELF_LIMIT
    ): List<CatalogMovie> = FeaturedShelfMatcher.matchPopular(
        popularItems = popularMovies,
        expectedKind = TraktWatchlistKind.MOVIE,
        items = catalogs.asSequence().flatMap(::catalogMovieItems),
        hideAdultContent = hideAdultContent,
        maximumAge = maximumAge,
        blockUnrated = blockUnrated,
        limit = limit
    )

    fun rankRecentlyAddedMovies(
        catalogs: List<XtreamCatalog>,
        firstSeenBySource: Map<String, Map<Int, Long>> = emptyMap(),
        hideAdultContent: Boolean = false,
        maximumAge: Int? = null,
        blockUnrated: Boolean = false,
        limit: Int = DEFAULT_SHELF_LIMIT
    ): List<CatalogMovie> = FeaturedShelfMatcher.rankRecentlyAdded(
        catalogs = catalogs,
        firstSeenBySource = firstSeenBySource,
        extract = ::catalogMovieItems,
        hideAdultContent = hideAdultContent,
        maximumAge = maximumAge,
        blockUnrated = blockUnrated,
        limit = limit
    )
}

package com.fortresstv.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fortresstv.player.library.FastIdMatcher
import com.fortresstv.player.library.UserLibraryStore.Companion.libraryId
import com.fortresstv.player.omdb.OmdbMetadataResolver
import com.fortresstv.player.omdb.OmdbPreFetchTarget
import com.fortresstv.player.omdb.OmdbSettings
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.profile.ParentalControls
import com.fortresstv.player.trakt.FeaturedMoviesShelf
import com.fortresstv.player.trakt.FeaturedMoviesShelfResolver
import com.fortresstv.player.trakt.FeaturedSeriesShelf
import com.fortresstv.player.trakt.FeaturedSeriesShelfResolver
import com.fortresstv.player.trakt.TraktMatchedRecommendations
import com.fortresstv.player.trakt.TraktMatchedWatchlist
import com.fortresstv.player.trakt.TraktPopularMoviesService
import com.fortresstv.player.trakt.TraktPopularMoviesStore
import com.fortresstv.player.trakt.TraktPopularShowsService
import com.fortresstv.player.trakt.TraktPopularShowsStore
import com.fortresstv.player.trakt.TraktRecommendationItem
import com.fortresstv.player.trakt.TraktRecommendationsService
import com.fortresstv.player.trakt.TraktRecommendationsStore
import com.fortresstv.player.trakt.TraktSettingsStore
import com.fortresstv.player.trakt.TraktWatchlistItem
import com.fortresstv.player.trakt.TraktWatchlistService
import com.fortresstv.player.trakt.TraktWatchlistStore
import com.fortresstv.player.xtream.MoviesFirstSeenStore
import com.fortresstv.player.xtream.SeriesFirstSeenStore
import com.fortresstv.player.xtream.XtreamCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Pure shelf builders shared by [FortressLibrary] and unit tests.
 */
internal object HomeLibraryShelfBuilder {
    fun buildFavoriteChannels(
        channels: List<PlaylistChannel>,
        favoriteChannelIds: Set<String>
    ): List<PlaylistChannel> {
        if (favoriteChannelIds.isEmpty()) return emptyList()
        val matcher = FastIdMatcher(favoriteChannelIds)
        return channels.filter { ch -> matcher.matches(ch.libraryId(), ch.sourceItemId) }
    }

    fun buildRecentChannels(
        channels: List<PlaylistChannel>,
        recentChannelIds: List<String>
    ): List<PlaylistChannel> {
        if (recentChannelIds.isEmpty()) return emptyList()
        return recentChannelIds.mapNotNull { id ->
            val raw = id.substringAfterLast(':')
            channels.firstOrNull { ch ->
                ch.libraryId() == id || "${ch.sourceId}:${ch.sourceItemId}" == id || ch.sourceItemId == id || ch.sourceItemId == raw
            }
        }
    }

    fun buildFollowedSeries(
        catalogs: List<XtreamCatalog>,
        followedSeriesIds: Set<String>,
        hideAdultContent: Boolean,
        maximumAge: Int?,
        blockUnrated: Boolean
    ): List<CatalogSeries> {
        if (followedSeriesIds.isEmpty()) return emptyList()
        val matcher = FastIdMatcher(followedSeriesIds)
        return catalogs.flatMap { catalog ->
            val categoryNames = catalog.seriesCategories.associate { it.id to it.name }
            catalog.series.filter {
                matcher.matches(it.libraryId(catalog.sourceId), it.id.toString()) &&
                    (!hideAdultContent || !isAdultContent(it.name, categoryNames[it.categoryId])) &&
                    ParentalControls.isContentAllowedForAge(it.ageRating, maximumAge, blockUnrated)
            }.map { CatalogSeries(catalog, it) }
        }
    }
}

/**
 * State holder for Home's Trakt, featured shelves, and OMDb prefetch orchestration,
 * keeping network/disk I/O and background matching out of [HomeScreen].
 */
internal class HomeShelvesStateHolder(
    private val appContext: Context,
    val traktNamespace: String,
    private val scope: CoroutineScope
) {
    private val traktStore = TraktSettingsStore(appContext, traktNamespace)
    private val traktWatchlistStore = TraktWatchlistStore(appContext, traktNamespace)
    private val traktRecStore = TraktRecommendationsStore(appContext, traktNamespace)
    private val traktPopularStore = TraktPopularShowsStore(appContext, traktNamespace)
    private val traktPopularMoviesStore = TraktPopularMoviesStore(appContext, traktNamespace)
    private val recommendationLock = Mutex()

    val traktConnected: Boolean
        get() = traktStore.isConnected()

    var traktWatchlistItems by mutableStateOf(emptyList<TraktWatchlistItem>())
        private set
    var traktRecItems by mutableStateOf(emptyList<TraktRecommendationItem>())
        private set
    var traktPopularItems by mutableStateOf(emptyList<TraktRecommendationItem>())
        private set
    var traktPopularMovieItems by mutableStateOf(emptyList<TraktRecommendationItem>())
        private set

    var matchedTraktWatchlist by mutableStateOf(TraktMatchedWatchlist())
        private set
    var matchedTraktRecs by mutableStateOf(TraktMatchedRecommendations())
        private set
    var featuredSeriesShelf by mutableStateOf(FeaturedSeriesShelf())
        private set
    var featuredMoviesShelf by mutableStateOf(FeaturedMoviesShelf())
        private set
    var ratingCacheVersion by mutableIntStateOf(0)
        private set

    private var coreSyncJob: Job? = null
    private var popularShowsJob: Job? = null
    private var popularMoviesJob: Job? = null
    private var watchlistMatchJob: Job? = null
    private var recsMatchJob: Job? = null
    private var featuredSeriesJob: Job? = null
    private var featuredMoviesJob: Job? = null
    private var omdbPrefetchJob: Job? = null

    private var lastSyncedConnected: Boolean? = null
    private var lastPopularShowsKey: Pair<Boolean, Boolean>? = null
    private var lastPopularMoviesKey: Pair<Boolean, Boolean>? = null
    private var lastRecsCatalogs: List<XtreamCatalog>? = null
    private var lastRecsParams: List<Any?>? = null
    private var lastWatchlistCatalogs: List<XtreamCatalog>? = null
    private var lastWatchlistParams: List<Any?>? = null
    private var lastFeaturedSeriesCatalogs: List<XtreamCatalog>? = null
    private var lastFeaturedSeriesParams: List<Any?>? = null
    private var lastFeaturedMoviesCatalogs: List<XtreamCatalog>? = null
    private var lastFeaturedMoviesParams: List<Any?>? = null

    fun ensureCoreTraktSynced() {
        val connected = traktConnected
        if (lastSyncedConnected == connected) return
        lastSyncedConnected = connected
        syncCoreTrakt()
    }

    fun syncCoreTrakt() {
        coreSyncJob?.cancel()
        lastSyncedConnected = traktConnected
        if (!traktConnected) {
            traktWatchlistItems = emptyList()
            traktRecItems = emptyList()
            return
        }
        coreSyncJob = scope.launch {
            val (watchlist, recommendations) = withContext(Dispatchers.IO) {
                traktWatchlistStore.loadWatchlist() to traktRecStore.loadRecommendations()
            }
            traktWatchlistItems = watchlist
            traktRecItems = recommendations
            launch {
                TraktWatchlistService.syncWatchlist(appContext, traktNamespace)
                    .onSuccess { traktWatchlistItems = it }
            }
            recommendationLock.withLock {
                TraktRecommendationsService.syncRecommendations(appContext, traktNamespace)
                    .onSuccess { traktRecItems = it }
            }
        }
    }

    fun syncPopularShows(enabled: Boolean) {
        val key = traktConnected to enabled
        if (lastPopularShowsKey == key) return
        lastPopularShowsKey = key
        popularShowsJob?.cancel()
        if (!traktConnected || !enabled) {
            traktPopularItems = emptyList()
            return
        }
        popularShowsJob = scope.launch {
            traktPopularItems = withContext(Dispatchers.IO) { traktPopularStore.loadPopularShows() }
            TraktPopularShowsService.syncPopularShows(appContext, traktNamespace)
                .onSuccess { traktPopularItems = it }
        }
    }

    fun syncPopularMovies(enabled: Boolean) {
        val key = traktConnected to enabled
        if (lastPopularMoviesKey == key) return
        lastPopularMoviesKey = key
        popularMoviesJob?.cancel()
        if (!traktConnected || !enabled) {
            traktPopularMovieItems = emptyList()
            return
        }
        popularMoviesJob = scope.launch {
            traktPopularMovieItems = withContext(Dispatchers.IO) { traktPopularMoviesStore.loadPopularMovies() }
            TraktPopularMoviesService.syncPopularMovies(appContext, traktNamespace)
                .onSuccess { traktPopularMovieItems = it }
        }
    }

    fun updateRecommendations(items: List<TraktRecommendationItem>) {
        traktRecItems = items
    }

    fun recomputeWatchlist(showTraktWatchlist: Boolean, catalogs: List<XtreamCatalog>) {
        val items = traktWatchlistItems
        val params = listOf(showTraktWatchlist, items)
        if (sameCatalogRefs(lastWatchlistCatalogs, catalogs) && lastWatchlistParams == params) {
            return
        }
        lastWatchlistCatalogs = catalogs
        lastWatchlistParams = params
        watchlistMatchJob?.cancel()
        if (!showTraktWatchlist || items.isEmpty()) {
            matchedTraktWatchlist = TraktMatchedWatchlist()
            return
        }
        watchlistMatchJob = scope.launch {
            matchedTraktWatchlist = withContext(Dispatchers.Default) {
                TraktWatchlistService.matchWatchlist(items, catalogs)
            }
        }
    }

    fun recomputeRecommendations(
        showTraktRecs: Boolean,
        catalogs: List<XtreamCatalog>,
        hideAdultContent: Boolean,
        maximumAge: Int?,
        blockUnrated: Boolean
    ) {
        val items = traktRecItems
        val params = listOf(showTraktRecs, items, hideAdultContent, maximumAge, blockUnrated)
        if (sameCatalogRefs(lastRecsCatalogs, catalogs) && lastRecsParams == params) {
            return
        }
        lastRecsCatalogs = catalogs
        lastRecsParams = params
        recsMatchJob?.cancel()
        if (!showTraktRecs || items.isEmpty() || catalogs.isEmpty()) {
            matchedTraktRecs = TraktMatchedRecommendations()
            return
        }
        recsMatchJob = scope.launch {
            matchedTraktRecs = withContext(Dispatchers.Default) {
                TraktRecommendationsService.matchRecommendations(
                    recommendations = items,
                    catalogs = catalogs,
                    hideAdultContent = hideAdultContent,
                    maximumAge = maximumAge,
                    blockUnrated = blockUnrated
                )
            }
        }
    }

    fun recomputeFeaturedSeries(
        showFeaturedSeries: Boolean,
        catalogs: List<XtreamCatalog>,
        hideAdultContent: Boolean,
        maximumAge: Int?,
        blockUnrated: Boolean
    ) {
        val popular = traktPopularItems
        val connected = traktConnected
        val params = listOf(showFeaturedSeries, connected, popular, hideAdultContent, maximumAge, blockUnrated)
        if (sameCatalogRefs(lastFeaturedSeriesCatalogs, catalogs) && lastFeaturedSeriesParams == params) {
            return
        }
        lastFeaturedSeriesCatalogs = catalogs
        lastFeaturedSeriesParams = params
        featuredSeriesJob?.cancel()
        if (!showFeaturedSeries || catalogs.isEmpty()) {
            featuredSeriesShelf = FeaturedSeriesShelf()
            return
        }
        featuredSeriesJob = scope.launch {
            featuredSeriesShelf = withContext(Dispatchers.Default) {
                val firstSeenStore = SeriesFirstSeenStore(appContext.filesDir)
                val firstSeenBySource = withContext(Dispatchers.IO) {
                    catalogs.associate { it.sourceId to firstSeenStore.loadFirstSeen(it.sourceId) }
                }
                FeaturedSeriesShelfResolver.resolve(
                    traktConnected = connected,
                    popularShows = popular,
                    catalogs = catalogs,
                    firstSeenBySource = firstSeenBySource,
                    hideAdultContent = hideAdultContent,
                    maximumAge = maximumAge,
                    blockUnrated = blockUnrated
                )
            }
        }
    }

    fun recomputeFeaturedMovies(
        showFeaturedMovies: Boolean,
        catalogs: List<XtreamCatalog>,
        hideAdultContent: Boolean,
        maximumAge: Int?,
        blockUnrated: Boolean
    ) {
        val popular = traktPopularMovieItems
        val connected = traktConnected
        val params = listOf(showFeaturedMovies, connected, popular, hideAdultContent, maximumAge, blockUnrated)
        if (sameCatalogRefs(lastFeaturedMoviesCatalogs, catalogs) && lastFeaturedMoviesParams == params) {
            return
        }
        lastFeaturedMoviesCatalogs = catalogs
        lastFeaturedMoviesParams = params
        featuredMoviesJob?.cancel()
        if (!showFeaturedMovies || catalogs.isEmpty()) {
            featuredMoviesShelf = FeaturedMoviesShelf()
            return
        }
        featuredMoviesJob = scope.launch {
            featuredMoviesShelf = withContext(Dispatchers.Default) {
                val firstSeenStore = MoviesFirstSeenStore(appContext.filesDir)
                val firstSeenBySource = withContext(Dispatchers.IO) {
                    catalogs.associate { it.sourceId to firstSeenStore.loadFirstSeen(it.sourceId) }
                }
                FeaturedMoviesShelfResolver.resolve(
                    traktConnected = connected,
                    popularMovies = popular,
                    catalogs = catalogs,
                    firstSeenBySource = firstSeenBySource,
                    hideAdultContent = hideAdultContent,
                    maximumAge = maximumAge,
                    blockUnrated = blockUnrated
                )
            }
        }
    }

    fun prefetchOmdbForFeatured(omdbSettings: OmdbSettings) {
        omdbPrefetchJob?.cancel()
        ratingCacheVersion++
        if (!omdbSettings.enabled || omdbSettings.apiKey.isBlank()) return
        val targets = buildList {
            featuredMoviesShelf.items.take(15).forEach { entry ->
                add(OmdbPreFetchTarget("movie", entry.movie.name, entry.movie.year, entry.movie.imdbId))
            }
            featuredSeriesShelf.items.take(15).forEach { entry ->
                add(OmdbPreFetchTarget("series", entry.series.name, entry.series.year, entry.series.imdbId))
            }
        }
        if (targets.isEmpty()) return
        omdbPrefetchJob = scope.launch {
            OmdbMetadataResolver.prefetchCuratedBatch(appContext, omdbSettings, targets)
            ratingCacheVersion++
        }
    }
}

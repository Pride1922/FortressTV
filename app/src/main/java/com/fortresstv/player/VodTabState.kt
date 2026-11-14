package com.fortresstv.player

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.xtream.XtreamCatalog

internal fun sameCatalogRefs(a: List<XtreamCatalog>?, b: List<XtreamCatalog>): Boolean {
    if (a === b) return true
    if (a == null || a.size != b.size) return false
    for (i in a.indices) {
        if (a[i] !== b[i]) return false
    }
    return true
}

@Stable
internal class VodTabFilterState {
    val queryState: MutableState<String> = mutableStateOf("")
    val appliedQueryState: MutableState<String> = mutableStateOf("")
    val sortOptionState: MutableState<VodSortOption> = mutableStateOf(VodSortOption.ALPHABETICAL)
    val filterTopRatedState: MutableState<Boolean> = mutableStateOf(false)
    val filterFreshState: MutableState<Boolean> = mutableStateOf(false)
    val filterPersonalState: MutableState<Boolean> = mutableStateOf(false)
    val selectedCategoryState: MutableState<CatalogCategory?> = mutableStateOf(null)
}

@Stable
internal class SeriesTabState {
    val filters = VodTabFilterState()

    val allMatchingSeriesState: MutableState<List<CatalogSeries>> = mutableStateOf(emptyList())
    val topRatedCountState: MutableIntState = mutableIntStateOf(0)
    val freshCountState: MutableIntState = mutableIntStateOf(0)
    val ratedCountState: MutableIntState = mutableIntStateOf(0)
    val totalCountState: MutableIntState = mutableIntStateOf(0)
    val isCatalogLoadingState: MutableState<Boolean> = mutableStateOf(true)
    val fullyWatchedSeriesKeysState: MutableState<Set<String>> = mutableStateOf(emptySet())
    val followedSeriesState: MutableState<List<CatalogSeries>> = mutableStateOf(emptyList())
    val followedCountState: MutableIntState = mutableIntStateOf(0)
    var cachedSeriesEpisodeIds: Map<String, List<String>> = emptyMap()

    private var lastCatalogs: List<XtreamCatalog>? = null
    private var lastParams: List<Any?>? = null
    private var lastEpisodeCacheCatalogs: List<XtreamCatalog>? = null
    private var lastEpisodeCacheProfiles: List<PlaylistProfile>? = null
    private var lastWatchedIds: Set<String>? = null
    private var lastFollowedMatching: List<CatalogSeries>? = null
    private var lastFollowedIds: Set<String>? = null
    private var lastScrollParams: List<Any?>? = null
    private var hasInitializedScroll = false

    fun needsRecompute(
        catalogs: List<XtreamCatalog>,
        params: List<Any?>
    ): Boolean {
        if (!isCatalogLoadingState.value && sameCatalogRefs(lastCatalogs, catalogs) && lastParams == params) {
            return false
        }
        return true
    }

    fun markComputed(
        catalogs: List<XtreamCatalog>,
        params: List<Any?>
    ) {
        lastCatalogs = catalogs
        lastParams = params
    }

    fun needsEpisodeCacheRecompute(
        catalogs: List<XtreamCatalog>,
        profiles: List<PlaylistProfile>
    ): Boolean {
        return !(sameCatalogRefs(lastEpisodeCacheCatalogs, catalogs) && lastEpisodeCacheProfiles === profiles)
    }

    fun markEpisodeCacheComputed(
        catalogs: List<XtreamCatalog>,
        profiles: List<PlaylistProfile>
    ) {
        lastEpisodeCacheCatalogs = catalogs
        lastEpisodeCacheProfiles = profiles
    }

    fun needsWatchedRecompute(
        catalogs: List<XtreamCatalog>,
        profiles: List<PlaylistProfile>,
        watchedItemIds: Set<String>
    ): Boolean {
        return !(sameCatalogRefs(lastEpisodeCacheCatalogs, catalogs) &&
            lastEpisodeCacheProfiles === profiles &&
            lastWatchedIds == watchedItemIds)
    }

    fun markWatchedComputed(
        watchedItemIds: Set<String>
    ) {
        lastWatchedIds = watchedItemIds
    }

    fun needsFollowedRecompute(
        allMatchingSeries: List<CatalogSeries>,
        followedSeriesIds: Set<String>
    ): Boolean {
        return !(lastFollowedMatching === allMatchingSeries && lastFollowedIds == followedSeriesIds)
    }

    fun markFollowedComputed(
        allMatchingSeries: List<CatalogSeries>,
        followedSeriesIds: Set<String>
    ) {
        lastFollowedMatching = allMatchingSeries
        lastFollowedIds = followedSeriesIds
    }

    fun consumeScrollReset(params: List<Any?>): Boolean {
        if (!hasInitializedScroll) {
            hasInitializedScroll = true
            lastScrollParams = params
            return false
        }
        if (lastScrollParams == params) return false
        lastScrollParams = params
        return true
    }
}

@Stable
internal class MoviesTabState {
    val filters = VodTabFilterState()

    val allMatchingMoviesState: MutableState<List<CatalogMovie>> = mutableStateOf(emptyList())
    val topRatedCountState: MutableIntState = mutableIntStateOf(0)
    val freshCountState: MutableIntState = mutableIntStateOf(0)
    val ratedCountState: MutableIntState = mutableIntStateOf(0)
    val totalCountState: MutableIntState = mutableIntStateOf(0)
    val isCatalogLoadingState: MutableState<Boolean> = mutableStateOf(true)
    val watchlistMoviesState: MutableState<List<CatalogMovie>> = mutableStateOf(emptyList())
    val watchlistCountState: MutableIntState = mutableIntStateOf(0)

    private var lastCatalogs: List<XtreamCatalog>? = null
    private var lastParams: List<Any?>? = null
    private var lastWatchlistMatching: List<CatalogMovie>? = null
    private var lastWatchlistIds: Set<String>? = null
    private var lastScrollParams: List<Any?>? = null
    private var hasInitializedScroll = false

    fun needsWatchlistRecompute(
        allMatchingMovies: List<CatalogMovie>,
        movieWatchlistIds: Set<String>
    ): Boolean {
        return !(lastWatchlistMatching === allMatchingMovies && lastWatchlistIds == movieWatchlistIds)
    }

    fun markWatchlistComputed(
        allMatchingMovies: List<CatalogMovie>,
        movieWatchlistIds: Set<String>
    ) {
        lastWatchlistMatching = allMatchingMovies
        lastWatchlistIds = movieWatchlistIds
    }

    fun needsRecompute(
        catalogs: List<XtreamCatalog>,
        params: List<Any?>
    ): Boolean {
        if (!isCatalogLoadingState.value && sameCatalogRefs(lastCatalogs, catalogs) && lastParams == params) {
            return false
        }
        return true
    }

    fun markComputed(
        catalogs: List<XtreamCatalog>,
        params: List<Any?>
    ) {
        lastCatalogs = catalogs
        lastParams = params
    }

    fun consumeScrollReset(params: List<Any?>): Boolean {
        if (!hasInitializedScroll) {
            hasInitializedScroll = true
            lastScrollParams = params
            return false
        }
        if (lastScrollParams == params) return false
        lastScrollParams = params
        return true
    }
}

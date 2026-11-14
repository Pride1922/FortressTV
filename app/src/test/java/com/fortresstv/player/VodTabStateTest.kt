package com.fortresstv.player

import com.fortresstv.player.xtream.XtreamCatalog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VodTabStateTest {

    @Test
    fun seriesTabStateSkipsRecomputeWhenCatalogRefsAndParamsMatch() {
        val state = SeriesTabState()
        val catalogs = listOf(XtreamCatalog(sourceId = "s1", sourceName = "Source 1"))
        val params = listOf("", null, false, null, false, VodSortOption.ALPHABETICAL, false, false, 0)

        assertTrue(state.needsRecompute(catalogs, params))

        state.isCatalogLoadingState.value = false
        state.markComputed(catalogs, params)

        assertFalse(state.needsRecompute(catalogs, params))
        assertFalse(state.needsRecompute(catalogs.toList(), params))

        val changedSortParams = listOf("", null, false, null, false, VodSortOption.RATING, false, false, 0)
        assertTrue(state.needsRecompute(catalogs, changedSortParams))

        val newCatalogs = listOf(XtreamCatalog(sourceId = "s1", sourceName = "Source 1"))
        assertTrue(state.needsRecompute(newCatalogs, params))
    }

    @Test
    fun moviesTabStatePreservesScrollOnInitialEntryAndResetsOnFilterChange() {
        val state = MoviesTabState()
        val initialParams = listOf("", null, VodSortOption.ALPHABETICAL, false, false, false)

        // Initial composition (e.g. switching back to Movies tab) must not reset preserved scroll position
        assertFalse(state.consumeScrollReset(initialParams))
        // Re-evaluating same params does not reset scroll
        assertFalse(state.consumeScrollReset(initialParams))

        // Changing a filter/sort resets scroll to top
        val filteredParams = listOf("", null, VodSortOption.ALPHABETICAL, true, false, false)
        assertTrue(state.consumeScrollReset(filteredParams))
        assertFalse(state.consumeScrollReset(filteredParams))
    }
}

package com.fortresstv.player

import com.fortresstv.player.library.UserLibraryStore.Companion.libraryId
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamCategory
import com.fortresstv.player.xtream.XtreamSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLibraryShelfBuilderTest {

    private val ch1 = PlaylistChannel(
        position = 0,
        name = "News One",
        streamUrl = "http://example.com/1.m3u8",
        sourceId = "srcA",
        sourceItemId = "101"
    )
    private val ch2 = PlaylistChannel(
        position = 1,
        name = "Sports Two",
        streamUrl = "http://example.com/2.m3u8",
        sourceId = "srcA",
        sourceItemId = "102"
    )
    private val ch3 = PlaylistChannel(
        position = 2,
        name = "Cinema Three",
        streamUrl = "http://example.com/3.m3u8",
        sourceId = "srcB",
        sourceItemId = "201"
    )

    @Test
    fun buildFavoriteChannelsReturnsEmptyWhenNoFavorites() {
        assertTrue(HomeLibraryShelfBuilder.buildFavoriteChannels(listOf(ch1, ch2), emptySet()).isEmpty())
    }

    @Test
    fun buildFavoriteChannelsMatchesLibraryIdAndSourceItemId() {
        val result = HomeLibraryShelfBuilder.buildFavoriteChannels(
            channels = listOf(ch1, ch2, ch3),
            favoriteChannelIds = setOf(requireNotNull(ch1.libraryId()), ch3.sourceItemId)
        )
        assertEquals(listOf(ch1, ch3), result)
    }

    @Test
    fun buildRecentChannelsPreservesOrderAndMatchesVariousIdForms() {
        val result = HomeLibraryShelfBuilder.buildRecentChannels(
            channels = listOf(ch1, ch2, ch3),
            recentChannelIds = listOf("srcB:201", requireNotNull(ch1.libraryId()), "legacy:102")
        )
        assertEquals(listOf(ch3, ch1, ch2), result)
    }

    @Test
    fun buildFollowedSeriesFiltersAdultAndAgeRestrictedItems() {
        val catalog = XtreamCatalog(
            sourceId = "src1",
            sourceName = "Source 1",
            seriesCategories = listOf(
                XtreamCategory("1", "Drama"),
                XtreamCategory("2", "XXX Adult")
            ),
            series = listOf(
                XtreamSeries(10, "Family Show", "1", null, "8.0", "2022", null, ageRating = "TV-PG"),
                XtreamSeries(11, "Adult Show", "2", null, "7.0", "2022", null, ageRating = "TV-MA"),
                XtreamSeries(12, "Mature Show", "1", null, "8.5", "2022", null, ageRating = "TV-MA"),
                XtreamSeries(13, "Unrated Show", "1", null, "7.5", "2022", null, ageRating = null)
            )
        )
        val followedIds = setOf(
            catalog.series[0].libraryId("src1"),
            catalog.series[1].libraryId("src1"),
            "12",
            "13"
        )

        val result = HomeLibraryShelfBuilder.buildFollowedSeries(
            catalogs = listOf(catalog),
            followedSeriesIds = followedIds,
            hideAdultContent = true,
            maximumAge = 14,
            blockUnrated = true
        )

        assertEquals(listOf("Family Show"), result.map { it.series.name })
    }
}

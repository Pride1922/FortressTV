package com.fortresstv.player.library

import com.fortresstv.player.library.UserLibraryStore.Companion.libraryId
import com.fortresstv.player.xtream.XtreamEpisode
import com.fortresstv.player.xtream.XtreamMovie
import com.fortresstv.player.xtream.XtreamSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UserLibraryIdentityTest {
    @Test
    fun movieAndSeriesIdsIncludeTypeAndSource() {
        val movie = XtreamMovie(42, "Movie", null, null, "mp4", null, null, null)
        val series = XtreamSeries(42, "Series", null, null, null, null, null)
        val episode = XtreamEpisode("ep-100", "Pilot", 1, 1, "mkv", null, null)

        assertEquals("movie:provider-a:42", movie.libraryId("provider-a"))
        assertEquals("series:provider-a:42", series.libraryId("provider-a"))
        assertEquals("episode:provider-a:ep-100", episode.libraryId("provider-a"))
        assertNotEquals(movie.libraryId("provider-a"), movie.libraryId("provider-b"))
        assertNotEquals(movie.libraryId("provider-a"), series.libraryId("provider-a"))
        assertNotEquals(episode.libraryId("provider-a"), episode.libraryId("provider-b"))
    }

    @Test
    fun resolvesNextUnwatchedEpisodeInOrder() {
        val ep1 = XtreamEpisode("1", "Ep 1", 1, 1, "mkv", null, null)
        val ep2 = XtreamEpisode("2", "Ep 2", 1, 2, "mkv", null, null)
        val ep3 = XtreamEpisode("3", "Ep 3", 2, 1, "mkv", null, null)
        val episodes = listOf(ep3, ep1, ep2)

        val sorted = episodes.sortedWith(compareBy({ it.season }, { it.episodeNumber }))
        val watched = setOf(ep1.libraryId("source-1"))

        val next = sorted.firstOrNull { it.libraryId("source-1") !in watched }
        assertEquals(ep2, next)

        val allWatched = setOf(ep1.libraryId("source-1"), ep2.libraryId("source-1"), ep3.libraryId("source-1"))
        val noneLeft = sorted.firstOrNull { it.libraryId("source-1") !in allWatched }
        assertEquals(null, noneLeft)
    }
}

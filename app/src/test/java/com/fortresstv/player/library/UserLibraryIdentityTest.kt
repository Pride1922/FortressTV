package com.fortresstv.player.library

import com.fortresstv.player.library.UserLibraryStore.Companion.libraryId
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

        assertEquals("movie:provider-a:42", movie.libraryId("provider-a"))
        assertEquals("series:provider-a:42", series.libraryId("provider-a"))
        assertNotEquals(movie.libraryId("provider-a"), movie.libraryId("provider-b"))
        assertNotEquals(movie.libraryId("provider-a"), series.libraryId("provider-a"))
    }
}

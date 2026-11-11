package com.fortresstv.player

import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamMovie
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieCatalogDeduplicationTest {
    @Test
    fun mergesAcrossSourcesWithDifferentArtworkAndMetadataIds() {
        val first = entry("first", "10 Minutes Gone (2019)", null, tmdb = "123")
        val second = entry("second", "10 minutes gone", "2019", imdb = "tt456")
        val third = entry("third", "Translated title", null, imdb = "tt456")
        assertEquals(listOf(first), deduplicateCatalogMovies(listOf(first, second, third)))
    }

    @Test
    fun preservesDifferentYearsAndConflictingIds() {
        val original = entry("first", "Movie", "2019", tmdb = "123")
        val differentId = entry("second", "Movie", "2019", tmdb = "456")
        val differentYear = entry("third", "Movie", "2022")
        val entries = listOf(original, differentId, differentYear)
        assertEquals(entries, deduplicateCatalogMovies(entries))
    }

    private fun entry(source: String, title: String, year: String?, tmdb: String? = null, imdb: String? = null) =
        CatalogMovie(
            XtreamCatalog(source, source),
            XtreamMovie(1, title, null, "https://example.com/$source", "mkv", null, year, null, tmdbId = tmdb, imdbId = imdb)
        )
}

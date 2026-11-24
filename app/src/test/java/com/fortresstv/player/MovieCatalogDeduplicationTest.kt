package com.fortresstv.player

import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamMovie
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieCatalogDeduplicationTest {
    @Test
    fun versionGroupsRetainSourceIdentityAndProviderOrder() {
        val first = entry("first", "Movie", "2019", tmdb = "123")
        val second = entry("second", "Movie [4K]", "2019", tmdb = "123")
        val remake = entry("third", "Movie", "2022", tmdb = "456")
        assertEquals(listOf(listOf(first, second), listOf(remake)), groupCatalogMovieVersions(listOf(first, second, first, remake)))
    }

    @Test
    fun versionGroupsDoNotMergeConflictingIdsOrUnknownCrossSourceYears() {
        val first = entry("first", "Movie", null)
        val second = entry("second", "Movie", null)
        assertEquals(listOf(listOf(first), listOf(second)), groupCatalogMovieVersions(listOf(first, second)))
    }
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

    @Test
    fun deduplicatesIntraSourceQualityAndLanguageVariants() {
        val cat = XtreamCatalog("src1", "Source 1")
        val pt4k = CatalogMovie(cat, XtreamMovie(1, "PT: Gladiator (2000) [4K]", null, "https://example.com/1", "mkv", null, "2000", null))
        val enHd = CatalogMovie(cat, XtreamMovie(2, "EN | Gladiator (2000) 1080p", null, "https://example.com/2", "mkv", null, null, null))
        val leg = CatalogMovie(cat, XtreamMovie(3, "Gladiator (2000) [LEGENDADO] HEVC", null, "https://example.com/3", "mkv", null, "2000", null))
        assertEquals(listOf(pt4k), deduplicateCatalogMovies(listOf(pt4k, enHd, leg)))
    }

    private fun entry(source: String, title: String, year: String?, tmdb: String? = null, imdb: String? = null) =
        CatalogMovie(
            XtreamCatalog(source, source),
            XtreamMovie(1, title, null, "https://example.com/$source", "mkv", null, year, null, tmdbId = tmdb, imdbId = imdb)
        )
}

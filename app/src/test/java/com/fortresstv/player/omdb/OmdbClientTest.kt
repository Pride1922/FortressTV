package com.fortresstv.player.omdb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OmdbClientTest {

    @Test
    fun normalizeImdbId_handlesVariousFormats() {
        assertEquals("tt0903747", OmdbClient.normalizeImdbId("tt0903747"))
        assertEquals("tt0903747", OmdbClient.normalizeImdbId("TT0903747"))
        assertEquals("tt0903747", OmdbClient.normalizeImdbId("0903747"))
        assertEquals("tt1234567", OmdbClient.normalizeImdbId("  tt1234567  "))
        assertNull(OmdbClient.normalizeImdbId(null))
        assertNull(OmdbClient.normalizeImdbId(""))
        assertNull(OmdbClient.normalizeImdbId("   "))
        assertNull(OmdbClient.normalizeImdbId("invalid"))
    }

    @Test
    fun cleanSearchTitle_stripsTechnicalTagsAndQualityPrefixes() {
        assertEquals("Breaking Bad", OmdbClient.cleanSearchTitle("Breaking Bad [FHD] (1080p)"))
        assertEquals("Oppenheimer", OmdbClient.cleanSearchTitle("Oppenheimer.2023.4K.HEVC.H265"))
        assertEquals("Inception", OmdbClient.cleanSearchTitle("PT: Inception [BluRay]"))
        assertEquals("The Dark Knight", OmdbClient.cleanSearchTitle("The.Dark.Knight.UHD.Remux"))
        assertEquals("The Mentalist", OmdbClient.cleanSearchTitle("The Mentalist (Legendado)"))
        assertEquals("Gladiator II", OmdbClient.cleanSearchTitle("PT | Gladiator II (2024) [IMAX]"))
        assertEquals("Breaking Bad", OmdbClient.cleanSearchTitle("PT: Breaking Bad S01-S05 [1080p] DUAL"))
    }

    @Test
    fun extractYear_identifiesYearFromFallbackOrTitle() {
        assertEquals("2023", OmdbClient.extractYear("Oppenheimer", "2023"))
        assertEquals("2024", OmdbClient.extractYear("Gladiator II (2024)", null))
        assertEquals("2010", OmdbClient.extractYear("Inception.2010.1080p", null))
        assertNull(OmdbClient.extractYear("The Mentalist", null))
    }

    @Test
    fun metadataHasRatingsAndAwardsEvaluatesCorrectly() {
        val full = OmdbMetadata(
            title = "Breaking Bad",
            imdbRating = "9.5",
            imdbVotes = "2,100,000",
            rottenTomatoes = "96%",
            metascore = "87",
            awards = "Won 16 Primetime Emmys. 159 wins & 247 nominations total"
        )
        assertTrue(full.hasRatings)
        assertTrue(full.hasAwards)

        val rtOnly = OmdbMetadata(
            title = "Indie Film",
            rottenTomatoes = "88%"
        )
        assertTrue(rtOnly.hasRatings)
        assertFalse(rtOnly.hasAwards)

        val empty = OmdbMetadata(
            title = "Unknown",
            imdbRating = "N/A",
            rottenTomatoes = null,
            metascore = "N/A",
            awards = "N/A"
        )
        assertFalse(empty.hasRatings)
        assertFalse(empty.hasAwards)
    }
}

package com.fortresstv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VodMetadataTest {
    @Test
    fun formatVodRatingRoundsToOneDecimal() {
        assertEquals("8.7", formatVodRating("8.661"))
        assertEquals("8.0", formatVodRating("8"))
        assertNull(formatVodRating("N/A"))
    }

    @Test
    fun formatsRatingsToOneDecimalPlace() {
        assertEquals("2022  ★ 7.4/10", formatVodMetadata("2022", "7.446"))
        assertEquals("★ 7.4/10", formatVodMetadata(null, "7.446/10"))
        assertEquals("★ 7.4/10", formatVodMetadata(null, "7,446"))
        assertEquals("★ 7.0/10", formatVodMetadata(null, "7"))
    }

    @Test
    fun omitsMissingOrInvalidRatings() {
        assertEquals("2022", formatVodMetadata("2022", null))
        assertEquals("2022", formatVodMetadata("2022", "N/A"))
        assertEquals("", formatVodMetadata(null, ""))
    }

    @Test
    fun showsTheYearOfAFullReleaseDate() {
        assertEquals("2008  ★ 8.2/10", formatVodMetadata("2008-09-23", "8.2"))
        assertEquals("2008", displayYear("2008-09-23"))
        assertEquals("2008", displayYear("23/09/2008"))
        assertEquals("2008", displayYear(" 2008.09.23 "))
        assertEquals("2024", displayYear("2024"))
    }

    @Test
    fun keepsYearTextThatIsNotADate() {
        assertEquals("2019–2023", displayYear("2019–2023"))
        assertEquals("TBA", displayYear("TBA"))
        assertEquals("123", displayYear("123"))
        assertNull(displayYear("  "))
        assertNull(displayYear(null))
    }
}

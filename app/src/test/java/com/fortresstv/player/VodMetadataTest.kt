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
}

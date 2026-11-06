package com.fortresstv.player.profile

import org.junit.Assert.*
import org.junit.Test

class ParentalControlsTest {

    @Test
    fun parsesStandardUsRatings() {
        assertEquals(0, ParentalControls.parseAgeRating("G"))
        assertEquals(0, ParentalControls.parseAgeRating("TV-G"))
        assertEquals(0, ParentalControls.parseAgeRating("TV-Y"))
        assertEquals(6, ParentalControls.parseAgeRating("TV-Y7"))
        assertEquals(6, ParentalControls.parseAgeRating("TV-Y7-FV"))
        assertEquals(10, ParentalControls.parseAgeRating("PG"))
        assertEquals(10, ParentalControls.parseAgeRating("TV-PG"))
        assertEquals(12, ParentalControls.parseAgeRating("PG-13"))
        assertEquals(14, ParentalControls.parseAgeRating("TV-14"))
        assertEquals(18, ParentalControls.parseAgeRating("R"))
        assertEquals(18, ParentalControls.parseAgeRating("NC-17"))
        assertEquals(18, ParentalControls.parseAgeRating("TV-MA"))
    }

    @Test
    fun parsesPortugueseRatings() {
        assertEquals(0, ParentalControls.parseAgeRating("Para todos"))
        assertEquals(0, ParentalControls.parseAgeRating("AL"))
        assertEquals(6, ParentalControls.parseAgeRating("M/6"))
        assertEquals(12, ParentalControls.parseAgeRating("M/12"))
        assertEquals(14, ParentalControls.parseAgeRating("M/14"))
        assertEquals(16, ParentalControls.parseAgeRating("M/16"))
        assertEquals(18, ParentalControls.parseAgeRating("M/18"))
    }

    @Test
    fun parsesInternationalAndNumericRatings() {
        assertEquals(0, ParentalControls.parseAgeRating("U"))
        assertEquals(6, ParentalControls.parseAgeRating("6+"))
        assertEquals(12, ParentalControls.parseAgeRating("+12"))
        assertEquals(16, ParentalControls.parseAgeRating("16"))
        assertEquals(18, ParentalControls.parseAgeRating("18+"))
    }

    @Test
    fun unratedReturnsNull() {
        assertNull(ParentalControls.parseAgeRating(null))
        assertNull(ParentalControls.parseAgeRating(""))
        assertNull(ParentalControls.parseAgeRating("   "))
        assertNull(ParentalControls.parseAgeRating("NR"))
        assertNull(ParentalControls.parseAgeRating("UNRATED"))
        assertNull(ParentalControls.parseAgeRating("Random String"))
    }

    @Test
    fun contentAllowedEvaluationWithAgeCeiling() {
        // Child under 12
        val maxAge = 12
        val blockUnrated = true

        // Permitted
        assertTrue(ParentalControls.isContentAllowedForAge("G", maxAge, blockUnrated))
        assertTrue(ParentalControls.isContentAllowedForAge("TV-Y7", maxAge, blockUnrated))
        assertTrue(ParentalControls.isContentAllowedForAge("PG", maxAge, blockUnrated))
        assertTrue(ParentalControls.isContentAllowedForAge("M/12", maxAge, blockUnrated))

        // Blocked: higher age
        assertFalse(ParentalControls.isContentAllowedForAge("M/14", maxAge, blockUnrated))
        assertFalse(ParentalControls.isContentAllowedForAge("TV-14", maxAge, blockUnrated))
        assertFalse(ParentalControls.isContentAllowedForAge("M/16", maxAge, blockUnrated))
        assertFalse(ParentalControls.isContentAllowedForAge("R", maxAge, blockUnrated))
        assertFalse(ParentalControls.isContentAllowedForAge("18+", maxAge, blockUnrated))

        // Blocked: unrated content fails closed when blockUnrated is true
        assertFalse(ParentalControls.isContentAllowedForAge(null, maxAge, blockUnrated))
        assertFalse(ParentalControls.isContentAllowedForAge("NR", maxAge, blockUnrated))
        assertFalse(ParentalControls.isContentAllowedForAge("Unknown", maxAge, blockUnrated))
    }

    @Test
    fun adultProfileWithoutRestrictionsAllowsEverything() {
        assertTrue(ParentalControls.isContentAllowedForAge("R", null, false))
        assertTrue(ParentalControls.isContentAllowedForAge("M/18", null, false))
        assertTrue(ParentalControls.isContentAllowedForAge(null, null, false))
        assertTrue(ParentalControls.isContentAllowedForAge("UNRATED", null, false))
    }

    @Test
    fun adultPatternDetectionMatchesKeywords() {
        assertTrue(ParentalControls.isAdultPattern("XXX Channel", "General"))
        assertTrue(ParentalControls.isAdultPattern("Movie", "Adultos"))
        assertTrue(ParentalControls.isAdultPattern("Action 18+", "Movies"))
        assertTrue(ParentalControls.isAdultPattern("Late Night Porn", "TV"))
        assertFalse(ParentalControls.isAdultPattern("Kids Cartoons", "Animation"))
        assertFalse(ParentalControls.isAdultPattern("Documentary", "Science"))
    }
}

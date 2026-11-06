package com.fortresstv.player.profile

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class HouseholdProfileTest {

    @Test
    fun defaultAdultProfileHasSensibleDefaults() {
        val adult = HouseholdProfile.defaultAdult("Parent", "pt-PT")
        assertEquals("Parent", adult.name)
        assertEquals(ProfileKind.ADULT, adult.kind)
        assertEquals("pt-PT", adult.language)
        assertFalse(adult.hideAdultContent)
        assertNull(adult.maximumAge)
        assertFalse(adult.blockUnrated)
        assertTrue(adult.isAdult)
        assertFalse(adult.isChild)
    }

    @Test
    fun childProfileEnforcesRestrictions() {
        val child = HouseholdProfile(
            name = "Kid",
            kind = ProfileKind.CHILD,
            language = "en",
            hideAdultContent = true,
            maximumAge = 12,
            blockUnrated = true
        )
        assertEquals("Kid", child.name)
        assertEquals(ProfileKind.CHILD, child.kind)
        assertTrue(child.hideAdultContent)
        assertEquals(12, child.maximumAge)
        assertTrue(child.blockUnrated)
        assertTrue(child.isChild)
        assertFalse(child.isAdult)
    }

    @Test(expected = IllegalArgumentException::class)
    fun childProfileFailsWithoutMaximumAge() {
        HouseholdProfile(
            name = "Kid",
            kind = ProfileKind.CHILD,
            maximumAge = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun childProfileFailsIfAdultContentNotHidden() {
        HouseholdProfile(
            name = "Kid",
            kind = ProfileKind.CHILD,
            hideAdultContent = false,
            maximumAge = 10,
            blockUnrated = true
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun childProfileFailsIfUnratedNotBlocked() {
        HouseholdProfile(
            name = "Kid",
            kind = ProfileKind.CHILD,
            hideAdultContent = true,
            maximumAge = 10,
            blockUnrated = false
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankProfileNameIsRejected() {
        HouseholdProfile(name = "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun overlyLongProfileNameIsRejected() {
        HouseholdProfile(name = "a".repeat(61))
    }

    @Test
    fun jsonRoundTripPreservesAllFields() {
        val profile = HouseholdProfile(
            id = "11111111-2222-3333-4444-555555555555",
            householdId = "00000000-0000-0000-0000-000000000000",
            name = "Teens",
            kind = ProfileKind.CHILD,
            language = "pt-PT",
            hideAdultContent = true,
            maximumAge = 14,
            blockUnrated = true
        )
        val json = profile.encode()
        val decoded = HouseholdProfile.decode(json)

        assertEquals(profile.id, decoded.id)
        assertEquals(profile.householdId, decoded.householdId)
        assertEquals(profile.name, decoded.name)
        assertEquals(profile.kind, decoded.kind)
        assertEquals(profile.language, decoded.language)
        assertEquals(profile.hideAdultContent, decoded.hideAdultContent)
        assertEquals(profile.maximumAge, decoded.maximumAge)
        assertEquals(profile.blockUnrated, decoded.blockUnrated)
    }
}

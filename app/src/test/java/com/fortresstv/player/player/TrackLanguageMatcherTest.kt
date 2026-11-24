package com.fortresstv.player.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackLanguageMatcherTest {

    @Test
    fun matchesLanguage_acceptsSourceLanguageCodesWithRegionsAndCodecLabels() {
        assertTrue(TrackLanguageMatcher.matchesLanguage("en_US", null, "en"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("ENG_GB", null, "en"))
        assertTrue(TrackLanguageMatcher.matchesLanguage(null, "ENG · AAC 5.1", "en"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("und", "[EN] AC3", "en"))
        assertTrue(TrackLanguageMatcher.matchesLanguage(null, "en_US · SDH", "en"))
        assertTrue(TrackLanguageMatcher.matchesLanguage(null, "POR · AAC", "pt"))
        assertFalse(TrackLanguageMatcher.matchesLanguage(null, "Bengali", "en"))
        assertFalse(TrackLanguageMatcher.matchesLanguage(null, "French · AAC", "en"))
        assertFalse(TrackLanguageMatcher.matchesLanguage(null, "Track 2", "en"))
    }

    @Test
    fun preferredLanguages_preservesPreferenceAndOptionalEnglishFallback() {
        assertEquals(listOf("en"), TrackLanguageMatcher.preferredLanguages("en", true))
        assertEquals(listOf("en-us"), TrackLanguageMatcher.preferredLanguages(" EN_US ", true))
        assertEquals(listOf("pt", "en"), TrackLanguageMatcher.preferredLanguages("pt", true))
        assertEquals(listOf("pt"), TrackLanguageMatcher.preferredLanguages("pt", false))
        assertEquals(emptyList<String>(), TrackLanguageMatcher.preferredLanguages("default", true))
        assertEquals(emptyList<String>(), TrackLanguageMatcher.preferredLanguages("off", true))
        assertEquals(emptyList<String>(), TrackLanguageMatcher.preferredLanguages("", true))
    }

    @Test
    fun matchesLanguage_identifiesPortugueseVariants() {
        assertTrue(TrackLanguageMatcher.matchesLanguage("pt", "Track 1", "pt"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("por", "Track 1", "pt"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("pt-PT", "Track 1", "pt"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("pt-BR", "Track 1", "pt"))
        assertTrue(TrackLanguageMatcher.matchesLanguage(null, "Portuguese", "pt"))
        assertTrue(TrackLanguageMatcher.matchesLanguage(null, "Português", "pt"))
        assertTrue(TrackLanguageMatcher.matchesLanguage(null, "Português (Brasil)", "pt"))
        assertTrue(TrackLanguageMatcher.matchesLanguage(null, "PT-PT", "pt"))
        assertFalse(TrackLanguageMatcher.matchesLanguage("en", "English", "pt"))
    }

    @Test
    fun matchesLanguage_identifiesEnglishVariants() {
        assertTrue(TrackLanguageMatcher.matchesLanguage("en", "Track 1", "en"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("eng", "Track 1", "en"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("en-US", "Track 1", "en"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("en-GB", "Track 1", "en"))
        assertTrue(TrackLanguageMatcher.matchesLanguage(null, "English", "en"))
        assertTrue(TrackLanguageMatcher.matchesLanguage(null, "Inglês", "en"))
        assertFalse(TrackLanguageMatcher.matchesLanguage("es", "Spanish", "en"))
    }

    @Test
    fun matchesLanguage_handlesOtherLanguages() {
        assertTrue(TrackLanguageMatcher.matchesLanguage("es", "Spanish", "es"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("spa", "Español", "es"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("fr", "French", "fr"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("fra", "Français", "fr"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("de", "German", "de"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("deu", "Deutsch", "de"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("it", "Italian", "it"))
        assertTrue(TrackLanguageMatcher.matchesLanguage("ita", "Italiano", "it"))
    }

    @Test
    fun matchesLanguage_rejectsOffAndDefault() {
        assertFalse(TrackLanguageMatcher.matchesLanguage("en", "English", "off"))
        assertFalse(TrackLanguageMatcher.matchesLanguage("en", "English", "default"))
        assertFalse(TrackLanguageMatcher.matchesLanguage("en", "English", ""))
    }

    @Test
    fun detectLanguageCode_detectsKnownLanguages() {
        assertEquals("pt", TrackLanguageMatcher.detectLanguageCode("por", "Português"))
        assertEquals("pt", TrackLanguageMatcher.detectLanguageCode("pt-PT", null))
        assertEquals("pt", TrackLanguageMatcher.detectLanguageCode(null, "Portuguese"))
        assertEquals("en", TrackLanguageMatcher.detectLanguageCode("eng", "English"))
        assertEquals("es", TrackLanguageMatcher.detectLanguageCode("spa", "Español"))
        assertEquals("fr", TrackLanguageMatcher.detectLanguageCode("fra", "Français"))
        assertEquals("de", TrackLanguageMatcher.detectLanguageCode("deu", "Deutsch"))
        assertEquals("it", TrackLanguageMatcher.detectLanguageCode("ita", "Italiano"))
        assertEquals("nl", TrackLanguageMatcher.detectLanguageCode("nl", "Nederlands"))
        assertNull(TrackLanguageMatcher.detectLanguageCode(null, "Unknown Track"))
    }
}

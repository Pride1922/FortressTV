package com.fortresstv.player

import com.fortresstv.player.ui.EnglishStrings
import com.fortresstv.player.ui.PortugueseStrings
import org.junit.Assert.*
import org.junit.Test

class SettingsSearchTest {
    @Test
    fun findsExactControlAndCorrectSectionInBothLanguages() {
        for (strings in listOf(EnglishStrings, PortugueseStrings)) {
            val results = searchSettings(settingsSearchEntries(strings), strings.preferredSubtitleLanguage)
            assertEquals(strings.preferredSubtitleLanguage, results.first().title)
            assertEquals(3, results.first().tab)
            assertFalse(results.first().group)
        }
    }

    @Test
    fun portugueseSearchIgnoresAccentsAndCase() {
        val results = searchSettings(settingsSearchEntries(PortugueseStrings), "SINCRONIZACAO")
        assertTrue(results.any { it.tab == 5 })
    }

    @Test
    fun allTermsMustMatchAndNoMatchIsEmpty() {
        val entries = settingsSearchEntries(EnglishStrings)
        assertTrue(searchSettings(entries, "English subtitles").any { it.title == EnglishStrings.preferredSubtitleLanguage })
        assertTrue(searchSettings(entries, "English unicorn").isEmpty())
        assertTrue(searchSettings(entries, "   ").isEmpty())
    }

    @Test
    fun searchIndexUsesUniqueDestinationsAndValidTabs() {
        for (strings in listOf(EnglishStrings, PortugueseStrings)) {
            val entries = settingsSearchEntries(strings)
            assertEquals(entries.size, entries.distinctBy { it.tab to it.title }.size)
            assertTrue(entries.all { it.tab in 0..6 && it.targetTitle.isNotBlank() })
        }
    }
}

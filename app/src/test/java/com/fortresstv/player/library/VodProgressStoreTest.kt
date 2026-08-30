package com.fortresstv.player.library

import org.junit.Assert.assertEquals
import org.junit.Test

class VodProgressStoreTest {
    @Test
    fun roundTripsSourceScopedProgressWithoutStreamCredentials() {
        val entry = VodProgressEntry(
            key = "episode:source-1:701",
            kind = VodProgressKind.EPISODE,
            sourceId = "source-1",
            itemId = "701",
            seriesId = 20,
            title = "S01E01 · Pilot",
            posterUrl = "https://img.example/pilot.jpg",
            containerExtension = "mkv",
            season = 1,
            episode = 1,
            positionMs = 120_000,
            durationMs = 2_400_000,
            updatedAtMs = 1000
        )

        val encoded = VodProgressStore.encode(listOf(entry))
        val decoded = VodProgressStore.decode(encoded)

        assertEquals(listOf(entry), decoded)
        assertEquals(false, encoded.contains("username"))
        assertEquals(false, encoded.contains("password"))
    }

    @Test
    fun ignoresMalformedEntries() {
        assertEquals(emptyList<VodProgressEntry>(), VodProgressStore.decode("not-json"))
        assertEquals(emptyList<VodProgressEntry>(), VodProgressStore.decode("[{}]"))
    }
}

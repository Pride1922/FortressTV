package com.fortresstv.player.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgGuideIdentityTest {
    private val channels = mapOf(
        "RTP1.pt" to EpgChannel("RTP1.pt", "RTP 1", displayNames = listOf("RTP 1", "RTP1 HD")),
        "SIC.pt" to EpgChannel("SIC.pt", "SIC Notícias")
    )
    private val programme = EpgProgramme("RTP1.pt", "News", null, null, 1_000L, 2_000L)

    @Test
    fun guidesCompareByIdentitySoUpdatesNeverWalkAllProgrammes() {
        val first = EpgGuide(channels, mapOf("RTP1.pt" to listOf(programme)))
        val sameContent = EpgGuide(channels, mapOf("RTP1.pt" to listOf(programme)))

        assertTrue(first == first)
        assertFalse(first == sameContent)
        assertEquals(System.identityHashCode(first), first.hashCode())
    }

    @Test
    fun guidesOnTheSameChannelsShareOneLookupAndResolveTheSame() {
        val today = EpgGuide(channels, mapOf("RTP1.pt" to listOf(programme))).warmUp()
        val tomorrow = EpgGuide(channels, emptyMap())

        assertSame(today.normalizedChannelMap, tomorrow.normalizedChannelMap)
        assertEquals("RTP1.pt", today.resolveChannelId(null, "PT: RTP 1 HD"))
        assertEquals("SIC.pt", tomorrow.resolveChannelId(null, "SIC Noticias"))
    }

    @Test
    fun differentChannelsGetTheirOwnLookup() {
        val first = EpgGuide(channels).warmUp()
        val other = EpgGuide(mapOf("CNN.pt" to EpgChannel("CNN.pt", "CNN Portugal")))

        assertEquals("CNN.pt", other.resolveChannelId(null, "CNN Portugal"))
        assertEquals("RTP1.pt", first.resolveChannelId(null, "RTP 1"))
    }
}

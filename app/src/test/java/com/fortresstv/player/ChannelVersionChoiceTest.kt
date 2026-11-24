package com.fortresstv.player

import com.fortresstv.player.playlist.PlaylistChannel
import org.junit.Assert.*
import org.junit.Test

class ChannelVersionChoiceTest {
    private val hd = PlaylistChannel(0, "Example HD", "https://example.com/hd", tvgId = "example", sourceId = "source", sourceItemId = "tvg:example")
    private val uhd = hd.copy(position = 1, name = "Example 4K", streamUrl = "https://example.com/uhd")

    @Test
    fun qualityVersionsWithSameEpgIdRemainDistinctAcrossRefreshOrdering() {
        assertNotEquals(channelVersionKey(hd), channelVersionKey(uhd))
        assertEquals(channelVersionKey(uhd), channelVersionKey(uhd.copy(position = 100, sourcePosition = 100)))
        assertFalse(channelVersionKey(uhd)!!.contains(uhd.streamUrl))
        assertEquals(uhd, resolveSavedChannelVersion(hd, listOf(uhd, hd), channelVersionKey(uhd)))
    }

    @Test
    fun unavailableOrUnrelatedSavedVersionFallsBackToRequestedChannel() {
        val other = uhd.copy(name = "Different Channel", tvgId = "different", sourceItemId = "different")
        assertEquals(hd, resolveSavedChannelVersion(hd, listOf(hd), channelVersionKey(uhd)))
        assertEquals(hd, resolveSavedChannelVersion(hd, listOf(hd, other), channelVersionKey(other)))
        assertEquals(hd, resolveSavedChannelVersion(hd, listOf(hd, uhd), null))
    }
}

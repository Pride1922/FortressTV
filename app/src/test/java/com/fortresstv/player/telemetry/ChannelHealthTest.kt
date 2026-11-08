package com.fortresstv.player.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelHealthTest {

    @Test
    fun `measuring state when live testing and startupMs is zero`() {
        val snapshot = ChannelHealthSnapshot.fromMetrics(
            channelId = "channel-1",
            startupMs = 0L,
            stallCount = 0,
            totalStallMs = 0L,
            errorCode = null,
            region = "PT",
            isLiveTesting = true
        )
        assertEquals(ConnectionHealthState.MEASURING, snapshot.state)
        assertEquals("channel-1", snapshot.channelId)
    }

    @Test
    fun `excellent state when startup is fast and zero stalls`() {
        val snapshot = ChannelHealthSnapshot.fromMetrics(
            channelId = "channel-1",
            startupMs = 850L,
            stallCount = 0,
            totalStallMs = 0L,
            errorCode = null,
            region = "PT"
        )
        assertEquals(ConnectionHealthState.EXCELLENT, snapshot.state)
        assertEquals(850L, snapshot.startupMs)
        assertEquals(0, snapshot.stallCount)
    }

    @Test
    fun `moderate state when single stall or elevated latency occurs`() {
        val snapshot = ChannelHealthSnapshot.fromMetrics(
            channelId = "channel-1",
            startupMs = 2500L,
            stallCount = 1,
            totalStallMs = 800L,
            errorCode = null,
            region = "PT"
        )
        assertEquals(ConnectionHealthState.MODERATE, snapshot.state)
    }

    @Test
    fun `poor state when frequent stalls or high latency occurs`() {
        val snapshot = ChannelHealthSnapshot.fromMetrics(
            channelId = "channel-1",
            startupMs = 5000L,
            stallCount = 4,
            totalStallMs = 8000L,
            errorCode = null,
            region = "PT"
        )
        assertEquals(ConnectionHealthState.POOR, snapshot.state)
    }

    @Test
    fun `poor state immediately upon player error`() {
        val snapshot = ChannelHealthSnapshot.fromMetrics(
            channelId = "channel-1",
            startupMs = 500L,
            stallCount = 0,
            totalStallMs = 0L,
            errorCode = "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED",
            region = "PT"
        )
        assertEquals(ConnectionHealthState.POOR, snapshot.state)
    }

    @Test
    fun `fromSummary converts PlaybackHealthSummary correctly`() {
        val summary = PlaybackHealthSummary(
            sessionId = "test-uuid",
            householdId = "hh-1",
            profileId = "prof-1",
            channelId = "rtp1",
            streamKind = StreamKindWire.LIVE,
            providerId = "provider-1",
            startedAtEpochMs = 1000L,
            endedAtEpochMs = 31000L,
            startupMs = 740L,
            stallCount = 0,
            totalStallMs = 0L,
            avgBitrateKbps = 5200,
            droppedFramesPct = 0f,
            playerErrorCode = null,
            region = "PT",
            completed = true
        )
        val snapshot = ChannelHealthSnapshot.fromSummary(summary)
        assertEquals("rtp1", snapshot.channelId)
        assertEquals(ConnectionHealthState.EXCELLENT, snapshot.state)
        assertEquals(740L, snapshot.startupMs)
    }
}

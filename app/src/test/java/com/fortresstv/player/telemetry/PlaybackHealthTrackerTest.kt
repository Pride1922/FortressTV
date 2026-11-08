package com.fortresstv.player.telemetry

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackHealthTrackerTest {

    private fun dummyEventTime(realtimeMs: Long = 0L): AnalyticsListener.EventTime {
        return AnalyticsListener.EventTime(
            realtimeMs,
            Timeline.EMPTY,
            0,
            null,
            0L,
            Timeline.EMPTY,
            0,
            null,
            0L,
            0L
        )
    }

    @Test
    fun tracksStartupLatencyOnFirstFrame() {
        var currentTime = 1000L
        var emittedSummary: PlaybackHealthSummary? = null

        val tracker = PlaybackHealthTracker(
            householdId = "hh-1",
            profileId = "prof-1",
            channelId = "ch-1",
            streamKind = StreamKindWire.LIVE,
            providerId = "prov-1",
            region = "PT",
            clock = { currentTime },
            onFinalized = { emittedSummary = it }
        )

        // Simulate 450ms until first frame renders
        currentTime = 1450L
        tracker.onRenderedFirstFrame(dummyEventTime(currentTime), Any(), 450L)

        // Multiple frame calls should not overwrite startupMs
        currentTime = 2000L
        tracker.onRenderedFirstFrame(dummyEventTime(currentTime), Any(), 1000L)

        tracker.finalizeSession()

        assertNotNull(emittedSummary)
        assertEquals(450L, emittedSummary!!.startupMs)
    }

    @Test
    fun tracksBufferingStallsOnlyWhenExceedingThreshold() {
        var currentTime = 1000L
        var emittedSummary: PlaybackHealthSummary? = null

        val tracker = PlaybackHealthTracker(
            householdId = "hh-1",
            profileId = "prof-1",
            channelId = "ch-1",
            streamKind = StreamKindWire.LIVE,
            providerId = "prov-1",
            clock = { currentTime },
            onFinalized = { emittedSummary = it }
        )

        val eventTime = dummyEventTime()

        // 1. Brief buffering < 500ms (e.g. 200ms) - should NOT count as a stall
        tracker.onPlaybackStateChanged(eventTime, Player.STATE_BUFFERING)
        currentTime += 200L
        tracker.onPlaybackStateChanged(eventTime, Player.STATE_READY)

        // 2. Significant stall of 800ms - SHOULD count as 1 stall
        tracker.onPlaybackStateChanged(eventTime, Player.STATE_BUFFERING)
        currentTime += 800L
        tracker.onPlaybackStateChanged(eventTime, Player.STATE_READY)

        // 3. Second significant stall of 1200ms
        tracker.onPlaybackStateChanged(eventTime, Player.STATE_BUFFERING)
        currentTime += 1200L
        tracker.onPlaybackStateChanged(eventTime, Player.STATE_READY)

        tracker.finalizeSession()

        assertNotNull(emittedSummary)
        assertEquals(2, emittedSummary!!.stallCount)
        assertEquals(2000L, emittedSummary!!.totalStallMs)
    }

    @Test
    fun tracksPlayerErrorAndMarksCompletedFalse() {
        var currentTime = 5000L
        var emittedSummary: PlaybackHealthSummary? = null

        val tracker = PlaybackHealthTracker(
            householdId = "hh-1",
            profileId = "prof-1",
            channelId = "vod-1",
            streamKind = StreamKindWire.VOD,
            providerId = "prov-1",
            clock = { currentTime },
            onFinalized = { emittedSummary = it }
        )

        tracker.recordError("ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT")
        tracker.finalizeSession()

        assertNotNull(emittedSummary)
        assertFalse(emittedSummary!!.completed)
        assertEquals("ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT", emittedSummary!!.playerErrorCode)
        assertEquals(1, emittedSummary!!.stallCount)
    }

    @Test
    fun finalizeSessionIsIdempotent() {
        var finalizeCallCount = 0
        var emittedSummary: PlaybackHealthSummary? = null

        val tracker = PlaybackHealthTracker(
            householdId = "hh-1",
            profileId = "prof-1",
            channelId = "ch-1",
            streamKind = StreamKindWire.LIVE,
            providerId = "prov-1",
            onFinalized = {
                finalizeCallCount++
                emittedSummary = it
            }
        )

        tracker.finalizeSession()
        tracker.finalizeSession()
        tracker.finalizeSession()

        assertEquals(1, finalizeCallCount)
        assertNotNull(emittedSummary)
    }

    @Test
    fun tracksDroppedFramesAndCalculatesPercentage() {
        var emittedSummary: PlaybackHealthSummary? = null

        val tracker = PlaybackHealthTracker(
            householdId = "hh-1",
            profileId = "prof-1",
            channelId = "ch-1",
            streamKind = StreamKindWire.LIVE,
            providerId = "prov-1",
            onFinalized = { emittedSummary = it }
        )

        val counters = DecoderCounters().apply {
            renderedOutputBufferCount = 950
            droppedBufferCount = 50
        }
        tracker.onVideoDisabled(dummyEventTime(), counters)
        tracker.finalizeSession()

        assertNotNull(emittedSummary)
        // 50 / (950 + 50) = 5.0%
        assertEquals(5.0f, emittedSummary!!.droppedFramesPct, 0.01f)
    }
}

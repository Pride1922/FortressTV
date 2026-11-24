package com.fortresstv.player.telemetry

import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.analytics.AnalyticsListener
import org.junit.Assert.*
import org.junit.Test

class MeasuredVideoQualityTest {
    private val event = AnalyticsListener.EventTime(0, Timeline.EMPTY, 0, null, 0, Timeline.EMPTY, 0, null, 0, 0)
    private val variant = playbackStreamVariantId("source", "item", "https://example.com/stream")!!

    @Test
    fun identitySeparatesSourcesAndVersionsWithoutExposingConfiguration() {
        assertEquals(variant, playbackStreamVariantId("source", "item", "https://example.com/stream"))
        assertNotEquals(variant, playbackStreamVariantId("other", "item", "https://example.com/stream"))
        assertNotEquals(variant, playbackStreamVariantId("source", "other", "https://example.com/stream"))
        assertNotEquals(variant, playbackStreamVariantId("source", "item", "https://example.com/alternate"))
        assertTrue(variant.matches(Regex("[0-9a-f]{64}")))
        assertNull(playbackStreamVariantId("", "item", "https://example.com/stream"))
    }

    @Test
    fun keepsLastRenderedResolutionRatherThanLargestAdvertisedSize() {
        var now = 1_000L
        var summary: PlaybackHealthSummary? = null
        val tracker = PlaybackHealthTracker(householdId = "household", profileId = "profile", channelId = "channel",
            streamKind = StreamKindWire.LIVE, providerId = "source", region = "PT", clock = { now },
            streamVariantId = variant, onFinalized = { summary = it })
        tracker.onVideoSizeChanged(event, VideoSize(3840, 2160))
        tracker.onRenderedFirstFrame(event, Any(), now)
        now = 2_000
        tracker.onVideoSizeChanged(event, VideoSize(1920, 1080))
        tracker.onVideoSizeChanged(event, VideoSize.UNKNOWN)
        now = 3_000
        tracker.finalizeSession()
        assertEquals(MeasuredVideoQuality(1920, 1080, 2_000), summary!!.videoQuality)
        assertEquals("FHD", summary!!.videoQuality!!.tier)
        tracker.onVideoSizeChanged(event, VideoSize(3840, 2160))
        assertEquals("FHD", summary!!.videoQuality!!.tier)
    }

    @Test
    fun sessionsWithoutRenderedVideoKeepUnknownQuality() {
        var summary: PlaybackHealthSummary? = null
        val tracker = PlaybackHealthTracker(householdId = "household", profileId = "profile", channelId = "channel",
            streamKind = StreamKindWire.LIVE, providerId = "source", region = "PT", streamVariantId = variant,
            onFinalized = { summary = it })
        tracker.onVideoSizeChanged(event, VideoSize(3840, 2160))
        tracker.finalizeSession()
        assertNull(summary!!.videoQuality)
    }

    @Test
    fun newTrackerDoesNotReusePreviousChannelResolution() {
        var summary: PlaybackHealthSummary? = null
        val tracker = PlaybackHealthTracker(householdId = "household", profileId = "profile", channelId = "next",
            streamKind = StreamKindWire.LIVE, providerId = "source", region = "PT", streamVariantId = variant,
            onFinalized = { summary = it })
        tracker.onRenderedFirstFrame(event, Any(), 0)
        tracker.finalizeSession()
        assertNull(summary!!.videoQuality)
    }

    @Test
    fun queuedQualityRoundTripsAndWirePayloadLeavesTierToDatabase() {
        val quality = MeasuredVideoQuality(3840, 2160, 1_700_000_000_000)
        val original = PlaybackHealthSummary.createForTest().copy(streamVariantId = variant, videoQuality = quality)
        assertEquals(original, PlaybackHealthSummary.fromJson(original.toJson()))
        val wire = original.toPostgrestPayload()
        assertEquals(3840, wire.getInt("video_width"))
        assertEquals(2160, wire.getInt("video_height"))
        assertEquals("2023-11-14T22:13:20Z", wire.getString("quality_observed_at"))
        assertEquals(variant, wire.getString("stream_variant_id"))
        assertFalse(wire.has("quality_tier"))
        assertFalse(wire.has("quality_observed_at_ms"))
    }

    @Test
    fun oldQueuedSummariesAndInvalidOptionalDataRemainReadable() {
        val old = PlaybackHealthSummary.createForTest()
        assertEquals(old, PlaybackHealthSummary.fromJson(old.toJson()))
        val json = old.toJson().put("stream_variant_id", variant).put("video_width", 3840)
        val parsed = PlaybackHealthSummary.fromJson(json)
        assertNull(parsed.videoQuality)
        assertEquals(old.sessionId, parsed.sessionId)
        assertFalse(parsed.toPostgrestPayload().has("video_width"))
    }
}

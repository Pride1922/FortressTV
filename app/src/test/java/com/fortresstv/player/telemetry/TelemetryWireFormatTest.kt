package com.fortresstv.player.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class TelemetryWireFormatTest {

    @Test
    fun streamKindWireFormatMatchesSqlCheckConstraint() {
        assertEquals("live", StreamKindWire.LIVE.wire)
        assertEquals("catchup", StreamKindWire.CATCHUP.wire)
        assertEquals("vod", StreamKindWire.VOD.wire)

        assertEquals(StreamKindWire.LIVE, StreamKindWire.fromWire("live"))
        assertEquals(StreamKindWire.CATCHUP, StreamKindWire.fromWire("catchup"))
        assertEquals(StreamKindWire.VOD, StreamKindWire.fromWire("vod"))
        assertEquals(StreamKindWire.LIVE, StreamKindWire.fromWire("unknown_fallback"))
    }

    @Test
    fun toPostgrestPayloadMatchesExpectedSchema() {
        val summary = PlaybackHealthSummary.createForTest(
            sessionId = UUID.randomUUID().toString(),
            householdId = "00000000-0000-0000-0000-000000000001",
            profileId = "00000000-0000-0000-0000-000000000002",
            channelId = "rtp-1",
            streamKind = StreamKindWire.LIVE,
            providerId = "provider-abc",
            startedAtEpochMs = 1700000000000L,
            endedAtEpochMs = 1700000060000L,
            startupMs = 350L,
            stallCount = 2,
            totalStallMs = 1400L,
            avgBitrateKbps = 3200,
            droppedFramesPct = 1.25f,
            playerErrorCode = null,
            region = "PT",
            completed = true
        )

        val json = summary.toPostgrestPayload()

        assertEquals(summary.sessionId, json.getString("session_id"))
        assertEquals(summary.householdId, json.getString("household_id"))
        assertEquals(summary.profileId, json.getString("profile_id"))
        assertEquals("rtp-1", json.getString("channel_id"))
        assertEquals("live", json.getString("stream_kind"))
        assertEquals("provider-abc", json.getString("provider_id"))
        assertEquals(350L, json.getLong("startup_ms"))
        assertEquals(2, json.getInt("stall_count"))
        assertEquals(1400L, json.getLong("total_stall_ms"))
        assertEquals(3200, json.getInt("avg_bitrate_kbps"))
        assertEquals(1.25, json.getDouble("dropped_frames_pct"), 0.001)
        assertTrue(json.isNull("player_error_code"))
        assertEquals("PT", json.getString("region"))
        assertTrue(json.getBoolean("completed"))

        // Check ISO-8601 timestamps
        val startedAtParsed = Instant.parse(json.getString("started_at"))
        val endedAtParsed = Instant.parse(json.getString("ended_at"))
        assertEquals(1700000000000L, startedAtParsed.toEpochMilli())
        assertEquals(1700000060000L, endedAtParsed.toEpochMilli())
    }

    @Test
    fun roundTripSerializationPreservesAllFields() {
        val original = PlaybackHealthSummary.createForTest(
            sessionId = "session-123",
            householdId = "household-abc",
            profileId = "profile-xyz",
            channelId = "channel-456",
            streamKind = StreamKindWire.CATCHUP,
            providerId = "source-789",
            startedAtEpochMs = 10000L,
            endedAtEpochMs = 70000L,
            startupMs = 600L,
            stallCount = 1,
            totalStallMs = 850L,
            avgBitrateKbps = 5400,
            droppedFramesPct = 0.8f,
            playerErrorCode = "ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT",
            region = "PT",
            completed = false
        )

        val json = original.toJson()
        val parsed = PlaybackHealthSummary.fromJson(json)

        assertEquals(original.sessionId, parsed.sessionId)
        assertEquals(original.householdId, parsed.householdId)
        assertEquals(original.profileId, parsed.profileId)
        assertEquals(original.channelId, parsed.channelId)
        assertEquals(original.streamKind, parsed.streamKind)
        assertEquals(original.providerId, parsed.providerId)
        assertEquals(original.startedAtEpochMs, parsed.startedAtEpochMs)
        assertEquals(original.endedAtEpochMs, parsed.endedAtEpochMs)
        assertEquals(original.startupMs, parsed.startupMs)
        assertEquals(original.stallCount, parsed.stallCount)
        assertEquals(original.totalStallMs, parsed.totalStallMs)
        assertEquals(original.avgBitrateKbps, parsed.avgBitrateKbps)
        assertEquals(original.droppedFramesPct, parsed.droppedFramesPct, 0.001f)
        assertEquals(original.playerErrorCode, parsed.playerErrorCode)
        assertEquals(original.region, parsed.region)
        assertEquals(original.completed, parsed.completed)
    }

    @Test
    fun sessionIdsAreUniqueAcrossMultipleSummaries() {
        val s1 = PlaybackHealthSummary.createForTest()
        val s2 = PlaybackHealthSummary.createForTest()
        assertNotNull(s1.sessionId)
        assertNotNull(s2.sessionId)
        assertNotEquals(s1.sessionId, s2.sessionId)
    }
}

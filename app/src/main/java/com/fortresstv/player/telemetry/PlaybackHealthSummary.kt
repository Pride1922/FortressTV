package com.fortresstv.player.telemetry

import androidx.annotation.VisibleForTesting
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/**
 * Categorizes the type of media stream played.
 * Wire format matches PostgreSQL check constraint ('live', 'catchup', 'vod').
 */
enum class StreamKindWire(val wire: String) {
    LIVE("live"),
    CATCHUP("catchup"),
    VOD("vod");

    companion object {
        fun fromWire(wire: String): StreamKindWire = when (wire.lowercase()) {
            "live" -> LIVE
            "catchup" -> CATCHUP
            "vod" -> VOD
            else -> LIVE
        }
    }
}

/**
 * Compact, anonymized quality-of-experience summary emitted per playback session.
 *
 * This data is used for:
 * 1. Channel connection health badge: Aggregated across sessions to compute stability scores
 *    (stall count, time-to-first-frame, dropped frames) per channel and region.
 * 2. Adaptive buffer tuning: Startup latency and average bitrate inform initial buffer sizes
 *    and rebuffering thresholds for upcoming sessions.
 *
 * Strict Privacy Rules:
 * - No PII (no user names, no stream URLs, no full IP address, no device hardware identifiers).
 * - Provider ID is a stable configuration hash, not the raw provider credentials or host.
 * - Region is coarse (e.g. ISO-3166 2-letter country code from system locale or "unknown").
 *
 * @property sessionId Unique identifier generated per playback session (UUID).
 * @property householdId Anonymized household/account identifier.
 * @property profileId Identifier of the active profile within the household.
 * @property channelId Stable identifier of the channel (empty string for VOD).
 * @property streamKind Type of stream (LIVE, CATCHUP, or VOD).
 * @property providerId Stable hash or identifier of the playlist/Xtream source.
 * @property startedAtEpochMs Wall-clock epoch timestamp (ms) when playback was requested.
 * @property endedAtEpochMs Wall-clock epoch timestamp (ms) when session concluded.
 * @property startupMs Time-to-first-frame in milliseconds from playback request to first rendered frame.
 * @property stallCount Number of buffering rebuffers that exceeded 500ms during active playback.
 * @property totalStallMs Cumulative duration (ms) spent in buffering stalls.
 * @property avgBitrateKbps Estimated average transfer bitrate in kilobits per second.
 * @property droppedFramesPct Percentage of video frames dropped (0.0 to 100.0).
 * @property playerErrorCode Media3 player error code name if session suffered a fatal failure, or null.
 * @property region Coarse geographical region (2-letter country code or "unknown").
 * @property completed True if playback finished or user exited cleanly; false on unhandled error/crash.
 */
data class PlaybackHealthSummary(
    val sessionId: String,
    val householdId: String,
    val profileId: String,
    val channelId: String,
    val streamKind: StreamKindWire,
    val providerId: String?,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val startupMs: Long,
    val stallCount: Int,
    val totalStallMs: Long,
    val avgBitrateKbps: Int,
    val droppedFramesPct: Float,
    val playerErrorCode: String?,
    val region: String,
    val completed: Boolean
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("session_id", sessionId)
        put("household_id", householdId)
        put("profile_id", profileId)
        put("channel_id", channelId)
        put("stream_kind", streamKind.wire)
        if (providerId != null) put("provider_id", providerId)
        put("started_at_ms", startedAtEpochMs)
        put("ended_at_ms", endedAtEpochMs)
        put("startup_ms", startupMs)
        put("stall_count", stallCount)
        put("total_stall_ms", totalStallMs)
        put("avg_bitrate_kbps", avgBitrateKbps)
        put("dropped_frames_pct", droppedFramesPct.toDouble())
        if (playerErrorCode != null) put("player_error_code", playerErrorCode)
        put("region", region)
        put("completed", completed)
    }

    /**
     * Converts to the wire format expected by the Supabase REST endpoint.
     */
    fun toPostgrestPayload(): JSONObject = JSONObject().apply {
        put("session_id", sessionId)
        put("household_id", toValidUuid(householdId))
        put("profile_id", toValidUuid(profileId))
        put("channel_id", channelId)
        put("stream_kind", streamKind.wire)
        if (providerId != null) put("provider_id", providerId) else put("provider_id", JSONObject.NULL)
        put("started_at", Instant.ofEpochMilli(startedAtEpochMs).toString())
        put("ended_at", Instant.ofEpochMilli(endedAtEpochMs).toString())
        put("startup_ms", startupMs)
        put("stall_count", stallCount)
        put("total_stall_ms", totalStallMs)
        put("avg_bitrate_kbps", avgBitrateKbps)
        put("dropped_frames_pct", droppedFramesPct.toDouble())
        if (playerErrorCode != null) put("player_error_code", playerErrorCode) else put("player_error_code", JSONObject.NULL)
        put("region", region)
        put("completed", completed)
    }

    private fun toValidUuid(raw: String): String = runCatching {
        UUID.fromString(raw).toString()
    }.getOrElse {
        UUID.nameUUIDFromBytes(raw.toByteArray(Charsets.UTF_8)).toString()
    }

    companion object {
        fun fromJson(json: JSONObject): PlaybackHealthSummary = PlaybackHealthSummary(
            sessionId = json.getString("session_id"),
            householdId = json.getString("household_id"),
            profileId = json.getString("profile_id"),
            channelId = json.optString("channel_id", ""),
            streamKind = StreamKindWire.fromWire(json.getString("stream_kind")),
            providerId = json.optString("provider_id").takeIf { it.isNotBlank() && it != "null" },
            startedAtEpochMs = json.optLong("started_at_ms", 0L),
            endedAtEpochMs = json.optLong("ended_at_ms", 0L),
            startupMs = json.optLong("startup_ms", 0L),
            stallCount = json.optInt("stall_count", 0),
            totalStallMs = json.optLong("total_stall_ms", 0L),
            avgBitrateKbps = json.optInt("avg_bitrate_kbps", 0),
            droppedFramesPct = json.optDouble("dropped_frames_pct", 0.0).toFloat(),
            playerErrorCode = json.optString("player_error_code").takeIf { it.isNotBlank() && it != "null" },
            region = json.optString("region", "unknown"),
            completed = json.optBoolean("completed", true)
        )

        @VisibleForTesting
        fun createForTest(
            sessionId: String = UUID.randomUUID().toString(),
            householdId: String = "test-household-id",
            profileId: String = "test-profile-id",
            channelId: String = "channel-1",
            streamKind: StreamKindWire = StreamKindWire.LIVE,
            providerId: String? = "provider-hash-1",
            startedAtEpochMs: Long = 1000L,
            endedAtEpochMs: Long = 61000L,
            startupMs: Long = 450L,
            stallCount: Int = 1,
            totalStallMs: Long = 1200L,
            avgBitrateKbps: Int = 4500,
            droppedFramesPct: Float = 0.5f,
            playerErrorCode: String? = null,
            region: String = "PT",
            completed: Boolean = true
        ): PlaybackHealthSummary = PlaybackHealthSummary(
            sessionId = sessionId,
            householdId = householdId,
            profileId = profileId,
            channelId = channelId,
            streamKind = streamKind,
            providerId = providerId,
            startedAtEpochMs = startedAtEpochMs,
            endedAtEpochMs = endedAtEpochMs,
            startupMs = startupMs,
            stallCount = stallCount,
            totalStallMs = totalStallMs,
            avgBitrateKbps = avgBitrateKbps,
            droppedFramesPct = droppedFramesPct,
            playerErrorCode = playerErrorCode,
            region = region,
            completed = completed
        )
    }
}

package com.fortresstv.player.telemetry

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Buffer tuning configuration tied directly to channel health stability.
 */
data class HealthBufferPolicy(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int,
    val label: String
)

/**
 * Categorizes the observed connection health of a stream.
 */
enum class ConnectionHealthState(val color: Color) {
    EXCELLENT(Color(0xFF4CAF50)), // Green
    MODERATE(Color(0xFFFFC107)),  // Amber
    POOR(Color(0xFFFF5252)),      // Red
    MEASURING(Color(0xFF90CAF9)), // Light Blue / Pulsing
    UNKNOWN(Color(0xFF888888));   // Gray

    val bufferPolicy: HealthBufferPolicy
        get() = when (this) {
            EXCELLENT -> HealthBufferPolicy(
                minBufferMs = 8_000,
                maxBufferMs = 25_000,
                bufferForPlaybackMs = 600,
                bufferForPlaybackAfterRebufferMs = 1_500,
                label = "Fast-Start (600ms)"
            )
            MODERATE -> HealthBufferPolicy(
                minBufferMs = 15_000,
                maxBufferMs = 40_000,
                bufferForPlaybackMs = 1_800,
                bufferForPlaybackAfterRebufferMs = 3_000,
                label = "Balanced (1.8s)"
            )
            POOR -> HealthBufferPolicy(
                minBufferMs = 25_000,
                maxBufferMs = 60_000,
                bufferForPlaybackMs = 4_000,
                bufferForPlaybackAfterRebufferMs = 6_000,
                label = "Anti-Freeze (4.0s)"
            )
            MEASURING, UNKNOWN -> HealthBufferPolicy(
                minBufferMs = 12_000,
                maxBufferMs = 35_000,
                bufferForPlaybackMs = 1_500,
                bufferForPlaybackAfterRebufferMs = 2_500,
                label = "Default (1.5s)"
            )
        }
}

/**
 * Snapshot of observed quality-of-experience metrics for a specific channel.
 */
data class ChannelHealthSnapshot(
    val channelId: String,
    val state: ConnectionHealthState,
    val startupMs: Long,
    val stallCount: Int,
    val totalStallMs: Long,
    val region: String,
    val lastTestedEpochMs: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("channel_id", channelId)
        put("state", state.name)
        put("startup_ms", startupMs)
        put("stall_count", stallCount)
        put("total_stall_ms", totalStallMs)
        put("region", region)
        put("last_tested_ms", lastTestedEpochMs)
    }

    companion object {
        fun fromJson(json: JSONObject): ChannelHealthSnapshot = ChannelHealthSnapshot(
            channelId = json.getString("channel_id"),
            state = runCatching { ConnectionHealthState.valueOf(json.getString("state")) }.getOrDefault(ConnectionHealthState.UNKNOWN),
            startupMs = json.optLong("startup_ms", 0L),
            stallCount = json.optInt("stall_count", 0),
            totalStallMs = json.optLong("total_stall_ms", 0L),
            region = json.optString("region", "unknown"),
            lastTestedEpochMs = json.optLong("last_tested_ms", 0L)
        )

        fun fromMetrics(
            channelId: String,
            startupMs: Long,
            stallCount: Int,
            totalStallMs: Long,
            errorCode: String?,
            region: String,
            isLiveTesting: Boolean = false,
            clockMs: Long = System.currentTimeMillis()
        ): ChannelHealthSnapshot {
            val state = when {
                isLiveTesting && startupMs == 0L -> ConnectionHealthState.MEASURING
                errorCode != null || stallCount >= 3 || totalStallMs >= 5000L || startupMs > 4500L -> ConnectionHealthState.POOR
                stallCount in 1..2 || startupMs in 2001L..4500L -> ConnectionHealthState.MODERATE
                startupMs in 1L..2000L && stallCount == 0 -> ConnectionHealthState.EXCELLENT
                isLiveTesting -> ConnectionHealthState.MEASURING
                else -> ConnectionHealthState.UNKNOWN
            }
            return ChannelHealthSnapshot(
                channelId = channelId,
                state = state,
                startupMs = startupMs,
                stallCount = stallCount,
                totalStallMs = totalStallMs,
                region = region,
                lastTestedEpochMs = clockMs
            )
        }

        fun fromSummary(summary: PlaybackHealthSummary): ChannelHealthSnapshot = fromMetrics(
            channelId = summary.channelId,
            startupMs = summary.startupMs,
            stallCount = summary.stallCount,
            totalStallMs = summary.totalStallMs,
            errorCode = summary.playerErrorCode,
            region = summary.region,
            isLiveTesting = false,
            clockMs = summary.endedAtEpochMs
        )
    }
}

/**
 * In-memory and persistent cache of recent channel health scores.
 */
class ChannelHealthRepository internal constructor(
    private val prefsProvider: () -> SharedPreferences
) {
    companion object {
        private const val PREFS_NAME = "fortress_channel_health"
        private const val KEY_SNAPSHOTS = "channel_snapshots"

        @Volatile
        private var instance: ChannelHealthRepository? = null

        fun get(context: Context): ChannelHealthRepository = instance ?: synchronized(this) {
            instance ?: ChannelHealthRepository(
                prefsProvider = {
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            ).also { instance = it }
        }
    }

    private val preferences by lazy { prefsProvider() }
    private val _localSnapshots = MutableStateFlow<Map<String, ChannelHealthSnapshot>>(emptyMap())
    private val _aggregateSnapshots = MutableStateFlow<Map<String, ChannelHealthSnapshot>>(emptyMap())
    private val _effectiveSnapshots = MutableStateFlow<Map<String, ChannelHealthSnapshot>>(emptyMap())

    /**
     * Effective stream health snapshots, prioritizing local observations on this device
     * with aggregate cloud community scores as a fallback for unwatched channels.
     */
    val snapshots: StateFlow<Map<String, ChannelHealthSnapshot>> = _effectiveSnapshots.asStateFlow()

    init {
        loadPersisted()
    }

    fun getHealth(channelId: String): ChannelHealthSnapshot? = _effectiveSnapshots.value[channelId]

    /**
     * Updates health in real time while a session is actively playing.
     */
    fun recordActiveMetrics(
        channelId: String,
        startupMs: Long,
        stallCount: Int,
        totalStallMs: Long,
        errorCode: String?,
        region: String,
        isLiveTesting: Boolean = true
    ) {
        if (channelId.isBlank()) return
        val snapshot = ChannelHealthSnapshot.fromMetrics(
            channelId = channelId,
            startupMs = startupMs,
            stallCount = stallCount,
            totalStallMs = totalStallMs,
            errorCode = errorCode,
            region = region,
            isLiveTesting = isLiveTesting
        )
        synchronized(this) {
            val updated = _localSnapshots.value.toMutableMap()
            updated[channelId] = snapshot
            _localSnapshots.value = updated
            refreshEffective()
            persistAsync(updated)
        }
    }

    /**
     * Updates health when a final [PlaybackHealthSummary] is completed.
     */
    fun recordSummary(summary: PlaybackHealthSummary) {
        if (summary.channelId.isBlank()) return
        val snapshot = ChannelHealthSnapshot.fromSummary(summary)
        synchronized(this) {
            val updated = _localSnapshots.value.toMutableMap()
            updated[summary.channelId] = snapshot
            _localSnapshots.value = updated
            refreshEffective()
            persistAsync(updated)
        }
    }

    /**
     * Sets crowdsourced community aggregates fetched from Supabase.
     */
    fun setAggregateSnapshots(aggregates: Map<String, ChannelHealthSnapshot>) {
        synchronized(this) {
            _aggregateSnapshots.value = aggregates
            refreshEffective()
        }
    }

    private fun refreshEffective() {
        // Local observations on this device always take precedence over global aggregates
        _effectiveSnapshots.value = _aggregateSnapshots.value + _localSnapshots.value
    }

    private fun loadPersisted() {
        val raw = preferences.getString(KEY_SNAPSHOTS, null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            val map = mutableMapOf<String, ChannelHealthSnapshot>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val snapshot = ChannelHealthSnapshot.fromJson(obj)
                map[snapshot.channelId] = snapshot
            }
            _localSnapshots.value = map
            refreshEffective()
        }
    }

    private fun persistAsync(map: Map<String, ChannelHealthSnapshot>) {
        runCatching {
            val arr = JSONArray()
            // Keep top 200 most recent channels
            map.values.sortedByDescending { it.lastTestedEpochMs }.take(200).forEach {
                arr.put(it.toJson())
            }
            preferences.edit().putString(KEY_SNAPSHOTS, arr.toString()).apply()
        }
    }
}

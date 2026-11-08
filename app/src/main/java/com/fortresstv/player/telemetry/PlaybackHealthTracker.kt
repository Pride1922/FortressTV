package com.fortresstv.player.telemetry

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tracks playback health metrics for an active session by implementing Media3's [AnalyticsListener].
 *
 * Captures startup latency (time-to-first-frame), rebuffering stalls (>500ms),
 * dropped frame ratios, network throughput, and fatal playback errors.
 *
 * Finalization is idempotent via [AtomicBoolean] and safe to call from multiple lifecycle points
 * (player release, UI dismiss, activity onStop).
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackHealthTracker(
    val sessionId: String = UUID.randomUUID().toString(),
    val householdId: String,
    val profileId: String,
    val channelId: String,
    val streamKind: StreamKindWire,
    val providerId: String?,
    val region: String = resolveRegion(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val onActiveMetricsUpdated: ((startupMs: Long, stallCount: Int, totalStallMs: Long, errorCode: String?, region: String) -> Unit)? = null,
    private val onFinalized: (PlaybackHealthSummary) -> Unit
) : AnalyticsListener {

    private val startedAtEpochMs: Long = clock()
    private var startupMs: Long = 0L
    private var firstFrameRendered: Boolean = false

    private var stallCount: Int = 0
    private var totalStallMs: Long = 0L
    private var bufferingStartEpochMs: Long? = null

    private var totalBytesLoaded: Long = 0L
    private var totalLoadDurationMs: Long = 0L

    private var droppedFramesCount: Int = 0
    private var renderedFramesCount: Int = 0

    private var playerErrorCode: String? = null
    private var completedCleanly: Boolean = true

    private val isFinalized = AtomicBoolean(false)

    companion object {
        const val MIN_STALL_THRESHOLD_MS = 500L

        fun resolveRegion(): String {
            val country = Locale.getDefault().country.takeIf { it.isNotBlank() }
                ?: runCatching { android.content.res.Resources.getSystem().configuration.locales.get(0)?.country?.takeIf { it.isNotBlank() } }.getOrNull()
            return country?.uppercase() ?: "unknown"
        }
    }

    override fun onRenderedFirstFrame(
        eventTime: AnalyticsListener.EventTime,
        output: Any,
        renderTimeMs: Long
    ) {
        if (!firstFrameRendered) {
            startupMs = (clock() - startedAtEpochMs).coerceAtLeast(0L)
            firstFrameRendered = true
            onActiveMetricsUpdated?.invoke(startupMs, stallCount, totalStallMs, playerErrorCode, region)
        }
    }

    override fun onPlaybackStateChanged(
        eventTime: AnalyticsListener.EventTime,
        state: Int
    ) {
        val now = clock()
        if (state == Player.STATE_BUFFERING) {
            // Only count mid-stream rebuffering as a stall after the first frame has rendered
            if (firstFrameRendered && bufferingStartEpochMs == null) {
                bufferingStartEpochMs = now
            }
        } else {
            bufferingStartEpochMs?.let { start ->
                val duration = now - start
                if (duration >= MIN_STALL_THRESHOLD_MS) {
                    stallCount++
                    totalStallMs += duration
                    onActiveMetricsUpdated?.invoke(startupMs, stallCount, totalStallMs, playerErrorCode, region)
                }
                bufferingStartEpochMs = null
            }
        }
    }

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long
    ) {
        droppedFramesCount += droppedFrames
    }

    override fun onVideoDisabled(
        eventTime: AnalyticsListener.EventTime,
        decoderCounters: DecoderCounters
    ) {
        renderedFramesCount += decoderCounters.renderedOutputBufferCount
        droppedFramesCount += decoderCounters.droppedBufferCount
    }

    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        if (loadEventInfo.bytesLoaded > 0) {
            totalBytesLoaded += loadEventInfo.bytesLoaded
            totalLoadDurationMs += loadEventInfo.loadDurationMs.coerceAtLeast(1L)
        }
    }

    fun recordError(errorCodeName: String) {
        playerErrorCode = errorCodeName
        completedCleanly = false
        stallCount++
        onActiveMetricsUpdated?.invoke(startupMs, stallCount, totalStallMs, playerErrorCode, region)
    }

    override fun onPlayerError(
        eventTime: AnalyticsListener.EventTime,
        error: PlaybackException
    ) {
        recordError(error.errorCodeName)
    }

    /**
     * Finalizes session metrics and delivers the [PlaybackHealthSummary].
     * Idempotent; only the first invocation executes.
     */
    fun finalizeSession() {
        if (!isFinalized.compareAndSet(false, true)) return

        val endedAt = clock()

        // If session terminates while currently in a buffering stall, account for it
        bufferingStartEpochMs?.let { start ->
            val duration = endedAt - start
            if (duration >= MIN_STALL_THRESHOLD_MS) {
                stallCount++
                totalStallMs += duration
            }
            bufferingStartEpochMs = null
        }

        // Calculate average bitrate (bits / ms = kbps)
        val avgBitrateKbps = if (totalBytesLoaded > 0 && totalLoadDurationMs > 0) {
            ((totalBytesLoaded * 8L) / totalLoadDurationMs).toInt()
        } else {
            0
        }

        // Calculate dropped frames percentage
        val totalFrames = renderedFramesCount + droppedFramesCount
        val droppedFramesPct = if (totalFrames > 0) {
            ((droppedFramesCount.toDouble() / totalFrames) * 100.0).toFloat().coerceIn(0f, 100f)
        } else {
            0f
        }

        val summary = PlaybackHealthSummary(
            sessionId = sessionId,
            householdId = householdId,
            profileId = profileId,
            channelId = channelId,
            streamKind = streamKind,
            providerId = providerId,
            startedAtEpochMs = startedAtEpochMs,
            endedAtEpochMs = endedAt,
            startupMs = startupMs,
            stallCount = stallCount,
            totalStallMs = totalStallMs,
            avgBitrateKbps = avgBitrateKbps,
            droppedFramesPct = droppedFramesPct,
            playerErrorCode = playerErrorCode,
            region = region,
            completed = completedCleanly
        )

        onFinalized(summary)
    }
}

package com.fortresstv.player.player

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.DefaultAllocator
import com.fortresstv.player.telemetry.ConnectionHealthState

/**
 * Adaptive [LoadControl] that dynamically adjusts buffer thresholds to match the channel's observed
 * connection health.
 *
 * - EXCELLENT (Green): Ultra-fast startup buffer (600ms) for responsive, cable-like channel zapping.
 * - MODERATE (Yellow): Standard balanced buffer (1.8s startup, 15-40s cushion).
 * - POOR (Red): Deep buffer (4.0s startup, 25-60s cushion) to prevent stutter on flaky IPTV servers.
 */
@UnstableApi
@Suppress("DEPRECATION")
class AdaptiveLoadControl(
    private val allocator: DefaultAllocator = DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
    initialHealthState: ConnectionHealthState = ConnectionHealthState.UNKNOWN
) : LoadControl {

    @Volatile
    var currentHealthState: ConnectionHealthState = initialHealthState
        private set

    init {
        runCatching {
            Log.d("FortressTV.QoE", "AdaptiveLoadControl initialized with policy: $currentHealthState (${currentHealthState.bufferPolicy.label})")
        }
    }

    private val fastStartControl: DefaultLoadControl = DefaultLoadControl.Builder()
        .setAllocator(allocator)
        .setBufferDurationsMs(
            ConnectionHealthState.EXCELLENT.bufferPolicy.minBufferMs,
            ConnectionHealthState.EXCELLENT.bufferPolicy.maxBufferMs,
            ConnectionHealthState.EXCELLENT.bufferPolicy.bufferForPlaybackMs,
            ConnectionHealthState.EXCELLENT.bufferPolicy.bufferForPlaybackAfterRebufferMs
        )
        .build()

    private val balancedControl: DefaultLoadControl = DefaultLoadControl.Builder()
        .setAllocator(allocator)
        .setBufferDurationsMs(
            ConnectionHealthState.MODERATE.bufferPolicy.minBufferMs,
            ConnectionHealthState.MODERATE.bufferPolicy.maxBufferMs,
            ConnectionHealthState.MODERATE.bufferPolicy.bufferForPlaybackMs,
            ConnectionHealthState.MODERATE.bufferPolicy.bufferForPlaybackAfterRebufferMs
        )
        .build()

    private val antiFreezeControl: DefaultLoadControl = DefaultLoadControl.Builder()
        .setAllocator(allocator)
        .setBufferDurationsMs(
            ConnectionHealthState.POOR.bufferPolicy.minBufferMs,
            ConnectionHealthState.POOR.bufferPolicy.maxBufferMs,
            ConnectionHealthState.POOR.bufferPolicy.bufferForPlaybackMs,
            ConnectionHealthState.POOR.bufferPolicy.bufferForPlaybackAfterRebufferMs
        )
        .build()

    fun updateHealthState(state: ConnectionHealthState) {
        if (currentHealthState != state) {
            val previous = currentHealthState
            currentHealthState = state
            runCatching {
                Log.d("FortressTV.QoE", "AdaptiveLoadControl switched policy: $previous -> $state (${state.bufferPolicy.label})")
            }
        }
    }

    private val activeDelegate: DefaultLoadControl
        get() = when (currentHealthState) {
            ConnectionHealthState.EXCELLENT -> fastStartControl
            ConnectionHealthState.MODERATE -> balancedControl
            ConnectionHealthState.POOR -> antiFreezeControl
            ConnectionHealthState.MEASURING, ConnectionHealthState.UNKNOWN -> balancedControl
        }

    override fun onPrepared() {
        fastStartControl.onPrepared()
        balancedControl.onPrepared()
        antiFreezeControl.onPrepared()
    }

    override fun onPrepared(playerId: PlayerId) {
        fastStartControl.onPrepared(playerId)
        balancedControl.onPrepared(playerId)
        antiFreezeControl.onPrepared(playerId)
    }

    override fun onTracksSelected(
        parameters: LoadControl.Parameters,
        trackGroups: TrackGroupArray,
        trackSelections: Array<out ExoTrackSelection>
    ) {
        activeDelegate.onTracksSelected(parameters, trackGroups, trackSelections)
    }

    override fun onTracksSelected(
        timeline: Timeline,
        mediaPeriodId: MediaSource.MediaPeriodId,
        renderers: Array<out Renderer>,
        trackGroups: TrackGroupArray,
        trackSelections: Array<out ExoTrackSelection>
    ) {
        activeDelegate.onTracksSelected(timeline, mediaPeriodId, renderers, trackGroups, trackSelections)
    }

    override fun onTracksSelected(
        renderers: Array<out Renderer>,
        trackGroups: TrackGroupArray,
        trackSelections: Array<out ExoTrackSelection>
    ) {
        activeDelegate.onTracksSelected(renderers, trackGroups, trackSelections)
    }

    override fun onStopped() {
        fastStartControl.onStopped()
        balancedControl.onStopped()
        antiFreezeControl.onStopped()
    }

    override fun onStopped(playerId: PlayerId) {
        fastStartControl.onStopped(playerId)
        balancedControl.onStopped(playerId)
        antiFreezeControl.onStopped(playerId)
    }

    override fun onReleased() {
        fastStartControl.onReleased()
        balancedControl.onReleased()
        antiFreezeControl.onReleased()
    }

    override fun onReleased(playerId: PlayerId) {
        fastStartControl.onReleased(playerId)
        balancedControl.onReleased(playerId)
        antiFreezeControl.onReleased(playerId)
    }

    override fun getAllocator(playerId: PlayerId): Allocator = allocator

    override fun getBackBufferDurationUs(): Long = activeDelegate.getBackBufferDurationUs(PlayerId.UNSET)

    override fun getBackBufferDurationUs(playerId: PlayerId): Long = activeDelegate.getBackBufferDurationUs(playerId)

    override fun retainBackBufferFromKeyframe(): Boolean = activeDelegate.retainBackBufferFromKeyframe(PlayerId.UNSET)

    override fun retainBackBufferFromKeyframe(playerId: PlayerId): Boolean = activeDelegate.retainBackBufferFromKeyframe(playerId)

    override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean =
        activeDelegate.shouldContinueLoading(parameters)

    override fun shouldContinueLoading(
        playbackPositionUs: Long,
        bufferedDurationUs: Long,
        playbackSpeed: Float
    ): Boolean = activeDelegate.shouldContinueLoading(playbackPositionUs, bufferedDurationUs, playbackSpeed)

    override fun shouldContinuePreloading(
        playerId: PlayerId,
        timeline: Timeline,
        mediaPeriodId: MediaSource.MediaPeriodId,
        bufferedDurationUs: Long
    ): Boolean = activeDelegate.shouldContinuePreloading(playerId, timeline, mediaPeriodId, bufferedDurationUs)

    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean =
        activeDelegate.shouldStartPlayback(parameters)

    override fun shouldStartPlayback(
        bufferedDurationUs: Long,
        playbackSpeed: Float,
        rebuffering: Boolean,
        targetLiveOffsetUs: Long
    ): Boolean = activeDelegate.shouldStartPlayback(bufferedDurationUs, playbackSpeed, rebuffering, targetLiveOffsetUs)

    override fun shouldStartPlayback(
        timeline: Timeline,
        mediaPeriodId: MediaSource.MediaPeriodId,
        bufferedDurationUs: Long,
        playbackSpeed: Float,
        rebuffering: Boolean,
        targetLiveOffsetUs: Long
    ): Boolean = activeDelegate.shouldStartPlayback(timeline, mediaPeriodId, bufferedDurationUs, playbackSpeed, rebuffering, targetLiveOffsetUs)
}

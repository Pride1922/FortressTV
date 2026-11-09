package com.fortresstv.player

import android.content.Context
import androidx.media3.common.Player

/**
 * Workflow owner for coordinating player Picture-in-Picture eligibility,
 * sleep timer auto-stop registration, and fullscreen playback policy.
 */
internal object PlaybackOrchestrationWorkflow {
    /**
     * Synchronizes Picture-in-Picture state with the host Activity.
     * Strictly enforces the invariant: PiP is fullscreen-only. Pressing Home
     * from inline/preview playback pauses playback instead of entering PiP.
     */
    fun updatePiPState(
        context: Context,
        autoPiPEnabled: Boolean,
        isFullscreen: Boolean,
        isPlaying: Boolean
    ) {
        val activity = context.findActivity() as? MainActivity ?: return
        activity.updatePictureInPictureState(
            enabled = autoPiPEnabled,
            fullscreenPlaying = isFullscreen && isPlaying
        )
    }

    /**
     * Binds an active Player instance to the host Activity's PlaybackSleepTimer.
     * On deadline expiry, audio/video pauses promptly and releases kept-screen flags.
     */
    fun registerSleepTimer(
        context: Context,
        player: Player,
        onAutoStop: () -> Unit
    ): (() -> Unit)? {
        val activity = context.findActivity() as? MainActivity ?: return null
        return activity.registerSleepTimerPlayer(player, onAutoStop)
    }
}

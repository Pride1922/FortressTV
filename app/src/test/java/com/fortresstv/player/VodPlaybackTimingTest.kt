package com.fortresstv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VodPlaybackTimingTest {
    @Test
    fun nextEpisodeCountdownStartsAtFortyFiveSecondsRemaining() {
        val duration = 30 * 60_000L
        assertFalse(shouldStartNextEpisodeCountdown(duration - 45_001L, duration, true, false, true, -1))
        assertTrue(shouldStartNextEpisodeCountdown(duration - 45_000L, duration, true, false, true, -1))
        assertFalse(shouldStartNextEpisodeCountdown(duration - 30_000L, duration, false, false, true, -1))
        assertFalse(shouldStartNextEpisodeCountdown(duration - 30_000L, duration, true, true, true, -1))
        assertFalse(shouldStartNextEpisodeCountdown(duration - 30_000L, duration, true, false, false, -1))
        assertFalse(shouldStartNextEpisodeCountdown(duration - 30_000L, duration, true, false, true, 5))
    }
}

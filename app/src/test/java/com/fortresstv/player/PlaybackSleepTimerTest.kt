package com.fortresstv.player

import org.junit.Assert.*
import org.junit.Test

class PlaybackSleepTimerTest {
    private class Harness {
        var now = 0L
        var closeCount = 0
        var pending: (() -> Unit)? = null
        val timer = PlaybackSleepTimer(
            elapsedRealtime = { now },
            schedule = { _, action ->
                pending = action
                val cancel: () -> Unit = { pending = null }
                cancel
            },
            onStateChanged = {},
            onExpired = { closeCount++ }
        )
        fun advance(ms: Long) {
            now += ms
            val action = pending
            pending = null
            action?.invoke()
        }
    }

    @Test fun oneHourExpiresEvenWhenEpisodesEndAndNoScreenObservesTimer() {
        val h = Harness()
        h.timer.select(60)
        h.advance(20 * 60_000L)
        h.timer.mediaEnded()
        h.advance(20 * 60_000L)
        h.timer.mediaEnded()
        assertEquals(0, h.closeCount)
        h.advance(20 * 60_000L)
        assertEquals(1, h.closeCount)
        assertTrue(h.timer.expired)
    }

    @Test fun endOfEpisodeClosesOnceWithoutConfirmation() {
        val h = Harness()
        h.timer.select(-1)
        h.timer.mediaEnded()
        h.timer.mediaEnded()
        assertEquals(1, h.closeCount)
        assertTrue(h.timer.expired)
    }

    @Test fun offCancelsDeadlineAndEndOfEpisode() {
        val h = Harness()
        h.timer.select(60)
        h.timer.select(null)
        h.advance(120 * 60_000L)
        h.timer.mediaEnded()
        assertEquals(0, h.closeCount)
        h.timer.select(-1)
        h.timer.select(null)
        h.timer.mediaEnded()
        assertEquals(0, h.closeCount)
    }

    @Test fun programmeUsesSelectedDeadlineRatherThanMediaEnd() {
        val h = Harness()
        h.timer.select(-1, 30_000L)
        h.timer.mediaEnded()
        assertEquals(0, h.closeCount)
        h.advance(29_999L)
        assertEquals(0, h.closeCount)
        h.advance(1L)
        assertEquals(1, h.closeCount)
    }

    @Test fun expiredTimerCannotBeRearmedByLatePlaybackCallbacks() {
        val h = Harness()
        h.timer.select(15)
        // Simulate resuming after the deadline before the scheduled callback runs.
        h.now = 16 * 60_000L
        h.timer.checkDeadline()
        h.timer.select(120)
        h.timer.checkDeadline()
        assertEquals(1, h.closeCount)
        assertTrue(h.timer.expired)
        assertNull(h.pending)
    }

    @Test fun changingTimerReplacesOldDeadline() {
        val h = Harness()
        h.timer.select(15)
        h.advance(10 * 60_000L)
        h.timer.select(60)
        h.advance(5 * 60_000L)
        assertEquals(0, h.closeCount)
        assertEquals(55L, h.timer.state.remainingMinutes)
        h.advance(55 * 60_000L)
        assertEquals(1, h.closeCount)
    }
}

package com.fortresstv.player

import org.junit.Assert.*
import org.junit.Test

class PlaybackSleepTimerTest {
    private class Harness {
        var now = 0L
        var promptCount = 0
        var stopCount = 0
        var pending: (() -> Unit)? = null
        val timer = PlaybackSleepTimer(
            elapsedRealtime = { now },
            schedule = { _, action ->
                pending = action
                val cancel: () -> Unit = { pending = null }
                cancel
            },
            onStateChanged = {},
            onExpired = { promptCount++ },
            onAutoStop = { stopCount++ }
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
        assertEquals(0, h.promptCount)
        h.advance(20 * 60_000L)
        assertEquals(1, h.promptCount)
        assertEquals(0, h.stopCount)
        assertTrue(h.timer.expired)
    }

    @Test fun endOfEpisodePausesAndRequestsConfirmationOnce() {
        val h = Harness()
        h.timer.select(-1)
        h.timer.mediaEnded()
        h.timer.mediaEnded()
        assertEquals(1, h.promptCount)
        assertEquals(0, h.stopCount)
        assertTrue(h.timer.expired)
    }

    @Test fun offCancelsDeadlineAndEndOfEpisode() {
        val h = Harness()
        h.timer.select(60)
        h.timer.select(null)
        h.advance(120 * 60_000L)
        h.timer.mediaEnded()
        assertEquals(0, h.promptCount)
        h.timer.select(-1)
        h.timer.select(null)
        h.timer.mediaEnded()
        assertEquals(0, h.promptCount)
    }

    @Test fun programmeUsesSelectedDeadlineRatherThanMediaEnd() {
        val h = Harness()
        h.timer.select(-1, 30_000L)
        h.timer.mediaEnded()
        assertEquals(0, h.promptCount)
        h.advance(29_999L)
        assertEquals(0, h.promptCount)
        h.advance(1L)
        assertEquals(1, h.promptCount)
    }

    @Test fun userCanSetAnotherTimerAfterPrompt() {
        val h = Harness()
        h.timer.select(15)
        // Simulate resuming after the deadline before the scheduled callback runs.
        h.now = 16 * 60_000L
        h.timer.checkDeadline()
        assertEquals(1, h.promptCount)
        h.timer.select(120)
        assertFalse(h.timer.expired)
        assertEquals(120L, h.timer.state.remainingMinutes)
        assertNotNull(h.pending)
    }

    @Test fun changingTimerReplacesOldDeadline() {
        val h = Harness()
        h.timer.select(15)
        h.advance(10 * 60_000L)
        h.timer.select(60)
        h.advance(5 * 60_000L)
        assertEquals(0, h.promptCount)
        assertEquals(55L, h.timer.state.remainingMinutes)
        h.advance(55 * 60_000L)
        assertEquals(1, h.promptCount)
    }

    @Test fun noResponseStopsPlaybackAfterThirtySeconds() {
        val h = Harness()
        h.timer.select(-1)
        h.timer.mediaEnded()
        repeat(29) { h.advance(1_000L) }
        assertEquals(0, h.stopCount)
        assertEquals(1, h.timer.state.confirmationSeconds)
        h.advance(1_000L)
        assertEquals(1, h.stopCount)
        assertFalse(h.timer.expired)
        assertFalse(h.timer.state.awaitingConfirmation)
    }

    @Test fun stopNowDoesNotWaitForCountdown() {
        val h = Harness()
        h.timer.select(-1)
        h.timer.mediaEnded()
        h.timer.stopNow()
        assertEquals(1, h.stopCount)
        assertNull(h.pending)
    }

    @Test fun tabletSleepCannotExtendConfirmationDeadline() {
        val h = Harness()
        h.timer.select(-1)
        h.timer.mediaEnded()
        h.now += 5 * 60_000L
        h.timer.checkDeadline()
        assertEquals(1, h.stopCount)
        assertFalse(h.timer.state.awaitingConfirmation)
    }
}

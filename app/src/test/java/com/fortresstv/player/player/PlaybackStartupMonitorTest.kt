package com.fortresstv.player.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStartupMonitorTest {
    private fun sample(bytes: Long = 0, buffer: Long = 0, network: Boolean = true, attempts: Int = 0) =
        BufferingSample(0, buffer, bytes, network, attempts)

    @Test fun partialDataThenSilenceIsCheckedAgain() = runBlocking {
        val samples = listOf(sample(), sample(bytes = 100), sample(bytes = 100))
        var index = 0
        var waits = 0
        val result = awaitBufferingStall(15_000, { samples[index++] }, { waits++ })
        assertEquals(StallDecision.RECOVER, result)
        assertEquals(2, waits)
    }

    @Test fun progressingBufferIsPreservedUntilReady() = runBlocking {
        val samples = listOf(sample(), sample(buffer = 700), sample(buffer = 1400), null)
        var index = 0
        assertNull(awaitBufferingStall(15_000, { samples[index++] }, {}))
        assertEquals(4, index)
    }

    @Test fun disconnectedStartupKeepsCheckingAndRecoversWhenRouteReturns() = runBlocking {
        val samples = listOf(sample(network = false), sample(network = false), sample())
        var index = 0
        assertEquals(StallDecision.RECOVER, awaitBufferingStall(15_000, { samples[index++] }, {}))
    }

    @Test fun retryLimitStillAppliesAfterWaitingForProgress() = runBlocking {
        val samples = listOf(sample(attempts = 3), sample(bytes = 1, attempts = 3), sample(bytes = 1, attempts = 3))
        var index = 0
        assertEquals(StallDecision.GIVE_UP, awaitBufferingStall(15_000, { samples[index++] }, {}))
    }

    @Test fun pauseOrErrorEndsObservation() = runBlocking {
        var calls = 0
        assertNull(awaitBufferingStall(15_000, { if (calls++ == 0) sample() else null }, {}))
        assertEquals(2, calls)
    }

    @Test(expected = CancellationException::class)
    fun channelChangeCancellationIsNotSwallowed() = runBlocking {
        awaitBufferingStall(15_000, { sample() }, { throw CancellationException() })
        Unit
    }

    @Test fun initiallyOfflineFirstNetworkTriggersRecoveryWithoutValidation() {
        val tracker = PlaybackNetworkTracker<String>()
        assertTrue(tracker.update("cellular", true, false))
        assertFalse(tracker.update("cellular", true, false))
        assertTrue(tracker.update("cellular", true, true))
        assertFalse(tracker.update("cellular", true, true))
    }

    @Test fun initialCallbackIsIgnoredButHandoverAndReconnectAreDetected() {
        val tracker = PlaybackNetworkTracker("wifi", true, true)
        assertFalse(tracker.update("wifi", true, true))
        assertTrue(tracker.update("cellular", true, false))
        tracker.lost("wifi")
        assertFalse(tracker.update("cellular", true, false))
        tracker.lost("cellular")
        assertTrue(tracker.update("cellular", true, false))
    }
}

package com.fortresstv.player.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackResilienceTest {
    @Test
    fun slowProgressWaitsWithoutDiscardingBuffer() {
        assertEquals(StallDecision.WAIT, StallRecoveryPolicy.decide(0, 600, 0, true))
        assertEquals(StallDecision.WAIT, StallRecoveryPolicy.decide(0, 0, 1, true, bytesProgress = 1))
    }

    @Test
    fun disconnectedNetworkWaitsForConnectivity() {
        assertEquals(StallDecision.WAIT, StallRecoveryPolicy.decide(0, 0, 0, false))
    }

    @Test
    fun deadConnectionRecoversThenStopsAtLimit() {
        assertEquals(StallDecision.RECOVER, StallRecoveryPolicy.decide(0, 0, 2, true))
        assertEquals(StallDecision.GIVE_UP, StallRecoveryPolicy.decide(0, 0, 3, true))
    }

    @Test
    fun retryUsesCappedExponentialDelay() {
        assertEquals(1_000L, PlaybackRetryRules.retryDelayMs(IOException("timeout"), 1))
        assertEquals(16_000L, PlaybackRetryRules.retryDelayMs(IOException("timeout"), 5))
        assertNull(PlaybackRetryRules.retryDelayMs(IOException("timeout"), 6))
    }

    @Test
    fun permanentHttpErrorsAreNotRetried() {
        assertNull(PlaybackRetryRules.retryDelayMs(403, 1))
        assertEquals(1_000L, PlaybackRetryRules.retryDelayMs(503, 1))
    }

    @Test
    fun profilesTradeLatencyForStability() {
        val automatic = PlaybackProfiles.forContent(PlaybackContentType.LIVE, PlaybackMode.AUTO)
        val stable = PlaybackProfiles.forContent(PlaybackContentType.LIVE, PlaybackMode.STABLE)
        assert(stable.minBufferMs > automatic.minBufferMs)
        assert(stable.targetLiveOffsetMs!! > automatic.targetLiveOffsetMs!!)
    }
}

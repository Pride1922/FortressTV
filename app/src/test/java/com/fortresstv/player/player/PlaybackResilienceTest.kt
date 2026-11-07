package com.fortresstv.player.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import androidx.media3.common.Player

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

    @Test
    fun readyFreezeRequiresActiveVideoAndNetwork() {
        assertEquals(true, ReadyFreezePolicy.shouldRecover(true, true, true, 12_000))
        assertEquals(false, ReadyFreezePolicy.shouldRecover(false, true, true, 30_000))
        assertEquals(false, ReadyFreezePolicy.shouldRecover(true, false, true, 30_000))
        assertEquals(false, ReadyFreezePolicy.shouldRecover(true, true, false, 30_000))
        assertEquals(false, ReadyFreezePolicy.shouldRecover(true, true, true, 11_999))
    }

    @Test
    fun readyFreezeNoticeRequiresActiveVideoAndSustainedPause() {
        assertEquals(true, ReadyFreezePolicy.shouldShowNotice(true, true, 3_500))
        assertEquals(false, ReadyFreezePolicy.shouldShowNotice(false, true, 5_000))
        assertEquals(false, ReadyFreezePolicy.shouldShowNotice(true, false, 5_000))
        assertEquals(false, ReadyFreezePolicy.shouldShowNotice(true, true, 3_499))
    }

    @Test
    fun liveEndedRequiresPlayWhenReadyAndNetworkToRecover() {
        assertEquals(StallDecision.RECOVER, LiveEndedRecoveryPolicy.decide(Player.STATE_ENDED, true, true, 0))
        assertEquals(StallDecision.RECOVER, LiveEndedRecoveryPolicy.decide(Player.STATE_ENDED, true, true, 2))
        assertEquals(StallDecision.GIVE_UP, LiveEndedRecoveryPolicy.decide(Player.STATE_ENDED, true, true, 3))
        assertEquals(StallDecision.WAIT, LiveEndedRecoveryPolicy.decide(Player.STATE_ENDED, true, false, 0))
        assertEquals(StallDecision.WAIT, LiveEndedRecoveryPolicy.decide(Player.STATE_ENDED, false, true, 0))
        assertEquals(StallDecision.WAIT, LiveEndedRecoveryPolicy.decide(Player.STATE_READY, true, true, 0))
    }
}

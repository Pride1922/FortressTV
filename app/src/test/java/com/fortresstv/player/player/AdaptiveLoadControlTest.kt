package com.fortresstv.player.player

import androidx.media3.common.util.UnstableApi
import com.fortresstv.player.telemetry.ConnectionHealthState
import org.junit.Assert.assertEquals
import org.junit.Test

@UnstableApi
class AdaptiveLoadControlTest {

    @Test
    fun `buffer policies are correctly defined per health state`() {
        val excellentPolicy = ConnectionHealthState.EXCELLENT.bufferPolicy
        assertEquals(600, excellentPolicy.bufferForPlaybackMs)
        assertEquals(8_000, excellentPolicy.minBufferMs)
        assertEquals(25_000, excellentPolicy.maxBufferMs)

        val moderatePolicy = ConnectionHealthState.MODERATE.bufferPolicy
        assertEquals(1_800, moderatePolicy.bufferForPlaybackMs)
        assertEquals(15_000, moderatePolicy.minBufferMs)
        assertEquals(40_000, moderatePolicy.maxBufferMs)

        val poorPolicy = ConnectionHealthState.POOR.bufferPolicy
        assertEquals(4_000, poorPolicy.bufferForPlaybackMs)
        assertEquals(25_000, poorPolicy.minBufferMs)
        assertEquals(60_000, poorPolicy.maxBufferMs)

        val unknownPolicy = ConnectionHealthState.UNKNOWN.bufferPolicy
        assertEquals(1_500, unknownPolicy.bufferForPlaybackMs)
    }

    @Test
    fun `adaptive load control switches health state dynamically`() {
        val loadControl = AdaptiveLoadControl(initialHealthState = ConnectionHealthState.UNKNOWN)
        assertEquals(ConnectionHealthState.UNKNOWN, loadControl.currentHealthState)

        loadControl.updateHealthState(ConnectionHealthState.EXCELLENT)
        assertEquals(ConnectionHealthState.EXCELLENT, loadControl.currentHealthState)

        loadControl.updateHealthState(ConnectionHealthState.POOR)
        assertEquals(ConnectionHealthState.POOR, loadControl.currentHealthState)
    }
}

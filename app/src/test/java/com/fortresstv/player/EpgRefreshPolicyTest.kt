package com.fortresstv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class EpgRefreshPolicyTest {
    private val zone = ZoneId.of("Europe/Lisbon")

    @Test
    fun refreshesAfterLocalMidnightEvenWhenCacheIsLessThan24HoursOld() {
        val lastUpdate = millis("2026-07-10T22:30:00Z")
        assertFalse(isEpgRefreshDue(lastUpdate, millis("2026-07-10T22:55:00Z"), zone, coversWindow = true))
        assertTrue(isEpgRefreshDue(lastUpdate, millis("2026-07-10T23:05:00Z"), zone, coversWindow = true))
    }

    @Test
    fun retriesAnIncompleteWindowAfterOneHourButKeepsACompleteSameDayGuide() {
        val lastUpdate = millis("2026-07-10T06:00:00Z")
        assertFalse(isEpgRefreshDue(lastUpdate, millis("2026-07-10T06:59:00Z"), zone, coversWindow = false))
        assertTrue(isEpgRefreshDue(lastUpdate, millis("2026-07-10T07:00:00Z"), zone, coversWindow = false))
        assertFalse(isEpgRefreshDue(lastUpdate, millis("2026-07-10T07:00:00Z"), zone, coversWindow = true))
    }

    private fun millis(value: String): Long = Instant.parse(value).toEpochMilli()
}

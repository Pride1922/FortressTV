package com.fortresstv.player.epg

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class EpgScheduleTest {
    @Test
    fun findsCurrentAndNextProgramme() {
        val guide = EpgGuide(
            channels = emptyMap(),
            programmes = mapOf("RTP1.pt" to listOf(
                programme("20260829120000 +0000", "20260829130000 +0000", "News"),
                programme("20260829130000 +0000", "20260829140000 +0000", "Talk show")
            ))
        )

        val snapshot = guide.snapshot("RTP1.pt", Instant.parse("2026-08-29T12:30:00Z"))

        assertEquals("News", snapshot.current?.title)
        assertEquals("Talk show", snapshot.next?.title)
    }

    @Test
    fun parsesXmlTvOffset() {
        assertEquals(Instant.parse("2026-08-29T11:00:00Z"), XmlTvTime.parse("20260829120000 +0100"))
    }

    @Test
    fun parsesXmlTvWithTimeShift() {
        // Shifting +1 hour moves 12:00 UTC to 13:00 UTC
        assertEquals(Instant.parse("2026-08-29T13:00:00Z"), XmlTvTime.parse("20260829120000 +0000", timeShiftHours = 1))
        // Shifting -2 hours moves 12:00 UTC to 10:00 UTC
        assertEquals(Instant.parse("2026-08-29T10:00:00Z"), XmlTvTime.parse("20260829120000 +0000", timeShiftHours = -2))
    }

    @Test
    fun parsesXmlTvWithCorrectionMinutes() {
        // +15 minutes
        assertEquals(Instant.parse("2026-08-29T12:15:00Z"), XmlTvTime.parse("20260829120000 +0000", correctionMinutes = 15))
        // -45 minutes
        assertEquals(Instant.parse("2026-08-29T11:15:00Z"), XmlTvTime.parse("20260829120000 +0000", correctionMinutes = -45))
    }

    @Test
    fun parsesXmlTvWithFallbackTimezoneWhenOffsetMissing() {
        val lisbon = java.time.ZoneId.of("Europe/Lisbon")
        // In August, Lisbon is UTC+1 (WEST)
        // 12:00 Lisbon time = 11:00 UTC
        assertEquals(Instant.parse("2026-08-29T11:00:00Z"), XmlTvTime.parse("20260829120000", fallbackZone = lisbon))

        val newYork = java.time.ZoneId.of("America/New_York")
        // In August, New York is UTC-4 (EDT)
        // 12:00 NY time = 16:00 UTC
        assertEquals(Instant.parse("2026-08-29T16:00:00Z"), XmlTvTime.parse("20260829120000", fallbackZone = newYork))

        // When offset is present in XMLTV, it MUST override fallbackZone
        assertEquals(Instant.parse("2026-08-29T10:00:00Z"), XmlTvTime.parse("20260829120000 +0200", fallbackZone = newYork))
    }

    @Test
    fun findsCurrentProgrammeWithTimeShift() {
        val guide = EpgGuide(
            channels = emptyMap(),
            programmes = mapOf("RTP1.pt" to listOf(
                programme("20260829120000 +0000", "20260829130000 +0000", "News"),
                programme("20260829130000 +0000", "20260829140000 +0000", "Talk show")
            ))
        )

        // At 13:30 UTC with +1h shift:
        // News (12:00-13:00) shifted by +1h becomes 13:00-14:00
        val snapshot = guide.snapshot("RTP1.pt", Instant.parse("2026-08-29T13:30:00Z"), timeShiftHours = 1)
        assertEquals("News", snapshot.current?.title)
        assertEquals("Talk show", snapshot.next?.title)
    }

    @Test
    fun findsCurrentProgrammeWithCorrectionMinutes() {
        val guide = EpgGuide(
            channels = emptyMap(),
            programmes = mapOf("RTP1.pt" to listOf(
                programme("20260829120000 +0000", "20260829130000 +0000", "News"),
                programme("20260829130000 +0000", "20260829140000 +0000", "Talk show")
            ))
        )

        // News is 12:00-13:00 UTC. At 13:10 UTC with +15m correction:
        // Programme appears shifted later by 15 mins (12:15-13:15 UTC).
        val snapshot = guide.snapshot("RTP1.pt", Instant.parse("2026-08-29T13:10:00Z"), correctionMinutes = 15)
        assertEquals("News", snapshot.current?.title)
        assertEquals("Talk show", snapshot.next?.title)
    }

    @Test
    fun snapshotPrefersManualXmltvId() {
        val guide = EpgGuide(
            channels = emptyMap(),
            programmes = mapOf(
                "RTP1.pt" to listOf(programme("20260829120000 +0000", "20260829130000 +0000", "RTP1 News")),
                "Manual.RTP1" to listOf(programme("20260829120000 +0000", "20260829130000 +0000", "Manual Override News"))
            )
        )

        // Without manual override, it resolves to RTP1.pt
        val autoSnapshot = guide.snapshot("RTP1.pt", Instant.parse("2026-08-29T12:30:00Z"))
        assertEquals("RTP1 News", autoSnapshot.current?.title)

        // With manual override, it resolves to Manual.RTP1
        val manualSnapshot = guide.snapshot("RTP1.pt", Instant.parse("2026-08-29T12:30:00Z"), manualXmltvId = "Manual.RTP1")
        assertEquals("Manual Override News", manualSnapshot.current?.title)
    }

    private fun programme(start: String, stop: String, title: String) = EpgProgramme(
        channelId = "RTP1.pt",
        title = title,
        description = null,
        category = null,
        startEpochMilli = XmlTvTime.parseToEpoch(start)!!,
        stopEpochMilli = XmlTvTime.parseToEpoch(stop)
    )
}

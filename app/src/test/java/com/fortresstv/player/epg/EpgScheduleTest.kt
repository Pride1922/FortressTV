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
                EpgProgramme("RTP1.pt", "20260829120000 +0000", "20260829130000 +0000", "News", null, null),
                EpgProgramme("RTP1.pt", "20260829130000 +0000", "20260829140000 +0000", "Talk show", null, null)
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
}

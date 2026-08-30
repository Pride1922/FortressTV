package com.fortresstv.player.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class EpgCacheCodecTest {
    private val guide = EpgGuide(
        channels = linkedMapOf(
            "RTP1.pt" to EpgChannel("RTP1.pt", "RTP 1", "https://example.com/rtp1.png")
        ),
        programmes = linkedMapOf(
            "RTP1.pt" to listOf(
                EpgProgramme(
                    channelId = "RTP1.pt",
                    title = "Jornal da Tarde",
                    description = "Notícias do dia.",
                    category = "Notícias",
                    startEpochMilli = XmlTvTime.parseToEpoch("20260829120000 +0000")!!,
                    stopEpochMilli = XmlTvTime.parseToEpoch("20260829130000 +0000")
                )
            )
        )
    )

    @Test
    fun roundTripsGuideWithoutLosingProgrammeData() {
        val bytes = ByteArrayOutputStream().also {
            EpgCacheCodec.write(it, "source-v1", guide)
        }.toByteArray()

        val restored = EpgCacheCodec.read(ByteArrayInputStream(bytes), "source-v1")

        assertEquals(guide, restored)
    }

    @Test
    fun rejectsCacheWhenSourceFingerprintChanges() {
        val bytes = ByteArrayOutputStream().also {
            EpgCacheCodec.write(it, "old-source", guide)
        }.toByteArray()

        assertThrows(IllegalArgumentException::class.java) {
            EpgCacheCodec.read(ByteArrayInputStream(bytes), "new-source")
        }
    }
}

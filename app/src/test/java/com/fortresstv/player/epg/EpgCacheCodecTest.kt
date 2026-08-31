package com.fortresstv.player.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class EpgCacheCodecTest {
    private val channel = EpgChannel("RTP1.pt", "RTP 1", "https://example.com/rtp1.png")
    private val programme = EpgProgramme(
        channelId = "RTP1.pt",
        title = "Jornal da Tarde",
        description = "Notícias do dia.",
        category = "Notícias",
        startEpochMilli = XmlTvTime.parseToEpoch("20260829120000 +0000")!!,
        stopEpochMilli = XmlTvTime.parseToEpoch("20260829130000 +0000")
    )
    private val manifest = EpgManifest(
        generationId = "gen-123",
        sourceFingerprint = "source-v1",
        timezoneId = "UTC",
        loadedDays = 3,
        todayEpochDay = 20700L,
        createdAtMillis = 1756470000000L,
        channels = mapOf("RTP1.pt" to channel),
        partitions = mapOf(
            20700L to EpgPartitionMetadata(20700L, "epg_day_gen-123_20700.bin", 1, 1024L, "checksum123")
        )
    )
    private val day = EpgDay(
        epochDay = 20700L,
        programmes = mapOf("RTP1.pt" to listOf(programme))
    )

    @Test
    fun roundTripsManifestWithoutLosingMetadata() {
        val bytes = ByteArrayOutputStream().also {
            EpgCacheCodec.writeManifest(it, manifest)
        }.toByteArray()

        val restored = EpgCacheCodec.readManifest(ByteArrayInputStream(bytes), "source-v1")

        assertEquals(manifest, restored)
    }

    @Test
    fun rejectsManifestWhenSourceFingerprintChanges() {
        val bytes = ByteArrayOutputStream().also {
            EpgCacheCodec.writeManifest(it, manifest)
        }.toByteArray()

        assertThrows(IllegalArgumentException::class.java) {
            EpgCacheCodec.readManifest(ByteArrayInputStream(bytes), "new-source")
        }
    }

    @Test
    fun roundTripsDayPartitionWithoutLosingProgrammeData() {
        val bytes = ByteArrayOutputStream().also {
            EpgCacheCodec.writeDayPartition(it, "gen-123", day)
        }.toByteArray()

        val restored = EpgCacheCodec.readDayPartition(ByteArrayInputStream(bytes), "gen-123", 20700L)

        assertEquals(day, restored)
    }
}

package com.fortresstv.player.epg

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EpgRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val channel = EpgChannel("RTP1.pt", "RTP 1", "https://example.com/rtp1.png")
    private val programme1 = EpgProgramme(
        channelId = "RTP1.pt",
        title = "Jornal da Tarde",
        description = "Notícias do dia.",
        category = "Notícias",
        startEpochMilli = 1756470000000L,
        stopEpochMilli = 1756473600000L
    )
    private val programme2 = EpgProgramme(
        channelId = "RTP1.pt",
        title = "Telejornal Amanhã",
        description = "Notícias de amanhã.",
        category = "Notícias",
        startEpochMilli = 1756556400000L,
        stopEpochMilli = 1756560000000L
    )

    private val manifest = EpgManifest(
        generationId = "gen-1",
        sourceFingerprint = "fp",
        timezoneId = "UTC",
        loadedDays = 3,
        todayEpochDay = 20700L,
        channels = mapOf("RTP1.pt" to channel),
        partitions = mapOf(
            20700L to EpgPartitionMetadata(20700L, "epg_day_gen-1_20700.bin", 1),
            20701L to EpgPartitionMetadata(20701L, "epg_day_gen-1_20701.bin", 1),
            20702L to EpgPartitionMetadata(20702L, "epg_day_gen-1_20702.bin", 1)
        )
    )

    @Test
    fun pinsTodayPartitionWhenGuideIsSet() = runBlocking {
        val repo = DefaultEpgRepository(epgDir = tempFolder.root)
        val guide = EpgGuide(
            channels = mapOf("RTP1.pt" to channel),
            programmes = mapOf("RTP1.pt" to listOf(programme1)),
            manifest = manifest
        )

        repo.setGuide(guide)

        assertEquals(manifest, repo.manifest.value)
        assertNotNull(repo.today.value)
        assertEquals(20700L, repo.today.value?.epochDay)
        assertEquals(20700L, repo.cachedDay(20700L)?.epochDay)
        assertNull(repo.cachedDay(20701L))
    }

    @Test
    fun loadsAndRetainsConfiguredDayPartitions() = runBlocking {
        val epgDir = tempFolder.root
        // Write day 20701 partition file
        val day20701 = EpgDay(20701L, mapOf("RTP1.pt" to listOf(programme2)))
        val partition20701File = File(epgDir, "epg_day_gen-1_20701.bin")
        partition20701File.outputStream().use { stream ->
            EpgCacheCodec.writeDayPartition(stream, "gen-1", day20701)
        }

        // Write day 20702 partition file
        val day20702 = EpgDay(20702L, mapOf("RTP1.pt" to listOf(programme1)))
        val partition20702File = File(epgDir, "epg_day_gen-1_20702.bin")
        partition20702File.outputStream().use { stream ->
            EpgCacheCodec.writeDayPartition(stream, "gen-1", day20702)
        }

        val repo = DefaultEpgRepository(epgDir = epgDir)
        val guide = EpgGuide(
            channels = mapOf("RTP1.pt" to channel),
            programmes = mapOf("RTP1.pt" to listOf(programme1)),
            manifest = manifest
        )
        repo.setGuide(guide)

        // Load day 20701
        val loaded20701 = repo.loadDay(20701L)
        assertNotNull(loaded20701)
        assertEquals("Telejornal Amanhã", loaded20701?.programmes?.get("RTP1.pt")?.first()?.title)
        assertEquals(20701L, repo.cachedDay(20701L)?.epochDay)

        // Today remains pinned
        assertEquals(20700L, repo.cachedDay(20700L)?.epochDay)

        // Load day 20702; both configured future days remain available for instant switching.
        val loaded20702 = repo.loadDay(20702L)
        assertNotNull(loaded20702)
        assertEquals(20702L, repo.cachedDay(20702L)?.epochDay)
        assertEquals(20701L, repo.cachedDay(20701L)?.epochDay)
        // Today is STILL pinned
        assertEquals(20700L, repo.cachedDay(20700L)?.epochDay)
    }

    @Test
    fun deduplicatesConcurrentLoadsForSameDay() = runBlocking {
        val epgDir = tempFolder.root
        val day20701 = EpgDay(20701L, mapOf("RTP1.pt" to listOf(programme2)))
        val partition20701File = File(epgDir, "epg_day_gen-1_20701.bin")
        partition20701File.outputStream().use { stream ->
            EpgCacheCodec.writeDayPartition(stream, "gen-1", day20701)
        }

        val repo = DefaultEpgRepository(epgDir = epgDir)
        val guide = EpgGuide(
            channels = mapOf("RTP1.pt" to channel),
            programmes = mapOf("RTP1.pt" to listOf(programme1)),
            manifest = manifest
        )
        repo.setGuide(guide)

        // Launch 5 concurrent requests for day 20701
        val results = (1..5).map {
            async { repo.loadDay(20701L) }
        }.awaitAll()

        assertEquals(5, results.size)
        results.forEach {
            assertEquals("Telejornal Amanhã", it?.programmes?.get("RTP1.pt")?.first()?.title)
        }
    }
}


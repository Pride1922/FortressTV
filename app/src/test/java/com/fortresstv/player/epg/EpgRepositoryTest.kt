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
import java.time.LocalDate
import java.time.ZoneId

class EpgRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val currentDay = LocalDate.now(ZoneId.of("UTC")).toEpochDay()
    private val day0 = currentDay
    private val day1 = currentDay + 1
    private val day2 = currentDay + 2

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
        todayEpochDay = day0,
        channels = mapOf("RTP1.pt" to channel),
        partitions = mapOf(
            day0 to EpgPartitionMetadata(day0, "epg_day_gen-1_$day0.bin", 1),
            day1 to EpgPartitionMetadata(day1, "epg_day_gen-1_$day1.bin", 1),
            day2 to EpgPartitionMetadata(day2, "epg_day_gen-1_$day2.bin", 1)
        )
    )

    @Test
    fun pinsTodayPartitionWhenGuideIsSet() = runBlocking {
        val repo = DefaultEpgRepository(epgDir = tempFolder.root)
        val guide = EpgGuide(
            channels = mapOf("RTP1.pt" to channel),
            programmes = mapOf("RTP1.pt" to listOf(programme1)),
            manifest = manifest,
            epochDay = day0
        )

        repo.setGuide(guide)

        assertEquals(manifest, repo.manifest.value)
        assertNotNull(repo.today.value)
        assertEquals(day0, repo.today.value?.epochDay)
        assertEquals(day0, repo.cachedDay(day0)?.epochDay)
        assertNull(repo.cachedDay(day1))
    }

    @Test
    fun loadsAndRetainsConfiguredDayPartitions() = runBlocking {
        val epgDir = tempFolder.root
        // Write day1 partition file
        val day1Partition = EpgDay(day1, mapOf("RTP1.pt" to listOf(programme2)))
        val partitionDay1File = File(epgDir, "epg_day_gen-1_$day1.bin")
        partitionDay1File.outputStream().use { stream ->
            EpgCacheCodec.writeDayPartition(stream, "gen-1", day1Partition)
        }

        // Write day2 partition file
        val day2Partition = EpgDay(day2, mapOf("RTP1.pt" to listOf(programme1)))
        val partitionDay2File = File(epgDir, "epg_day_gen-1_$day2.bin")
        partitionDay2File.outputStream().use { stream ->
            EpgCacheCodec.writeDayPartition(stream, "gen-1", day2Partition)
        }

        val repo = DefaultEpgRepository(epgDir = epgDir)
        val guide = EpgGuide(
            channels = mapOf("RTP1.pt" to channel),
            programmes = mapOf("RTP1.pt" to listOf(programme1)),
            manifest = manifest,
            epochDay = day0
        )
        repo.setGuide(guide)

        // Load day1
        val loadedDay1 = repo.loadDay(day1)
        assertNotNull(loadedDay1)
        assertEquals("Telejornal Amanhã", loadedDay1?.programmes?.get("RTP1.pt")?.first()?.title)
        assertEquals(day1, repo.cachedDay(day1)?.epochDay)

        // Today remains pinned
        assertEquals(day0, repo.cachedDay(day0)?.epochDay)

        // Load day2; both configured future days remain available for instant switching.
        val loadedDay2 = repo.loadDay(day2)
        assertNotNull(loadedDay2)
        assertEquals(day2, repo.cachedDay(day2)?.epochDay)
        assertEquals(day1, repo.cachedDay(day1)?.epochDay)
        // Today is STILL pinned
        assertEquals(day0, repo.cachedDay(day0)?.epochDay)
    }

    @Test
    fun deduplicatesConcurrentLoadsForSameDay() = runBlocking {
        val epgDir = tempFolder.root
        val day1Partition = EpgDay(day1, mapOf("RTP1.pt" to listOf(programme2)))
        val partitionDay1File = File(epgDir, "epg_day_gen-1_$day1.bin")
        partitionDay1File.outputStream().use { stream ->
            EpgCacheCodec.writeDayPartition(stream, "gen-1", day1Partition)
        }

        val repo = DefaultEpgRepository(epgDir = epgDir)
        val guide = EpgGuide(
            channels = mapOf("RTP1.pt" to channel),
            programmes = mapOf("RTP1.pt" to listOf(programme1)),
            manifest = manifest,
            epochDay = day0
        )
        repo.setGuide(guide)

        // Launch 5 concurrent requests for day1
        val results = (1..5).map {
            async { repo.loadDay(day1) }
        }.awaitAll()

        assertEquals(5, results.size)
        results.forEach {
            assertEquals("Telejornal Amanhã", it?.programmes?.get("RTP1.pt")?.first()?.title)
        }
    }

    @Test
    fun doesNotCorruptTodayWhenGuideHasYesterdayProgrammes() = runBlocking {
        val yesterday = currentDay - 1
        val dynamicManifest = EpgManifest(
            generationId = "gen-dynamic",
            sourceFingerprint = "fp",
            timezoneId = "UTC",
            loadedDays = 3,
            todayEpochDay = yesterday, // Manifest was saved yesterday
            channels = mapOf("RTP1.pt" to channel),
            partitions = mapOf(
                yesterday to EpgPartitionMetadata(yesterday, "epg_day_gen-dynamic_$yesterday.bin", 1),
                currentDay to EpgPartitionMetadata(currentDay, "epg_day_gen-dynamic_$currentDay.bin", 1)
            )
        )
        // Write today's partition file to disk
        val todayPartition = EpgDay(currentDay, mapOf("RTP1.pt" to listOf(programme2)))
        val todayFile = File(tempFolder.root, "epg_day_gen-dynamic_$currentDay.bin")
        todayFile.outputStream().use { stream ->
            EpgCacheCodec.writeDayPartition(stream, "gen-dynamic", todayPartition)
        }

        val repo = DefaultEpgRepository(epgDir = tempFolder.root)
        val yesterdayGuide = EpgGuide(
            channels = mapOf("RTP1.pt" to channel),
            programmes = mapOf("RTP1.pt" to listOf(programme1)), // yesterday's programmes
            manifest = dynamicManifest,
            epochDay = yesterday
        )
        repo.setGuide(yesterdayGuide)

        // Guide had yesterday's programmes, so today must NOT be stamped with yesterday's programmes
        assertNull(repo.today.value)
        // Loading today loads today's true partition from disk
        val loadedToday = repo.loadDay(currentDay)
        assertNotNull(loadedToday)
        assertEquals("Telejornal Amanhã", loadedToday?.programmes?.get("RTP1.pt")?.first()?.title)
        assertEquals(currentDay, repo.today.value?.epochDay)
    }

    @Test
    fun returnsNullWhenManifestDoesNotContainToday() = runBlocking {
        val twoDaysAgo = currentDay - 2
        val yesterday = currentDay - 1
        val expiredManifest = EpgManifest(
            generationId = "gen-expired",
            sourceFingerprint = "fp",
            timezoneId = "UTC",
            loadedDays = 2,
            todayEpochDay = twoDaysAgo,
            channels = mapOf("RTP1.pt" to channel),
            partitions = mapOf(
                twoDaysAgo to EpgPartitionMetadata(twoDaysAgo, "epg_day_gen-expired_$twoDaysAgo.bin", 1),
                yesterday to EpgPartitionMetadata(yesterday, "epg_day_gen-expired_$yesterday.bin", 1)
            )
        )
        val repo = DefaultEpgRepository(epgDir = tempFolder.root)
        val guide = EpgGuide(
            channels = mapOf("RTP1.pt" to channel),
            programmes = mapOf("RTP1.pt" to listOf(programme1)),
            manifest = expiredManifest,
            epochDay = twoDaysAgo
        )
        repo.setGuide(guide)

        assertNull(repo.today.value)
        assertNull(repo.loadDay(currentDay))
    }
}

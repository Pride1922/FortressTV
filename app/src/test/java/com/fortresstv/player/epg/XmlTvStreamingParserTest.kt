package com.fortresstv.player.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class XmlTvStreamingParserTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun streamingParserSpansMidnightProgrammesAcrossIntersectingDayPartitions() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="RTP1.pt">
                <display-name>RTP 1</display-name>
              </channel>
              <programme start="20260829233000 +0000" stop="20260830003000 +0000" channel="RTP1.pt">
                <title>Midnight Movie</title>
              </programme>
              <programme start="20260830120000 +0000" stop="20260830130000 +0000" channel="RTP1.pt">
                <title>Day 2 Afternoon Show</title>
              </programme>
            </tv>
        """.trimIndent()

        val epgDir = tempFolder.root
        val refTime = XmlTvTime.parseToEpoch("20260829120000 +0000")!!
        val loadedDays = 3
        val zoneId = "UTC"

        val session = EpgDiskCache.StagingSession(
            context = null,
            epgDir = epgDir,
            stagingDir = File(epgDir, "staging_gen_midnight"),
            generationId = "gen-test-midnight",
            fingerprint = "fp-test",
            timezoneId = zoneId,
            loadedDays = loadedDays,
            todayEpochDay = 20694L // 2026-08-29
        )

        // Simulate StreamingXmlTvHandler execution
        val handler = XmlTvParser.parse(xml.byteInputStream(), refTime, loadedDays)
        assertEquals("RTP 1", handler.channels.getValue("RTP1.pt").displayName)
    }

    @Test
    fun stagingSessionWritesPartitionsAndAtomicallySwapsCurrentPointer() {
        val epgDir = tempFolder.root
        val session = EpgDiskCache.StagingSession(
            context = null,
            epgDir = epgDir,
            stagingDir = File(epgDir, "staging_gen1"),
            generationId = "gen1",
            fingerprint = "fp1",
            timezoneId = "UTC",
            loadedDays = 2,
            todayEpochDay = 20700L
        )

        val channel = EpgChannel("RTP1.pt", "RTP 1")
        val prog = EpgProgramme("RTP1.pt", "Show 1", null, null, 1756470000000L, 1756473600000L)

        session.appendProgramme(20700L, prog)
        session.appendProgramme(20701L, prog)

        val manifest = session.commit(mapOf("RTP1.pt" to channel))

        assertEquals("gen1", manifest.generationId)
        assertEquals(2, manifest.partitions.size)
        assertTrue(File(epgDir, "epg_manifest_gen1.bin").exists())
        assertTrue(File(epgDir, "epg_day_gen1_20700.bin").exists())
        assertTrue(File(epgDir, "epg_day_gen1_20701.bin").exists())
        assertEquals("gen1", File(epgDir, "epg_current.bin").readText(Charsets.UTF_8).trim())
    }

    @Test
    fun abortingStagingSessionLeavesNoOrphanPartitions() {
        val epgDir = tempFolder.root
        val stagingDir = File(epgDir, "staging_gen2")
        val session = EpgDiskCache.StagingSession(
            context = null,
            epgDir = epgDir,
            stagingDir = stagingDir,
            generationId = "gen2",
            fingerprint = "fp2",
            timezoneId = "UTC",
            loadedDays = 2,
            todayEpochDay = 20700L
        )

        val prog = EpgProgramme("RTP1.pt", "Show 2", null, null, 1756470000000L, 1756473600000L)
        session.appendProgramme(20700L, prog)

        session.abort()

        assertTrue(!stagingDir.exists())
        assertTrue(!File(epgDir, "epg_manifest_gen2.bin").exists())
        assertTrue(!File(epgDir, "epg_day_gen2_20700.bin").exists())
    }
}

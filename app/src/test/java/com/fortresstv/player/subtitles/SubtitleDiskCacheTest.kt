package com.fortresstv.player.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream

class SubtitleDiskCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var cache: SubtitleDiskCache

    @Before
    fun setUp() {
        val baseDir = tempFolder.newFolder("subtitles_test")
        cache = SubtitleDiskCache(baseDir)
    }

    @Test
    fun saveAndRetrieveSubtitleString() {
        val key = "source1_series_123_s1e2_888"
        val srtContent = "1\n00:00:01,000 --> 00:00:03,000\nOlá Mundo\n"

        assertFalse(cache.hasSubtitle(key))

        val file = cache.saveSubtitle(key, srtContent)
        assertTrue(file.exists())
        assertTrue(cache.hasSubtitle(key))
        assertEquals(srtContent, file.readText(Charsets.UTF_8))
        assertEquals(file.absolutePath, cache.subtitleFile(key).absolutePath)
    }

    @Test
    fun saveSubtitleFromInputStream() {
        val key = "test_stream_key"
        val srtContent = "1\n00:00:05,000 --> 00:00:07,000\nLegenda de teste\n"
        val inputStream = ByteArrayInputStream(srtContent.toByteArray(Charsets.UTF_8))

        val file = cache.saveSubtitle(key, inputStream)
        assertTrue(file.exists())
        assertTrue(cache.hasSubtitle(key))
        assertEquals(srtContent, file.readText(Charsets.UTF_8))
    }

    @Test
    fun sanitizesCacheKeyCharacters() {
        val dangerousKey = "../../dangerous/path?name=bad*key"
        val file = cache.subtitleFile(dangerousKey)
        // Check that path does not escape the cache directory
        assertTrue(file.name.endsWith(".srt"))
        assertFalse(file.name.contains("/"))
        assertFalse(file.name.contains(".."))
    }

    @Test
    fun clearCacheRemovesAllFiles() {
        cache.saveSubtitle("item1", "sub 1")
        cache.saveSubtitle("item2", "sub 2")
        assertTrue(cache.hasSubtitle("item1"))
        assertTrue(cache.hasSubtitle("item2"))

        cache.clearCache()
        assertFalse(cache.hasSubtitle("item1"))
        assertFalse(cache.hasSubtitle("item2"))
    }
}

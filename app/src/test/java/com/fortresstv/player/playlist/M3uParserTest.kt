package com.fortresstv.player.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class M3uParserTest {
    @Test
    fun preservesOrderAndMetadata() {
        val playlist = M3uParser.parse(
            """
                #EXTM3U
                #EXTINF:-1 tvg-id="one.id" tvg-name="One" tvg-logo="https://example.test/one.png" group-title="News, local",One HD
                https://example.test/one.m3u8
                #EXTINF:-1 tvg-id="two.id" group-title="Sports",Two
                https://example.test/two.ts
            """.trimIndent()
        )

        assertEquals(listOf("One HD", "Two"), playlist.channels.map { it.name })
        assertEquals(listOf(0, 1), playlist.channels.map { it.position })
        assertEquals("one.id", playlist.channels[0].tvgId)
        assertEquals("https://example.test/one.png", playlist.channels[0].logoUrl)
        assertEquals(listOf("News, local", "Sports"), playlist.groups)
    }

    @Test
    fun parsesCatchupAttributes() {
        val playlist = M3uParser.parse(
            """
                #EXTM3U
                #EXTINF:-1 catchup="default" catchup-days="7",Catchup Channel
                https://example.test/catchup.m3u8
                #EXTINF:-1,Regular Channel
                https://example.test/regular.m3u8
            """.trimIndent()
        )

        assertEquals(true, playlist.channels[0].hasCatchup)
        assertEquals(7, playlist.channels[0].catchupDays)

        assertEquals(false, playlist.channels[1].hasCatchup)
        assertEquals(0, playlist.channels[1].catchupDays)
    }

    @Test
    fun supportsExtGrp() {
        val playlist = M3uParser.parse("#EXTINF:-1,Radio\n#EXTGRP:Audio\nhttps://example.test/radio")

        assertEquals("Audio", playlist.channels.single().group)
    }
}

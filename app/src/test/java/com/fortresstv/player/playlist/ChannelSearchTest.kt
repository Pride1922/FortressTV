package com.fortresstv.player.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelSearchTest {
    private val channels = listOf(
        PlaylistChannel(0, "RTP 1", "https://stream/1", "RTP1.pt", "Rádio Televisão Portuguesa", group = "General"),
        PlaylistChannel(1, "SIC Notícias", "https://stream/2", "SICNoticias.pt", group = "News"),
        PlaylistChannel(2, "SPORT TV 1", "https://stream/3", "SportTV1.pt", group = "Sports"),
        PlaylistChannel(3, "SPORT TV 2", "https://stream/4", "SportTV2.pt", group = "Sports")
    )

    @Test
    fun searchesNameCaseInsensitivelyAndPreservesOrder() {
        assertEquals(listOf(2, 3), channels.searchChannels("sport").map { it.position })
    }

    @Test
    fun searchesTvgIdAndTvgName() {
        assertEquals("SIC Notícias", channels.searchChannels("sicnoticias.pt").single().name)
        assertEquals("RTP 1", channels.searchChannels("televisão").single().name)
    }

    @Test
    fun blankQueryRestoresEntireList() {
        assertEquals(channels, channels.searchChannels("   "))
    }

    @Test
    fun worksWithinPreselectedCategory() {
        val sports = channels.filter { it.group == "Sports" }
        assertEquals(1, sports.searchChannels("TV 2").size)
    }
}

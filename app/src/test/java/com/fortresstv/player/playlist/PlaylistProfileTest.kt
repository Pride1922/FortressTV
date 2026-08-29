package com.fortresstv.player.playlist

import com.fortresstv.player.source.ContentSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistProfileTest {

    @Test
    fun playlistProfileProperties() {
        val profile = PlaylistProfile(
            id = "profile-123",
            name = "Sports IPTV",
            source = ContentSource.RemoteUrl("https://example.com/live.m3u", "Sports IPTV"),
            lastUpdated = 1700000000000L,
            channelCount = 150
        )

        assertEquals("profile-123", profile.id)
        assertEquals("Sports IPTV", profile.name)
        assertEquals(150, profile.channelCount)
        assertTrue(profile.source is ContentSource.RemoteUrl)
    }

    @Test
    fun localFileProfileProperties() {
        val profile = PlaylistProfile(
            id = "file-abc",
            name = "Local List",
            source = ContentSource.LocalFile("content://media/external/file/123", "Local List"),
            lastUpdated = 0L,
            channelCount = 42
        )

        assertEquals("file-abc", profile.id)
        assertEquals("Local List", profile.name)
        assertEquals(42, profile.channelCount)
        assertTrue(profile.source is ContentSource.LocalFile)
    }
}

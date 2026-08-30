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

    @Test
    fun mergesEnabledLiveSourcesInConfiguredOrder() {
        val second = PlaylistProfile("two", "Second", ContentSource.RemoteUrl("https://example.com/2.m3u", "Second"), enabled = true, order = 1)
        val first = PlaylistProfile("one", "First", ContentSource.RemoteUrl("https://example.com/1.m3u", "First"), enabled = true, order = 0)
        val disabled = PlaylistProfile("off", "Off", ContentSource.RemoteUrl("https://example.com/off.m3u", "Off"), enabled = false, order = 2)
        val loaded = mapOf(
            "one" to Playlist(listOf(PlaylistChannel(0, "One A", "https://stream/1"))),
            "two" to Playlist(listOf(PlaylistChannel(0, "Two A", "https://stream/2"))),
            "off" to Playlist(listOf(PlaylistChannel(0, "Hidden", "https://stream/off")))
        )

        val merged = mergeLivePlaylists(listOf(second, disabled, first), loaded)

        assertEquals(listOf("One A", "Two A"), merged.channels.map { it.name })
        assertEquals(listOf("one", "two"), merged.channels.map { it.sourceId })
        assertEquals(listOf(0, 1), merged.channels.map { it.position })
    }

    @Test
    fun excludesSourcesAssignedOnlyToMoviesOrSeries() {
        val live = PlaylistProfile("live", "Live", ContentSource.RemoteUrl("https://example.com/live.m3u", "Live"), role = SourceRole.LIVE_TV)
        val movies = PlaylistProfile("movies", "Movies", ContentSource.RemoteUrl("https://example.com/vod.m3u", "Movies"), role = SourceRole.MOVIES_AND_SERIES)
        val loaded = mapOf(
            "live" to Playlist(listOf(PlaylistChannel(0, "Live channel", "https://stream/live"))),
            "movies" to Playlist(listOf(PlaylistChannel(0, "A movie", "https://stream/movie")))
        )

        val merged = mergeLivePlaylists(listOf(live, movies), loaded)

        assertEquals(listOf("Live channel"), merged.channels.map { it.name })
    }
}

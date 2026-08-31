package com.fortresstv.player.backup

import com.fortresstv.player.library.VodProgressEntry
import com.fortresstv.player.library.VodProgressKind
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.playlist.SourceRole
import com.fortresstv.player.source.ContentSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    @Test
    fun roundTripJsonSerializationPreservesAllFields() {
        val settings = BackupSettings(
            language = "pt",
            playChannelsFullscreen = true,
            autoPictureInPicture = false,
            hideAdultContent = true,
            parentalControlEnabled = true,
            parentalPin = "1234",
            epgSource = ContentSource.RemoteUrl("https://example.com/epg.xml", "My EPG"),
            epgTimeShiftHours = 2,
            activePlaylistId = "profile-1"
        )

        val playlists = listOf(
            PlaylistProfile(
                id = "profile-1",
                name = "Xtream Source",
                source = ContentSource.Xtream(
                    value = "http://iptv.example.com:8080",
                    displayName = "Xtream Source",
                    username = "testuser",
                    password = "secretpassword"
                ),
                lastUpdated = 1700000000L,
                channelCount = 1500,
                enabled = true,
                role = SourceRole.MOVIES_AND_SERIES,
                order = 0
            ),
            PlaylistProfile(
                id = "profile-2",
                name = "Live TV M3U",
                source = ContentSource.RemoteUrl(
                    value = "https://example.com/live.m3u",
                    displayName = "Live TV M3U"
                ),
                lastUpdated = 1700001000L,
                channelCount = 200,
                enabled = true,
                role = SourceRole.LIVE_TV,
                order = 1
            )
        )

        val library = BackupLibrary(
            favoriteChannelIds = setOf("channel:profile-2:101", "channel:profile-2:102"),
            movieWatchlistIds = setOf("movie:profile-1:555"),
            followedSeriesIds = setOf("series:profile-1:777"),
            watchedItemIds = setOf("episode:profile-1:1001"),
            recentChannelIds = listOf("channel:profile-2:101"),
            hiddenCategoriesByProfile = mapOf("profile-1" to setOf("Adult", "News")),
            categoryOrderByProfile = mapOf("profile-1" to listOf("Movies", "Series"))
        )

        val vodProgress = listOf(
            VodProgressEntry(
                key = "movie:profile-1:555",
                kind = VodProgressKind.MOVIE,
                sourceId = "profile-1",
                itemId = "555",
                seriesId = null,
                title = "Inception",
                posterUrl = "https://example.com/poster.jpg",
                containerExtension = "mp4",
                season = null,
                episode = null,
                positionMs = 300000,
                durationMs = 600000,
                updatedAtMs = 1700005000L
            )
        )

        val payload = BackupPayload(
            version = 1,
            createdAt = 1700006000L,
            appVersion = "0.1.9",
            settings = settings,
            playlists = playlists,
            library = library,
            vodProgress = vodProgress
        )

        val json = BackupManager.encodePayloadToJson(payload)
        val decoded = BackupManager.decodePayloadFromJson(json)

        assertEquals(payload.version, decoded.version)
        assertEquals(payload.appVersion, decoded.appVersion)
        assertEquals(payload.settings.language, decoded.settings.language)
        assertEquals(payload.settings.playChannelsFullscreen, decoded.settings.playChannelsFullscreen)
        assertEquals(payload.settings.autoPictureInPicture, decoded.settings.autoPictureInPicture)
        assertEquals(payload.settings.hideAdultContent, decoded.settings.hideAdultContent)
        assertEquals(payload.settings.parentalControlEnabled, decoded.settings.parentalControlEnabled)
        assertEquals(payload.settings.parentalPin, decoded.settings.parentalPin)
        assertEquals(payload.settings.epgTimeShiftHours, decoded.settings.epgTimeShiftHours)
        assertEquals(payload.settings.activePlaylistId, decoded.settings.activePlaylistId)

        assertEquals(2, decoded.playlists.size)
        val p1 = decoded.playlists[0]
        assertEquals("profile-1", p1.id)
        assertTrue(p1.source is ContentSource.Xtream)
        val p1Source = p1.source as ContentSource.Xtream
        assertEquals("testuser", p1Source.username)
        assertEquals("secretpassword", p1Source.password)

        assertEquals(2, decoded.library.favoriteChannelIds.size)
        assertEquals(setOf("channel:profile-2:101", "channel:profile-2:102"), decoded.library.favoriteChannelIds)
        assertEquals(setOf("movie:profile-1:555"), decoded.library.movieWatchlistIds)
        assertEquals(setOf("series:profile-1:777"), decoded.library.followedSeriesIds)
        assertEquals(setOf("episode:profile-1:1001"), decoded.library.watchedItemIds)
        assertEquals(setOf("Adult", "News"), decoded.library.hiddenCategoriesByProfile["profile-1"])
        assertEquals(listOf("Movies", "Series"), decoded.library.categoryOrderByProfile["profile-1"])

        assertEquals(1, decoded.vodProgress.size)
        val v = decoded.vodProgress[0]
        assertEquals("movie:profile-1:555", v.key)
        assertEquals(VodProgressKind.MOVIE, v.kind)
        assertEquals("Inception", v.title)
        assertEquals(300000L, v.positionMs)
    }
}

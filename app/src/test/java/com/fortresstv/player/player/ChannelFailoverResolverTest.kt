package com.fortresstv.player.player

import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.telemetry.ChannelHealthRepository
import com.fortresstv.player.telemetry.ChannelHealthSnapshot
import com.fortresstv.player.telemetry.ConnectionHealthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelFailoverResolverTest {

    @Test
    fun `normalizeChannelName strips iptv prefixes resolutions and framerates`() {
        assertEquals("rtp1", ChannelFailoverResolver.normalizeChannelName("PT: RTP 1 HD (50fps)"))
        assertEquals("rtp1", ChannelFailoverResolver.normalizeChannelName("PT | RTP 1 FHD"))
        assertEquals("rtp1", ChannelFailoverResolver.normalizeChannelName("RTP 1 4K HEVC"))
        assertEquals("sporttv1", ChannelFailoverResolver.normalizeChannelName("PT: SPORT TV 1 FHD [RAW]"))
        assertEquals("sporttv1", ChannelFailoverResolver.normalizeChannelName("PT-SPORT TV 1 HD"))
        assertEquals("cmtv", ChannelFailoverResolver.normalizeChannelName("CMTV 1080p"))
    }

    @Test
    fun `isCandidate matches channels by tvgId regardless of display name or casing`() {
        val target = PlaylistChannel(
            position = 0,
            name = "RTP 1 1080p",
            streamUrl = "https://provider1.com/live/stream1.m3u8",
            tvgId = "rtp1.pt",
            sourceId = "source-1"
        )
        val candidate = PlaylistChannel(
            position = 1,
            name = "RTP 1 Portugal HD",
            streamUrl = "https://provider2.com/live/backup1.m3u8",
            tvgId = "RTP1.PT",
            sourceId = "source-2"
        )
        assertTrue(ChannelFailoverResolver.isCandidate(target, candidate))
    }

    @Test
    fun `isCandidate matches channels by normalized name when tvgId is missing or empty`() {
        val target = PlaylistChannel(
            position = 0,
            name = "PT: SIC NOTICIAS FHD",
            streamUrl = "https://provider1.com/live/sicnoticias.m3u8",
            tvgId = null,
            sourceId = "source-1"
        )
        val candidate = PlaylistChannel(
            position = 1,
            name = "SIC NOTICIAS HD (50fps)",
            streamUrl = "https://provider2.com/live/sicnoticias_backup.m3u8",
            tvgId = "",
            sourceId = "source-2"
        )
        assertTrue(ChannelFailoverResolver.isCandidate(target, candidate))
    }

    @Test
    fun `isCandidate rejects candidate with exact same streamUrl to prevent identical retry loops`() {
        val target = PlaylistChannel(
            position = 0,
            name = "RTP 1",
            streamUrl = "https://provider1.com/live/rtp1.m3u8",
            tvgId = "rtp1.pt"
        )
        val identicalCandidate = PlaylistChannel(
            position = 1,
            name = "RTP 1 HD",
            streamUrl = "https://provider1.com/live/rtp1.m3u8",
            tvgId = "rtp1.pt"
        )
        assertFalse(ChannelFailoverResolver.isCandidate(target, identicalCandidate))
    }

    @Test
    fun `isCandidate rejects completely different channels`() {
        val target = PlaylistChannel(
            position = 0,
            name = "RTP 1",
            streamUrl = "https://provider1.com/live/rtp1.m3u8",
            tvgId = "rtp1.pt"
        )
        val different = PlaylistChannel(
            position = 1,
            name = "RTP 2",
            streamUrl = "https://provider1.com/live/rtp2.m3u8",
            tvgId = "rtp2.pt"
        )
        assertFalse(ChannelFailoverResolver.isCandidate(target, different))
    }

    @Test
    fun `findFailoverCandidates filters out already attempted urls`() {
        val failed = PlaylistChannel(
            position = 0,
            name = "RTP 1",
            streamUrl = "https://provider1.com/stream1.m3u8",
            tvgId = "rtp1.pt"
        )
        val candidate1 = PlaylistChannel(
            position = 1,
            name = "RTP 1 FHD",
            streamUrl = "https://provider2.com/stream2.m3u8",
            tvgId = "rtp1.pt"
        )
        val candidate2 = PlaylistChannel(
            position = 2,
            name = "RTP 1 4K",
            streamUrl = "https://provider3.com/stream3.m3u8",
            tvgId = "rtp1.pt"
        )

        val attempted = setOf(failed.streamUrl, candidate1.streamUrl)
        val candidates = ChannelFailoverResolver.findFailoverCandidates(
            failedChannel = failed,
            allChannels = listOf(failed, candidate1, candidate2),
            attemptedUrls = attempted
        )

        assertEquals(1, candidates.size)
        assertEquals(candidate2.streamUrl, candidates.first().streamUrl)
    }

    @Test
    fun `findFailoverCandidates ranks candidates with EXCELLENT health over POOR health`() {
        val failed = PlaylistChannel(
            position = 0,
            name = "RTP 1",
            streamUrl = "https://provider1.com/stream1.m3u8",
            tvgId = "rtp1.pt",
            sourceId = "source-1"
        )
        val poorCandidate = PlaylistChannel(
            position = 1,
            name = "RTP 1 FHD",
            streamUrl = "https://provider2.com/stream_slow.m3u8",
            tvgId = "rtp1.pt",
            sourceId = "source-2",
            sourceItemId = "source-2:rtp1"
        )
        val excellentCandidate = PlaylistChannel(
            position = 2,
            name = "RTP 1 4K",
            streamUrl = "https://provider3.com/stream_fast.m3u8",
            tvgId = "rtp1.pt",
            sourceId = "source-3",
            sourceItemId = "source-3:rtp1"
        )

        val fakePrefs = object : android.content.SharedPreferences {
            override fun getAll(): MutableMap<String, *> = mutableMapOf<String, Any>()
            override fun getString(key: String?, defValue: String?): String? = null
            override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
            override fun getInt(key: String?, defValue: Int): Int = defValue
            override fun getLong(key: String?, defValue: Long): Long = defValue
            override fun getFloat(key: String?, defValue: Float): Float = defValue
            override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue
            override fun contains(key: String?): Boolean = false
            override fun edit(): android.content.SharedPreferences.Editor = error("Stub")
            override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
            override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
        }

        val repo = ChannelHealthRepository(prefsProvider = { fakePrefs })
        repo.setAggregateSnapshots(
            mapOf(
                "source-2:rtp1" to ChannelHealthSnapshot.fromMetrics("source-2:rtp1", 4200L, 2, 4000L, null, "PT"),
                "source-3:rtp1" to ChannelHealthSnapshot.fromMetrics("source-3:rtp1", 750L, 0, 0L, null, "PT")
            )
        )

        val results = ChannelFailoverResolver.findFailoverCandidates(
            failedChannel = failed,
            allChannels = listOf(failed, poorCandidate, excellentCandidate),
            attemptedUrls = setOf(failed.streamUrl),
            healthRepo = repo
        )

        assertEquals(2, results.size)
        assertEquals("RTP 1 4K", results[0].name)
        assertEquals("RTP 1 FHD", results[1].name)
    }
}

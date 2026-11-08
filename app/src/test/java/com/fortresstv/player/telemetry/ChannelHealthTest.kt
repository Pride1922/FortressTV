package com.fortresstv.player.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelHealthTest {

    @Test
    fun `measuring state when live testing and startupMs is zero`() {
        val snapshot = ChannelHealthSnapshot.fromMetrics(
            channelId = "channel-1",
            startupMs = 0L,
            stallCount = 0,
            totalStallMs = 0L,
            errorCode = null,
            region = "PT",
            isLiveTesting = true
        )
        assertEquals(ConnectionHealthState.MEASURING, snapshot.state)
        assertEquals("channel-1", snapshot.channelId)
    }

    @Test
    fun `excellent state when startup is fast and zero stalls`() {
        val snapshot = ChannelHealthSnapshot.fromMetrics(
            channelId = "channel-1",
            startupMs = 850L,
            stallCount = 0,
            totalStallMs = 0L,
            errorCode = null,
            region = "PT"
        )
        assertEquals(ConnectionHealthState.EXCELLENT, snapshot.state)
        assertEquals(850L, snapshot.startupMs)
        assertEquals(0, snapshot.stallCount)
    }

    @Test
    fun `moderate state when single stall or elevated latency occurs`() {
        val snapshot = ChannelHealthSnapshot.fromMetrics(
            channelId = "channel-1",
            startupMs = 2500L,
            stallCount = 1,
            totalStallMs = 800L,
            errorCode = null,
            region = "PT"
        )
        assertEquals(ConnectionHealthState.MODERATE, snapshot.state)
    }

    @Test
    fun `poor state when frequent stalls or high latency occurs`() {
        val snapshot = ChannelHealthSnapshot.fromMetrics(
            channelId = "channel-1",
            startupMs = 5000L,
            stallCount = 4,
            totalStallMs = 8000L,
            errorCode = null,
            region = "PT"
        )
        assertEquals(ConnectionHealthState.POOR, snapshot.state)
    }

    @Test
    fun `poor state immediately upon player error`() {
        val snapshot = ChannelHealthSnapshot.fromMetrics(
            channelId = "channel-1",
            startupMs = 500L,
            stallCount = 0,
            totalStallMs = 0L,
            errorCode = "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED",
            region = "PT"
        )
        assertEquals(ConnectionHealthState.POOR, snapshot.state)
    }

    @Test
    fun `fromSummary converts PlaybackHealthSummary correctly`() {
        val summary = PlaybackHealthSummary(
            sessionId = "test-uuid",
            householdId = "hh-1",
            profileId = "prof-1",
            channelId = "rtp1",
            streamKind = StreamKindWire.LIVE,
            providerId = "provider-1",
            startedAtEpochMs = 1000L,
            endedAtEpochMs = 31000L,
            startupMs = 740L,
            stallCount = 0,
            totalStallMs = 0L,
            avgBitrateKbps = 5200,
            droppedFramesPct = 0f,
            playerErrorCode = null,
            region = "PT",
            completed = true
        )
        val snapshot = ChannelHealthSnapshot.fromSummary(summary)
        assertEquals("rtp1", snapshot.channelId)
        assertEquals(ConnectionHealthState.EXCELLENT, snapshot.state)
        assertEquals(740L, snapshot.startupMs)
    }

    @Test
    fun `channel health repository prioritizes local observations over aggregate crowdsourced scores`() {
        var storedJson: String? = null
        val fakePrefs = object : android.content.SharedPreferences {
            override fun getAll(): MutableMap<String, *> = mutableMapOf<String, Any>()
            override fun getString(key: String?, defValue: String?): String? = storedJson ?: defValue
            override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
            override fun getInt(key: String?, defValue: Int): Int = defValue
            override fun getLong(key: String?, defValue: Long): Long = defValue
            override fun getFloat(key: String?, defValue: Float): Float = defValue
            override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue
            override fun contains(key: String?): Boolean = key == "channel_snapshots" && storedJson != null
            override fun edit(): android.content.SharedPreferences.Editor = object : android.content.SharedPreferences.Editor {
                override fun putString(key: String?, value: String?): android.content.SharedPreferences.Editor { storedJson = value; return this }
                override fun putStringSet(key: String?, values: MutableSet<String>?): android.content.SharedPreferences.Editor = this
                override fun putInt(key: String?, value: Int): android.content.SharedPreferences.Editor = this
                override fun putLong(key: String?, value: Long): android.content.SharedPreferences.Editor = this
                override fun putFloat(key: String?, value: Float): android.content.SharedPreferences.Editor = this
                override fun putBoolean(key: String?, value: Boolean): android.content.SharedPreferences.Editor = this
                override fun remove(key: String?): android.content.SharedPreferences.Editor = this
                override fun clear(): android.content.SharedPreferences.Editor = this
                override fun commit(): Boolean = true
                override fun apply() {}
            }
            override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
            override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
        }

        val repo = ChannelHealthRepository(prefsProvider = { fakePrefs })

        // 1. Cloud aggregate says channel-1 is POOR and channel-2 is EXCELLENT
        val aggregates = mapOf(
            "channel-1" to ChannelHealthSnapshot.fromMetrics("channel-1", 4000L, 3, 5000L, null, "PT"),
            "channel-2" to ChannelHealthSnapshot.fromMetrics("channel-2", 800L, 0, 0L, null, "PT")
        )
        repo.setAggregateSnapshots(aggregates)

        // channel-2 (not watched locally) falls back to aggregate (EXCELLENT)
        assertEquals(ConnectionHealthState.EXCELLENT, repo.getHealth("channel-2")?.state)

        // 2. But on this specific device, user watches channel-1 and it's actually EXCELLENT (low startup, 0 stalls)
        repo.recordActiveMetrics("channel-1", startupMs = 700L, stallCount = 0, totalStallMs = 0L, errorCode = null, region = "PT")

        // Local observation on this device MUST override the aggregate!
        assertEquals(ConnectionHealthState.EXCELLENT, repo.getHealth("channel-1")?.state)
        assertEquals(700L, repo.getHealth("channel-1")?.startupMs)
    }
}

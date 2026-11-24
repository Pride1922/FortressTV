package com.fortresstv.player.library

import com.fortresstv.player.profile.ProfileTransport
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ChannelQualitySyncTest {
    private val household = "00000000-0000-0000-0000-000000000001"
    private val profile = "00000000-0000-0000-0000-000000000002"
    private fun row(variant: String, width: Int, height: Int, observed: String) = JSONObject()
        .put("household_id", household).put("profile_id", profile).put("stream_variant_id", variant)
        .put("video_width", width).put("video_height", height).put("quality_observed_at", observed)

    @Test
    fun fetchIsBoundedProfileScopedAndUsesNewestValidSizePerVariant() {
        val id = "a".repeat(64)
        val response = JSONArray().put(row(id, 3840, 2160, "2026-11-24T10:00:00Z"))
            .put(row(id, 1920, 1080, "2026-11-24T11:00:00Z"))
            .put(row("b".repeat(64), 3840, 2160, "2026-11-24T11:00:00Z").put("profile_id", household))
            .put(row("c".repeat(64), 3840, 2160, "2026-11-24T11:00:00Z").put("household_id", profile))
            .put(row("d".repeat(64), 0, 1080, "2026-11-24T11:00:00Z"))
        var path = ""
        val transport = object : ProfileTransport {
            override fun request(method: String, requestPath: String, body: JSONObject?, accessToken: String?, headers: Map<String, String>?): String {
                assertEquals("GET", method)
                path = requestPath
                return response.toString()
            }
        }
        val actual = SupabaseSyncApi(transport).fetchChannelQualities(household, profile, "synthetic-token")
        assertEquals(1, actual.size)
        assertEquals("FHD", actual[id]!!.tier)
        assertTrue(path.contains("household_id=eq.$household"))
        assertTrue(path.contains("profile_id=eq.$profile"))
        assertTrue(path.contains("limit=500"))
        assertTrue(path.contains("stream_kind=eq.live"))
    }

    @Test
    fun unavailableCloudIsAnEmptyUpdate() {
        val transport = object : ProfileTransport {
            override fun request(method: String, path: String, body: JSONObject?, accessToken: String?, headers: Map<String, String>?): String =
                throw java.io.IOException("Offline")
        }
        assertTrue(SupabaseSyncApi(transport).fetchChannelQualities(household, profile, "synthetic-token").isEmpty())
    }
}

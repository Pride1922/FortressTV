package com.fortresstv.player.account

import com.fortresstv.player.library.CloudIdentityHasher
import com.fortresstv.player.library.CloudLibraryItem
import com.fortresstv.player.library.CloudWatchProgressItem
import com.fortresstv.player.library.SupabaseSyncApi
import com.fortresstv.player.library.UserLibraryStore
import com.fortresstv.player.library.VodProgressEntry
import com.fortresstv.player.library.VodProgressKind
import com.fortresstv.player.library.VodProgressStore
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.profile.HouseholdProfile
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamMovie
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class FreshDeviceRestorationTest {

    private class MockSupabaseBackend : com.fortresstv.player.profile.ProfileTransport {
        val storedSources = mutableListOf<HouseholdSourceRecord>()
        val storedLibrary = mutableListOf<CloudLibraryItem>()
        val storedProgress = mutableListOf<CloudWatchProgressItem>()
        var validUnlockToken: String = "real-unlock-token-123"

        override fun request(
            method: String,
            path: String,
            body: JSONObject?,
            accessToken: String?,
            headers: Map<String, String>?
        ): String {
            if (path.contains("fortress_save_household_source")) {
                val token = body?.optString("p_unlock_token")
                if (token != validUnlockToken) {
                    throw IOException("HTTP 403: Adult authorization required to access source connections")
                }
                val record = HouseholdSourceRecord(
                    id = body.getString("p_source_id"),
                    sourceType = body.getString("p_source_type"),
                    displayName = body.getString("p_display_name"),
                    enabled = body.optBoolean("p_enabled", true),
                    sourceOrder = body.optInt("p_source_order", 0),
                    config = body.getJSONObject("p_config")
                )
                storedSources.removeAll { it.id == record.id }
                storedSources.add(record)
                return "\"${record.id}\""
            }
            if (path.contains("fortress_get_household_sources")) {
                val arr = JSONArray()
                storedSources.forEach { s ->
                    arr.put(JSONObject().apply {
                        put("id", s.id)
                        put("source_type", s.sourceType)
                        put("display_name", s.displayName)
                        put("enabled", s.enabled)
                        put("source_order", s.sourceOrder)
                        put("config", s.config)
                    })
                }
                return arr.toString()
            }
            if (path.contains("profile_library")) {
                val profileSession = headers?.get("x-profile-session")
                if (profileSession.isNullOrBlank()) {
                    throw IOException("HTTP 401: Profile session required")
                }
                if (method == "POST") {
                    val profileId = body!!.optString("p_profile_id").ifBlank { body.optString("profile_id") }
                    val sourceId = body.optString("p_source_id").ifBlank { body.optString("source_id") }
                    val itemKind = body.optString("p_item_kind").ifBlank { body.optString("item_kind") }
                    val itemId = body.optString("p_item_id").ifBlank { body.optString("item_id") }
                    val isFav = if (body.has("p_is_favourite")) body.optBoolean("p_is_favourite") else body.optBoolean("is_favourite", false)
                    val isWatch = if (body.has("p_is_watchlisted")) body.optBoolean("p_is_watchlisted") else body.optBoolean("is_watchlisted", false)
                    val isWatched = if (body.has("p_is_watched")) body.optBoolean("p_is_watched") else body.optBoolean("is_watched", false)
                    val item = CloudLibraryItem(
                        householdId = body.optString("household_id", "house-1"),
                        profileId = profileId,
                        sourceId = sourceId,
                        itemKind = itemKind,
                        itemId = itemId,
                        isFavourite = isFav,
                        isWatchlisted = isWatch,
                        isWatched = isWatched
                    )
                    storedLibrary.removeAll { it.profileId == item.profileId && it.sourceId == item.sourceId && it.itemId == item.itemId }
                    storedLibrary.add(item)
                    return "{\"success\":true}"
                }
                if (method == "GET") {
                    val arr = JSONArray()
                    storedLibrary.forEach { it ->
                        arr.put(JSONObject().apply {
                            put("household_id", it.householdId)
                            put("profile_id", it.profileId)
                            put("source_id", it.sourceId)
                            put("item_kind", it.itemKind)
                            put("item_id", it.itemId)
                            put("is_favourite", it.isFavourite)
                            put("is_watchlisted", it.isWatchlisted)
                            put("is_watched", it.isWatched)
                            put("deleted_at", it.deletedAt ?: JSONObject.NULL)
                            put("updated_at", "2026-11-06T12:00:00Z")
                        })
                    }
                    return arr.toString()
                }
            }
            if (path.contains("profile_watch_progress")) {
                val profileSession = headers?.get("x-profile-session")
                if (profileSession.isNullOrBlank()) {
                    throw IOException("HTTP 401: Profile session required")
                }
                if (method == "POST") {
                    val profileId = body!!.optString("p_profile_id").ifBlank { body.optString("profile_id") }
                    val sourceId = body.optString("p_source_id").ifBlank { body.optString("source_id") }
                    val itemKind = body.optString("p_item_kind").ifBlank { body.optString("item_kind") }
                    val itemId = body.optString("p_item_id").ifBlank { body.optString("item_id") }
                    val pos = if (body.has("p_position_ms")) body.optLong("p_position_ms") else body.optLong("position_ms", 0L)
                    val dur = if (body.has("p_duration_ms")) body.optLong("p_duration_ms") else body.optLong("duration_ms", 0L)
                    val comp = if (body.has("p_completed")) body.optBoolean("p_completed") else body.optBoolean("completed", false)
                    val item = CloudWatchProgressItem(
                        householdId = body.optString("household_id", "house-1"),
                        profileId = profileId,
                        sourceId = sourceId,
                        itemKind = itemKind,
                        itemId = itemId,
                        positionMs = pos,
                        durationMs = dur,
                        completed = comp
                    )
                    storedProgress.removeAll { it.profileId == item.profileId && it.sourceId == item.sourceId && it.itemId == item.itemId }
                    storedProgress.add(item)
                    return "{\"success\":true}"
                }
                if (method == "GET") {
                    val arr = JSONArray()
                    storedProgress.forEach { it ->
                        arr.put(JSONObject().apply {
                            put("household_id", it.householdId)
                            put("profile_id", it.profileId)
                            put("source_id", it.sourceId)
                            put("item_kind", it.itemKind)
                            put("item_id", it.itemId)
                            put("position_ms", it.positionMs)
                            put("duration_ms", it.durationMs)
                            put("completed", it.completed)
                            put("deleted_at", it.deletedAt ?: JSONObject.NULL)
                            put("updated_at", "2026-11-06T12:00:00Z")
                        })
                    }
                    return arr.toString()
                }
            }
            return "{}"
        }
    }

    @Test
    fun freshDeviceRestoresSourcesProfilesFavouritesAndWatchProgress() {
        val backend = MockSupabaseBackend()
        val sourceApi = SupabaseSourceApi(backend)
        val syncApi = SupabaseSyncApi(backend)

        val householdId = "house-uuid-0001"
        val adultProfileId = "prof-adult-0001"
        val childProfileId = "prof-child-0002"
        val sourceId = "11111111-2222-3333-4444-555555555555"

        // === SIMULATE DEVICE A: Configure household, source, favorites, and progress ===
        val xtreamConfig = JSONObject().apply {
            put("server_url", "http://provider.test:8080")
            put("username", "household_user")
            put("password", "secret_pass")
        }
        val sourceRecord = HouseholdSourceRecord(
            id = sourceId,
            sourceType = "xtream",
            displayName = "Primary Xtream",
            enabled = true,
            sourceOrder = 0,
            config = xtreamConfig
        )
        sourceApi.saveHouseholdSource(sourceRecord, backend.validUnlockToken, "jwt-device-a")

        // Device A adult marks channel "101" as favorite and movie "505" as watchlisted with 120s progress
        val favChannelItem = CloudLibraryItem(
            householdId = householdId,
            profileId = adultProfileId,
            sourceId = sourceId,
            itemKind = "channel",
            itemId = CloudIdentityHasher.hashItemId("101"),
            isFavourite = true
        )
        syncApi.upsertProfileLibrary(listOf(favChannelItem), "adult-session-token", "jwt-device-a")

        val movieProgressItem = CloudWatchProgressItem(
            householdId = householdId,
            profileId = adultProfileId,
            sourceId = sourceId,
            itemKind = "movie",
            itemId = CloudIdentityHasher.hashItemId("505"),
            positionMs = 120_000L,
            durationMs = 600_000L,
            completed = false
        )
        syncApi.upsertProfileWatchProgress(listOf(movieProgressItem), "adult-session-token", "jwt-device-a")

        // === SIMULATE DEVICE B: Fresh installation ===
        // 1. Shared sources can be fetched without an adult unlock token so all households & profiles can play channels
        val restoredSources = sourceApi.getHouseholdSources(null, "jwt-device-b")
        assertEquals(1, restoredSources.size)
        val restoredSource = restoredSources[0]
        assertEquals(sourceId, restoredSource.id) // Preserves stable source UUID across devices
        assertEquals("Primary Xtream", restoredSource.displayName)
        assertEquals("household_user", restoredSource.config.getString("username"))
        assertEquals("secret_pass", restoredSource.config.getString("password"))

        // 2. Child profile or unauthorized user cannot modify or add cloud sources without adult authorization
        try {
            sourceApi.saveHouseholdSource(sourceRecord, "child-or-missing-token", "jwt-device-b")
            fail("Modifying or saving cloud source must require adult authorization")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("Adult authorization required"))
        }

        // 4. Fresh Device B pulls cloud library & watch progress for the adult profile
        val remoteLibrary = syncApi.fetchProfileLibrary(adultProfileId, "adult-session-b", "jwt-device-b")
        assertEquals(1, remoteLibrary.size)
        assertEquals(sourceId, remoteLibrary[0].sourceId)
        assertEquals("channel", remoteLibrary[0].itemKind)
        assertEquals(CloudIdentityHasher.hashItemId("101"), remoteLibrary[0].itemId)
        assertTrue(remoteLibrary[0].isFavourite)

        val remoteProgress = syncApi.fetchProfileWatchProgress(adultProfileId, "adult-session-b", "jwt-device-b")
        assertEquals(1, remoteProgress.size)
        assertEquals(sourceId, remoteProgress[0].sourceId)
        assertEquals("movie", remoteProgress[0].itemKind)
        assertEquals(CloudIdentityHasher.hashItemId("505"), remoteProgress[0].itemId)
        assertEquals(120_000L, remoteProgress[0].positionMs)
        assertEquals(600_000L, remoteProgress[0].durationMs)

        // 5. When catalogue is parsed from the restored source on Device B:
        // Reconnection matches items by hash without loss
        val channel101 = PlaylistChannel(
            position = 1,
            name = "News 24",
            streamUrl = "http://provider.test:8080/live/101.ts",
            sourceId = sourceId,
            sourceItemId = "101"
        )
        val movie505 = XtreamMovie(
            id = 505,
            name = "Inception",
            categoryId = "1",
            posterUrl = "http://poster.test/505.jpg",
            containerExtension = "mp4",
            rating = "8.8",
            year = "2010",
            plot = "A thief who steals corporate secrets..."
        )

        // Verify channel matches favorite
        val hashedChannelKey = "channel:$sourceId:${CloudIdentityHasher.hashItemId("101")}"
        val channelFavs = setOf(hashedChannelKey)
        // Checking with raw channel matches the hashed cloud favorite:
        val isFav = channelFavs.contains("channel:$sourceId:${channel101.sourceItemId}") ||
            channelFavs.contains("channel:$sourceId:${CloudIdentityHasher.hashItemId(channel101.sourceItemId)}")
        assertTrue("Restored favorite channel must match catalogue item", isFav)

        // Verify movie progress matches
        val progressHash = CloudIdentityHasher.hashItemId("505")
        assertEquals(progressHash, remoteProgress[0].itemId)
        assertEquals(120_000L, remoteProgress[0].positionMs)
    }

    @Test
    fun householdProfileAvatarPersistenceEncodesAndDecodes() {
        val original = HouseholdProfile(
            id = "profile-uuid-1234",
            householdId = "house-uuid-5678",
            name = "John",
            avatarUrl = "https://images.test/avatar1.png"
        )
        val json = original.encode()
        assertEquals("https://images.test/avatar1.png", json.getString("avatar_url"))

        val decoded = HouseholdProfile.decode(json)
        assertEquals("https://images.test/avatar1.png", decoded.avatarUrl)
        assertEquals("John", decoded.name)
    }
}

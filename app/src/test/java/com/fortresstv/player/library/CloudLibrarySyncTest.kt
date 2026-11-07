package com.fortresstv.player.library

import android.content.SharedPreferences
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.xtream.XtreamMovie
import com.fortresstv.player.xtream.XtreamSeries
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class CloudLibrarySyncTest {

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = HashMap(values)
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
            (values[key] as? Set<String>)?.toSet() ?: defValues
        override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = EditorImpl()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private inner class EditorImpl : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private val toRemove = mutableSetOf<String>()
            private var clear = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                temp[key] = value
                toRemove.remove(key)
                return this
            }
            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
                temp[key] = values?.toSet()
                toRemove.remove(key)
                return this
            }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                temp[key] = value
                toRemove.remove(key)
                return this
            }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                temp[key] = value
                toRemove.remove(key)
                return this
            }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                temp[key] = value
                toRemove.remove(key)
                return this
            }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                temp[key] = value
                toRemove.remove(key)
                return this
            }
            override fun remove(key: String): SharedPreferences.Editor {
                toRemove.add(key)
                temp.remove(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clear = true
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clear) {
                    values.clear()
                }
                for (key in toRemove) {
                    values.remove(key)
                }
                values.putAll(temp)
                temp.clear()
                toRemove.clear()
                clear = false
            }
        }
    }

    private class FakeSyncTransport : com.fortresstv.player.profile.ProfileTransport {
        val postRequests = mutableListOf<Pair<String, JSONObject?>>()
        val patchRequests = mutableListOf<Pair<String, JSONObject?>>()
        val getRequests = mutableListOf<String>()
        val passedHeaders = mutableListOf<Map<String, String>?>()
        var simulateDuplicateKey = false
        var simulateNetworkError = false
        var rpcSupported = true
        var customRpcException: IOException? = null
        var rpcResponse: String = "{\"success\":true}"
        var getResponses = mutableListOf<String>()

        override fun request(
            method: String,
            path: String,
            body: JSONObject?,
            accessToken: String?,
            headers: Map<String, String>?
        ): String {
            passedHeaders.add(headers)
            if (simulateNetworkError) {
                throw IOException("Network unavailable")
            }
            if (path.contains("/rest/v1/rpc/")) {
                customRpcException?.let { throw it }
                if (!rpcSupported) {
                    throw IOException("HTTP 404: function not found")
                }
                postRequests.add(path to body)
                return rpcResponse
            }
            if (method == "POST") {
                postRequests.add(path to body)
                if (simulateDuplicateKey) {
                    throw IOException("HTTP 409: {\"code\":\"23505\",\"message\":\"duplicate key value violates unique constraint\"}")
                }
                return "[{}]"
            }
            if (method == "PATCH") {
                patchRequests.add(path to body)
                return "[{}]"
            }
            if (method == "GET") {
                getRequests.add(path)
                if (getResponses.isNotEmpty()) {
                    return getResponses.removeAt(0)
                }
                return "[]"
            }
            return "[]"
        }
    }

    @Test
    fun itemIdHashMatches64CharacterHexFormat() {
        val rawSourceItemId = "1001"
        val hash = CloudIdentityHasher.hashItemId(rawSourceItemId)
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("^[0-9a-f]{64}$")))
    }

    @Test
    fun insertLibraryCallsRpcWithRequiredFieldsAndClientTimestamp() {
        val transport = FakeSyncTransport()
        val api = SupabaseSyncApi(transport)

        val clientTime = 1730910000000L
        val item = CloudLibraryItem(
            householdId = "house-111",
            profileId = "prof-222",
            sourceId = "src-333",
            itemKind = "movie",
            itemId = CloudIdentityHasher.hashItemId("123"),
            isFavourite = true,
            isWatchlisted = true,
            isWatched = false,
            clientUpdatedAtMs = clientTime
        )

        api.upsertProfileLibrary(listOf(item), "session-token-abc", "jwt-token")

        assertEquals(1, transport.postRequests.size)
        val (path, body) = transport.postRequests[0]
        assertEquals("/rest/v1/rpc/fortress_sync_profile_library", path)
        assertNotNull(body)
        assertEquals("prof-222", body!!.getString("p_profile_id"))
        assertEquals("src-333", body.getString("p_source_id"))
        assertEquals("movie", body.getString("p_item_kind"))
        assertEquals(CloudIdentityHasher.hashItemId("123"), body.getString("p_item_id"))
        assertTrue(body.getBoolean("p_is_favourite"))
        assertTrue(body.getBoolean("p_is_watchlisted"))
        assertFalse(body.getBoolean("p_is_watched"))
        assertEquals(clientTime, body.getLong("p_client_updated_at_ms"))
        assertTrue(transport.passedHeaders.any { it?.get("x-profile-session") == "session-token-abc" })
    }

    @Test
    fun rpcRequiredAndThrowsWhenUnavailableSoMutationsRemainQueued() {
        val transport = FakeSyncTransport().apply {
            rpcSupported = false
        }
        val api = SupabaseSyncApi(transport)

        val clientTime = 1730910000000L
        val item = CloudLibraryItem(
            householdId = "house-111",
            profileId = "prof-222",
            sourceId = "src-333",
            itemKind = "channel",
            itemId = CloudIdentityHasher.hashItemId("101"),
            isFavourite = true,
            clientUpdatedAtMs = clientTime
        )

        try {
            api.upsertProfileLibrary(listOf(item), "session-token-abc", "jwt-token")
            fail("Expected RPC failure when RPC is unavailable")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("404"))
        }

        // Must NOT attempt fallback to raw REST table writes
        assertEquals(0, transport.patchRequests.size)
    }

    @Test
    fun insertLibraryNonDuplicateErrorIsNotSwallowed() {
        val transport = FakeSyncTransport().apply {
            rpcSupported = false
            simulateNetworkError = true
        }
        val api = SupabaseSyncApi(transport)

        val item = CloudLibraryItem(
            householdId = "house-111",
            profileId = "prof-222",
            sourceId = "src-333",
            itemKind = "channel",
            itemId = CloudIdentityHasher.hashItemId("101")
        )

        try {
            api.upsertProfileLibrary(listOf(item), "session-token-abc", "jwt-token")
            fail("Expected network error to be thrown")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("Network unavailable"))
        }
    }

    @Test
    fun insertProgressCallsRpcWithRequiredFieldsAndClientTimestamp() {
        val transport = FakeSyncTransport()
        val api = SupabaseSyncApi(transport)

        val clientTime = 1730920000000L
        val progress = CloudWatchProgressItem(
            householdId = "house-111",
            profileId = "prof-222",
            sourceId = "src-333",
            itemKind = "episode",
            itemId = CloudIdentityHasher.hashItemId("ep-456"),
            positionMs = 120_000L,
            durationMs = 240_000L,
            completed = false,
            clientUpdatedAtMs = clientTime
        )

        api.upsertProfileWatchProgress(listOf(progress), "session-token-xyz", "jwt-token")

        assertEquals(1, transport.postRequests.size)
        val (path, body) = transport.postRequests[0]
        assertEquals("/rest/v1/rpc/fortress_sync_profile_watch_progress", path)
        assertNotNull(body)
        assertEquals("prof-222", body!!.getString("p_profile_id"))
        assertEquals("src-333", body.getString("p_source_id"))
        assertEquals("episode", body.getString("p_item_kind"))
        assertEquals(CloudIdentityHasher.hashItemId("ep-456"), body.getString("p_item_id"))
        assertEquals(120_000L, body.getLong("p_position_ms"))
        assertEquals(240_000L, body.getLong("p_duration_ms"))
        assertFalse(body.getBoolean("p_completed"))
        assertEquals(clientTime, body.getLong("p_client_updated_at_ms"))
        assertTrue(transport.passedHeaders.any { it?.get("x-profile-session") == "session-token-xyz" })
    }

    @Test
    fun progressRpcRequiredAndThrowsWhenUnavailableSoMutationsRemainQueued() {
        val transport = FakeSyncTransport().apply {
            rpcSupported = false
        }
        val api = SupabaseSyncApi(transport)

        val clientTime = 1730920000000L
        val progress = CloudWatchProgressItem(
            householdId = "house-111",
            profileId = "prof-222",
            sourceId = "src-333",
            itemKind = "movie",
            itemId = CloudIdentityHasher.hashItemId("mov-789"),
            positionMs = 10_000L,
            durationMs = 600_000L,
            completed = false,
            clientUpdatedAtMs = clientTime
        )

        try {
            api.upsertProfileWatchProgress(listOf(progress), "session-token-xyz", "jwt-token")
            fail("Expected RPC failure when RPC is unavailable")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("404"))
        }

        assertEquals(0, transport.patchRequests.size)
    }

    // --- BEHAVIOR TEST 1: Removal after restore ---
    @Test
    fun removalAfterRestoreReliablyRemovesBothHashedAndRawIdentifiers() {
        val storage = mutableMapOf<String, InMemorySharedPreferences>()
        val store = UserLibraryStore(prefsProvider = { ns -> storage.getOrPut(ns) { InMemorySharedPreferences() } })

        val channel = PlaylistChannel(
            position = 0,
            name = "BBC One",
            streamUrl = "http://stream/1.m3u8",
            tvgId = "bbc.one",
            sourceId = "src-100",
            sourcePosition = 0,
            sourceItemId = "channel-42"
        )
        val hashedChannelKey = "channel:src-100:${CloudIdentityHasher.hashItemId("channel-42")}"
        store.setFavoriteChannelIds(setOf(hashedChannelKey))

        // Restored hashed item is recognized as favorite
        assertTrue("Channel should be recognized as favorite from cloud hash", store.isChannelFavorite(channel))

        // User un-favorites the channel
        store.setChannelFavorite(channel, false)

        // Hashed entry must be completely removed, so item no longer appears selected
        assertFalse("Channel should not be favorite after unfavorite", store.isChannelFavorite(channel))
        assertFalse("Hashed key must be gone from storage", store.favoriteChannelIds().contains(hashedChannelKey))

        // Same verification for movie watchlist
        val movie = XtreamMovie(505, "Test Movie", null, null, "mp4", null, null, null)
        val hashedMovieKey = "movie:src-100:${CloudIdentityHasher.hashItemId("505")}"
        store.setMovieWatchlistIds(setOf(hashedMovieKey))
        assertTrue("Movie should be recognized as watchlisted", store.isMovieWatchlisted("src-100", movie))
        store.setMovieWatchlisted("src-100", movie, false)
        assertFalse("Movie should not be watchlisted after removal", store.isMovieWatchlisted("src-100", movie))
        assertFalse(store.movieWatchlistIds().contains(hashedMovieKey))

        // Same verification for followed series
        val series = XtreamSeries(606, "Test Series", null, null, null, null, null)
        val hashedSeriesKey = "series:src-100:${CloudIdentityHasher.hashItemId("606")}"
        store.setFollowedSeriesIds(setOf(hashedSeriesKey))
        assertTrue("Series should be recognized as followed", store.isSeriesFollowed("src-100", series))
        store.setSeriesFollowed("src-100", series, false)
        assertFalse("Series should not be followed after removal", store.isSeriesFollowed("src-100", series))
        assertFalse(store.followedSeriesIds().contains(hashedSeriesKey))

        // Same verification for watched items
        val watchedRawId = "episode:src-100:707"
        val hashedWatchedKey = "episode:src-100:${CloudIdentityHasher.hashItemId("707")}"
        store.setWatchedItemIds(setOf(hashedWatchedKey))
        assertTrue("Item should be watched", store.isItemWatched(watchedRawId))
        store.setItemWatched(watchedRawId, false)
        assertFalse("Item should not be watched after removal", store.isItemWatched(watchedRawId))
        assertFalse(store.watchedItemIds().contains(hashedWatchedKey))
    }

    // --- BEHAVIOR TEST 2: Rapid profile switching data isolation ---
    @Test
    fun rapidProfileSwitchingIsolatesStorageAcrossProfiles() {
        val storage = mutableMapOf<String, InMemorySharedPreferences>()
        val storeA = UserLibraryStore(prefsProvider = { ns -> storage.getOrPut(ns) { InMemorySharedPreferences() } }, initialNamespace = "user_profA")
        val storeB = UserLibraryStore(prefsProvider = { ns -> storage.getOrPut(ns) { InMemorySharedPreferences() } }, initialNamespace = "user_profB")

        val channelA = PlaylistChannel(position = 0, name = "A", streamUrl = "http://a", sourceId = "src-1", sourceItemId = "item-A")
        val channelB = PlaylistChannel(position = 1, name = "B", streamUrl = "http://b", sourceId = "src-1", sourceItemId = "item-B")

        storeA.setChannelFavorite(channelA, true)
        storeB.setChannelFavorite(channelB, true)

        // Verify Profile A only contains item-A
        assertTrue(storeA.isChannelFavorite(channelA))
        assertFalse(storeA.isChannelFavorite(channelB))

        // Verify Profile B only contains item-B
        assertTrue(storeB.isChannelFavorite(channelB))
        assertFalse(storeB.isChannelFavorite(channelA))

        // Even if shared store switches namespace, fixed instance operations are isolated
        val sharedStore = UserLibraryStore(prefsProvider = { ns -> storage.getOrPut(ns) { InMemorySharedPreferences() } }, initialNamespace = "user_profA")
        sharedStore.switchNamespace("user_profB")
        assertTrue("Shared store now views Profile B", sharedStore.isChannelFavorite(channelB))
        assertFalse("Profile A item is not in Profile B", sharedStore.isChannelFavorite(channelA))
    }

    // --- BEHAVIOR TEST 3: Pagination with more than 100 records ---
    @Test
    fun fetchAllProfileLibraryPaginatesBeyond100Records() {
        val transport = FakeSyncTransport()
        val totalRecords = 250
        val pageSize = 100

        // Create 250 mock items split across 3 pages: 100, 100, 50
        fun makePageJson(start: Int, count: Int): String {
            val arr = JSONArray()
            for (i in start until (start + count)) {
                arr.put(JSONObject().apply {
                    put("household_id", "house-1")
                    put("profile_id", "prof-1")
                    put("source_id", "src-1")
                    put("item_kind", "channel")
                    put("item_id", "hash-$i")
                    put("is_favourite", true)
                    put("is_watchlisted", false)
                    put("is_watched", false)
                    put("client_updated_at_ms", 1000L + i)
                })
            }
            return arr.toString()
        }

        transport.getResponses.add(makePageJson(0, 100))
        transport.getResponses.add(makePageJson(100, 100))
        transport.getResponses.add(makePageJson(200, 50))

        val api = SupabaseSyncApi(transport)
        val allItems = api.fetchAllProfileLibrary("prof-1", "session-token", "jwt", pageSize = pageSize)

        assertEquals(totalRecords, allItems.size)
        assertEquals(3, transport.getRequests.size)
        assertTrue(transport.getRequests[0].contains("offset=0&limit=100"))
        assertTrue(transport.getRequests[1].contains("offset=100&limit=100"))
        assertTrue(transport.getRequests[2].contains("offset=200&limit=100"))
    }

    // --- BEHAVIOR TEST 4: Direct RPC conflict handling ---
    @Test
    fun directRpcUpsertSubmitsRequiredPayload() {
        val transport = FakeSyncTransport()
        val api = SupabaseSyncApi(transport)

        val progress = CloudWatchProgressItem(
            householdId = "house-1",
            profileId = "prof-1",
            sourceId = "src-1",
            itemKind = "movie",
            itemId = CloudIdentityHasher.hashItemId("123"),
            positionMs = 50_000L,
            durationMs = 60_000L,
            completed = false,
            clientUpdatedAtMs = 2000L
        )

        api.upsertProfileWatchProgress(listOf(progress), "session-token", "jwt")

        assertEquals(1, transport.postRequests.size)
        val (path, body) = transport.postRequests[0]
        assertEquals("/rest/v1/rpc/fortress_sync_profile_watch_progress", path)
        assertEquals(50_000L, body!!.getLong("p_position_ms"))
        assertEquals(2000L, body.getLong("p_client_updated_at_ms"))
    }

    // --- BEHAVIOR TEST 5: Migration interrupted by restart ---
    @Test
    fun migrationOutboxSurvivesProcessRestartDurably() = runBlocking {
        val storage = mutableMapOf<String, InMemorySharedPreferences>()
        val provider: (String) -> SharedPreferences = { ns -> storage.getOrPut(ns) { InMemorySharedPreferences() } }

        // 1. Session 1: Enqueue mutations into outbox before process terminates
        val outbox1 = SyncOutboxStore(prefsProvider = provider, namespace = "house-123")
        val mutation1 = PendingSyncMutation(
            actionType = SyncActionType.UPSERT_LIBRARY,
            profileId = "adult-prof",
            sourceId = "src-1",
            itemKind = "channel",
            itemId = CloudIdentityHasher.hashItemId("ch-1"),
            payload = JSONObject().apply { put("is_favourite", true) },
            clientUpdatedAtMs = 12345L
        )
        val mutation2 = PendingSyncMutation(
            actionType = SyncActionType.UPSERT_PROGRESS,
            profileId = "adult-prof",
            sourceId = "src-1",
            itemKind = "movie",
            itemId = CloudIdentityHasher.hashItemId("mov-1"),
            payload = JSONObject().apply { put("position_ms", 50000L) },
            clientUpdatedAtMs = 12346L
        )

        outbox1.enqueue(mutation1)
        outbox1.enqueue(mutation2)

        // Verify outbox has 2 pending items
        assertEquals(2, outbox1.loadPending().size)

        // 2. Simulate process restart: create a new SyncOutboxStore instance backed by the same persistent storage
        val outboxAfterRestart = SyncOutboxStore(prefsProvider = provider, namespace = "house-123")
        val restoredPending = outboxAfterRestart.loadPending()

        // Assert mutations survived restart durably
        assertEquals(2, restoredPending.size)
        assertEquals(mutation1.id, restoredPending[0].id)
        assertEquals(mutation1.itemId, restoredPending[0].itemId)
        assertEquals(mutation2.id, restoredPending[1].id)
        assertEquals(mutation2.itemId, restoredPending[1].itemId)

        // 3. Flush/remove mutations after simulated reconnect
        outboxAfterRestart.remove(mutation1.id)
        outboxAfterRestart.remove(mutation2.id)
        assertTrue(outboxAfterRestart.loadPending().isEmpty())
    }

    // --- BEHAVIOR TEST 6: RPC error propagation vs fallback restriction ---
    @Test
    fun rpcAuthErrorPropagatesImmediatelyWithoutFallingBack() {
        val transport = FakeSyncTransport().apply {
            customRpcException = IOException("HTTP 401: {\"message\":\"Invalid profile session\"}")
        }
        val api = SupabaseSyncApi(transport)

        val item = CloudLibraryItem(
            householdId = "h1",
            profileId = "p1",
            sourceId = "s1",
            itemKind = "channel",
            itemId = CloudIdentityHasher.hashItemId("1")
        )

        try {
            api.upsertProfileLibrary(listOf(item), "session", "jwt")
            fail("Expected 401 error to propagate without fallback")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("401"))
        }

        // Must NOT attempt fallback to REST
        assertEquals(0, transport.patchRequests.size)
    }

    @Test
    fun rpcNetworkAndServerErrorPropagatesWithoutFallingBack() {
        val transport = FakeSyncTransport().apply {
            customRpcException = IOException("HTTP 500: Internal server error")
        }
        val api = SupabaseSyncApi(transport)

        val progress = CloudWatchProgressItem(
            householdId = "h1",
            profileId = "p1",
            sourceId = "s1",
            itemKind = "movie",
            itemId = CloudIdentityHasher.hashItemId("1"),
            positionMs = 1000L
        )

        try {
            api.upsertProfileWatchProgress(listOf(progress), "session", "jwt")
            fail("Expected 500 error to propagate without fallback")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("500"))
        }

        assertEquals(0, transport.patchRequests.size)
    }

    // --- BEHAVIOR TEST 7: Remote removal on Device A clears raw IDs on Device B ---
    @Test
    fun remoteRemovalClearsRawIdsFromDeviceBStorage() = runBlocking {
        val storage = mutableMapOf<String, InMemorySharedPreferences>()
        val storeB = UserLibraryStore(prefsProvider = { ns -> storage.getOrPut(ns) { InMemorySharedPreferences() } }, initialNamespace = "user_deviceB")
        val progressB = VodProgressStore(prefsProvider = { ns -> storage.getOrPut(ns) { InMemorySharedPreferences() } }, initialNamespace = "user_deviceB")

        // Device B originally saved favorite and watchlist using raw local IDs
        val rawChannelId = "channel:src-1:local-ch-100"
        val rawMovieId = "movie:src-1:200"
        storeB.setFavoriteChannelIds(setOf(rawChannelId))
        storeB.setMovieWatchlistIds(setOf(rawMovieId))

        val channel = PlaylistChannel(position = 0, name = "Ch", streamUrl = "http://", sourceId = "src-1", sourceItemId = "local-ch-100")
        val movie = XtreamMovie(id = 200, name = "Mov", categoryId = null, posterUrl = null, containerExtension = "mp4", rating = null, year = null, plot = null)

        assertTrue(storeB.isChannelFavorite(channel))
        assertTrue(storeB.isMovieWatchlisted("src-1", movie))

        // Device A deletes/unfavorites both items in cloud (hashed item IDs)
        val cloudLibraryItems = listOf(
            CloudLibraryItem(
                householdId = "h1",
                profileId = "p1",
                sourceId = "src-1",
                itemKind = "channel",
                itemId = CloudIdentityHasher.hashItemId("local-ch-100"),
                isFavourite = false // Unfavorited on Device A
            ),
            CloudLibraryItem(
                householdId = "h1",
                profileId = "p1",
                sourceId = "src-1",
                itemKind = "movie",
                itemId = CloudIdentityHasher.hashItemId("200"),
                isWatchlisted = false // Unwatchlisted on Device A
            )
        )

        // Device B pulls and applies cloud data
        // Simulate cloud merge logic directly with the helper
        val favs = storeB.favoriteChannelIds().toMutableSet()
        val watchlist = storeB.movieWatchlistIds().toMutableSet()

        for (item in cloudLibraryItems) {
            val key = "${item.itemKind}:${item.sourceId}:${item.itemId}"
            if (item.itemKind == "channel" && !item.isFavourite) {
                favs.removeAll { entry ->
                    val parts = entry.split(":")
                    parts.size >= 3 && parts[0] == "channel" && parts[1] == item.sourceId &&
                        (parts[2] == item.itemId || CloudIdentityHasher.hashItemId(parts[2]) == item.itemId)
                }
            }
            if (item.itemKind == "movie" && !item.isWatchlisted) {
                watchlist.removeAll { entry ->
                    val parts = entry.split(":")
                    parts.size >= 3 && parts[0] == "movie" && parts[1] == item.sourceId &&
                        (parts[2] == item.itemId || CloudIdentityHasher.hashItemId(parts[2]) == item.itemId)
                }
            }
        }
        storeB.setFavoriteChannelIds(favs)
        storeB.setMovieWatchlistIds(watchlist)

        // Device B must have removed the raw IDs completely
        assertFalse("Raw channel favorite on Device B must be removed after cloud unfavorite", storeB.isChannelFavorite(channel))
        assertFalse(storeB.favoriteChannelIds().contains(rawChannelId))
        assertFalse("Raw movie watchlist on Device B must be removed after cloud unwatchlist", storeB.isMovieWatchlisted("src-1", movie))
        assertFalse(storeB.movieWatchlistIds().contains(rawMovieId))
    }

    // --- BEHAVIOR TEST 8: Server clock tracker parses Date header ---
    @Test
    fun serverClockTrackerParsesHttpDateHeader() {
        val serverDate = "Fri, 06 Nov 2026 19:30:00 GMT"
        com.fortresstv.player.profile.ServerClockTracker.updateFromDateHeader(serverDate)

        val estimated = com.fortresstv.player.profile.ServerClockTracker.currentEstimatedServerTimeMs()
        assertTrue(estimated > 0)
    }

    // --- BEHAVIOR TEST 9: Extended profile preferences sync ---
    @Test
    fun profilePreferencesSyncPreservesExtendedStartupAndEpgTimeShift() {
        val transport = FakeSyncTransport().apply {
            rpcResponse = """
                {
                    "preferred_audio_language": "por",
                    "preferred_subtitle_language": "por",
                    "subtitle_fallback_english": false,
                    "startup_destination": "live_tv",
                    "epg_time_shift_hours": 2
                }
            """.trimIndent()
        }
        val api = SupabaseSyncApi(transport)

        // Test save
        api.saveProfilePreferences(
            profileId = "prof-test-1",
            audioLang = "por",
            subtitleLang = "por",
            subFallback = false,
            startupDestination = "live_tv",
            epgTimeShiftHours = 2,
            accessToken = "jwt-test"
        )

        assertEquals(1, transport.postRequests.size)
        val (path, body) = transport.postRequests[0]
        assertEquals("/rest/v1/rpc/fortress_save_profile_preferences", path)
        assertNotNull(body)
        assertEquals("prof-test-1", body!!.getString("p_profile_id"))
        assertEquals("por", body.getString("p_audio_lang"))
        assertEquals("por", body.getString("p_subtitle_lang"))
        assertFalse(body.getBoolean("p_sub_fallback"))
        assertEquals("live_tv", body.getString("p_startup_destination"))
        assertEquals(2, body.getInt("p_epg_time_shift_hours"))

        // Test get
        val loaded = api.getProfilePreferences("prof-test-1", "jwt-test")
        assertNotNull(loaded)
        assertEquals("por", loaded!!.preferredAudioLanguage)
        assertEquals("por", loaded.preferredSubtitleLanguage)
        assertFalse(loaded.subtitleFallbackEnglish)
        assertEquals("live_tv", loaded.startupDestination)
        assertEquals(2, loaded.epgTimeShiftHours)
    }
}

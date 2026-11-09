package com.fortresstv.player.library

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VodProgressStoreTest {

    private class TestSharedPreferences : SharedPreferences {
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
                temp.clear()
                toRemove.clear()
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clear) values.clear()
                toRemove.forEach { values.remove(it) }
                values.putAll(temp)
                temp.clear()
                toRemove.clear()
                clear = false
            }
        }
    }

    @Test
    fun roundTripsSourceScopedProgressWithoutStreamCredentials() {
        val entry = VodProgressEntry(
            key = "episode:source-1:701",
            kind = VodProgressKind.EPISODE,
            sourceId = "source-1",
            itemId = "701",
            seriesId = 20,
            title = "S01E01 · Pilot",
            posterUrl = "https://img.example/pilot.jpg",
            containerExtension = "mkv",
            season = 1,
            episode = 1,
            positionMs = 120_000,
            durationMs = 2_400_000,
            updatedAtMs = 1000
        )

        val encoded = VodProgressStore.encode(listOf(entry))
        val decoded = VodProgressStore.decode(encoded)

        assertEquals(listOf(entry), decoded)
        assertEquals(false, encoded.contains("username"))
        assertEquals(false, encoded.contains("password"))
    }

    @Test
    fun roundTripsCatchupProgress() {
        val catchupEntry = VodProgressEntry(
            key = "catchup:source-1:101_1731056400000",
            kind = VodProgressKind.CATCHUP,
            sourceId = "source-1",
            itemId = "101_1731056400000",
            seriesId = null,
            title = "RTP 1 · Telejornal",
            posterUrl = "https://img.example/rtp1.png",
            containerExtension = "ts",
            season = null,
            episode = null,
            positionMs = 600_000L,
            durationMs = 3_600_000L,
            updatedAtMs = 5000L
        )

        val encoded = VodProgressStore.encode(listOf(catchupEntry))
        val decoded = VodProgressStore.decode(encoded)

        assertEquals(1, decoded.size)
        val result = decoded.first()
        assertEquals(VodProgressKind.CATCHUP, result.kind)
        assertEquals("catchup:source-1:101_1731056400000", result.key)
        assertEquals(600_000L, result.positionMs)
        assertEquals(3_600_000L, result.durationMs)
        assertEquals(600_000f / 3_600_000f, result.progressFraction, 0.001f)
    }

    @Test
    fun profileProgressNamespaceGeneratesCorrectPartitions() {
        assertEquals("restricted_default", profileProgressNamespace(null, "restricted_default"))
        assertEquals("restricted_default", profileProgressNamespace("house_123", "restricted_default"))
        assertEquals("restricted_default", profileProgressNamespace("house_123", ""))

        // Offline / local profiles
        assertEquals("profile_fabio_1", profileProgressNamespace(null, "fabio_1"))
        assertEquals("profile_lisa_2", profileProgressNamespace(null, "lisa_2"))
        assertEquals("profile_sofia_child", profileProgressNamespace(null, "sofia_child"))

        // Authenticated multi-device household profiles
        assertEquals("hh_abc_fabio_1", profileProgressNamespace("hh_abc", "fabio_1"))
        assertEquals("hh_abc_lisa_2", profileProgressNamespace("hh_abc", "lisa_2"))
        assertEquals("hh_abc_sofia_child", profileProgressNamespace("hh_abc", "sofia_child"))
    }

    @Test
    fun strictlyIsolatesWatchProgressPerProfileWithoutCrossContamination() {
        val memoryStorage = mutableMapOf<String, SharedPreferences>()
        val store = VodProgressStore(
            prefsProvider = { ns -> memoryStorage.getOrPut(ns) { TestSharedPreferences() } },
            initialNamespace = "profile_fabio"
        )

        val fabioMovie = VodProgressEntry(
            key = "movie:source-1:500",
            kind = VodProgressKind.MOVIE,
            sourceId = "source-1",
            itemId = "500",
            seriesId = null,
            title = "Inception",
            posterUrl = null,
            containerExtension = "mp4",
            season = null,
            episode = null,
            positionMs = 1_800_000L,
            durationMs = 7_200_000L,
            updatedAtMs = 1000L
        )
        val fabioCatchup = VodProgressEntry(
            key = "catchup:source-1:99_123456",
            kind = VodProgressKind.CATCHUP,
            sourceId = "source-1",
            itemId = "99_123456",
            seriesId = null,
            title = "SIC · Jornal da Noite",
            posterUrl = null,
            containerExtension = "ts",
            season = null,
            episode = null,
            positionMs = 900_000L,
            durationMs = 3_600_000L,
            updatedAtMs = 1200L
        )

        store.save(fabioMovie)
        store.save(fabioCatchup)

        assertEquals(2, store.entries().size)
        assertNotNull(store.findProgress(VodProgressKind.MOVIE, "source-1", "500"))
        assertNotNull(store.findProgress(VodProgressKind.CATCHUP, "source-1", "99_123456"))

        // Switch to Lisa's profile: should be completely empty and isolated
        store.switchNamespace("profile_lisa")
        assertEquals(0, store.entries().size)
        assertNull(store.findProgress(VodProgressKind.MOVIE, "source-1", "500"))
        assertNull(store.findProgress(VodProgressKind.CATCHUP, "source-1", "99_123456"))

        // Lisa watches an episode
        val lisaEpisode = VodProgressEntry(
            key = "episode:source-1:201",
            kind = VodProgressKind.EPISODE,
            sourceId = "source-1",
            itemId = "201",
            seriesId = 10,
            title = "Friends S01E01",
            posterUrl = null,
            containerExtension = "mp4",
            season = 1,
            episode = 1,
            positionMs = 300_000L,
            durationMs = 1_200_000L,
            updatedAtMs = 2000L
        )
        store.save(lisaEpisode)

        assertEquals(1, store.entries().size)
        assertEquals("episode:source-1:201", store.entries().first().key)

        // Switch to Sofia (child): should also be completely empty and isolated
        store.switchNamespace("profile_sofia")
        assertEquals(0, store.entries().size)

        // Switch back to Fábio: retains exact original progress without interference
        store.switchNamespace("profile_fabio")
        assertEquals(2, store.entries().size)
        assertEquals(listOf("catchup:source-1:99_123456", "movie:source-1:500"), store.entries().map { it.key })
        assertEquals(1_800_000L, store.findProgress(VodProgressKind.MOVIE, "source-1", "500")?.positionMs)
        assertEquals(900_000L, store.findProgress(VodProgressKind.CATCHUP, "source-1", "99_123456")?.positionMs)
    }

    @Test
    fun catchupProgressKeyCanonicalizesAndParsesBothCanonicalAndLegacyFormats() {
        val instant = java.time.Instant.ofEpochMilli(1731056400000L)
        val key = CatchupProgressKey("RTP1.pt", instant)
        val itemId = key.toItemId()

        assertEquals("catchup:rtp1.pt:1731056400000", itemId)

        val parsed = CatchupProgressKey.fromItemId(itemId)
        assertNotNull(parsed)
        assertEquals("rtp1.pt", parsed!!.channelId)
        assertEquals(instant, parsed.programStartUtc)

        // Legacy underscore format fallback: "101_1731056400000"
        val legacyParsed = CatchupProgressKey.fromItemId("101_1731056400000")
        assertNotNull(legacyParsed)
        assertEquals("101", legacyParsed!!.channelId)
        assertEquals(instant, legacyParsed.programStartUtc)

        // Invalid format returns null
        assertNull(CatchupProgressKey.fromItemId("random-id"))
    }

    @Test
    fun vodProgressKindWireFormatMatchesSupabaseConstraint() {
        assertEquals("movie", VodProgressKind.MOVIE.wireValue)
        assertEquals("episode", VodProgressKind.EPISODE.wireValue)
        assertEquals("catchup", VodProgressKind.CATCHUP.wireValue)

        assertEquals(VodProgressKind.MOVIE, VodProgressKind.fromWireValue("movie"))
        assertEquals(VodProgressKind.EPISODE, VodProgressKind.fromWireValue("episode"))
        assertEquals(VodProgressKind.CATCHUP, VodProgressKind.fromWireValue("catchup"))
        assertEquals(VodProgressKind.CATCHUP, VodProgressKind.fromWireValue("CATCHUP"))
        assertEquals(VodProgressKind.EPISODE, VodProgressKind.fromWireValue("unknown"))
    }

    @Test
    fun concurrentMultiThreadedAccessAndNamespaceSwitchingIsThreadSafe() {
        val memoryStorage = java.util.concurrent.ConcurrentHashMap<String, SharedPreferences>()
        val store = VodProgressStore(
            prefsProvider = { ns -> memoryStorage.getOrPut(ns) { TestSharedPreferences() } },
            initialNamespace = "profile_worker_0"
        )

        val numThreads = 8
        val iterationsPerThread = 200
        val executor = java.util.concurrent.Executors.newFixedThreadPool(numThreads)
        val startLatch = java.util.concurrent.CountDownLatch(1)
        val doneLatch = java.util.concurrent.CountDownLatch(numThreads)
        val errors = java.util.concurrent.atomic.AtomicInteger(0)

        for (t in 0 until numThreads) {
            val threadId = t
            executor.execute {
                try {
                    startLatch.await()
                    for (i in 0 until iterationsPerThread) {
                        val profileId = "profile_worker_${(threadId + i) % 4}"
                        store.switchNamespace(profileId)
                        val entry = VodProgressEntry(
                            key = "movie:src-$threadId:item-$i",
                            kind = VodProgressKind.MOVIE,
                            sourceId = "src-$threadId",
                            itemId = "item-$i",
                            seriesId = null,
                            title = "Movie $i",
                            posterUrl = null,
                            containerExtension = "mp4",
                            season = null,
                            episode = null,
                            positionMs = 60_000L + i,
                            durationMs = 600_000L,
                            updatedAtMs = System.currentTimeMillis()
                        )
                        store.save(entry)
                        val readEntries = store.entries()
                        assertNotNull(readEntries)
                        store.findProgress(VodProgressKind.MOVIE, "src-$threadId", "item-$i")
                    }
                } catch (t: Throwable) {
                    errors.incrementAndGet()
                    t.printStackTrace()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        val finished = doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS)
        executor.shutdown()

        assertEquals(true, finished)
        assertEquals(0, errors.get())
    }

    @Test
    fun ignoresMalformedEntries() {
        assertEquals(emptyList<VodProgressEntry>(), VodProgressStore.decode("not-json"))
        assertEquals(emptyList<VodProgressEntry>(), VodProgressStore.decode("[{}]"))
    }

    @Test
    fun saveDoesNotEraseExistingProgressWhenPositionBelowMinimum() {
        val store = VodProgressStore(
            prefsProvider = { TestSharedPreferences() },
            initialNamespace = "test"
        )
        val validEntry = VodProgressEntry(
            key = "episode:source-1:701",
            kind = VodProgressKind.EPISODE,
            sourceId = "source-1",
            itemId = "701",
            seriesId = 20,
            title = "Pilot",
            posterUrl = null,
            containerExtension = "mkv",
            season = 1,
            episode = 1,
            positionMs = 120_000,
            durationMs = 2_400_000,
            updatedAtMs = 1000
        )
        store.save(validEntry)
        assertEquals(1, store.entries().size)

        // Now attempt to save 0ms during player teardown
        val teardownEntry = validEntry.copy(positionMs = 0L, updatedAtMs = 2000)
        store.save(teardownEntry)

        // Existing progress must NOT be erased
        assertEquals(1, store.entries().size)
        assertEquals(120_000L, store.entries().first().positionMs)
    }

    @Test
    fun saveNextEpisodeSavesUnstartedEpisodeAndReplacesPreviousEpisodeInSeries() {
        val store = VodProgressStore(
            prefsProvider = { TestSharedPreferences() },
            initialNamespace = "test_series"
        )
        val ep20 = VodProgressEntry(
            key = "episode:source-1:520",
            kind = VodProgressKind.EPISODE,
            sourceId = "source-1",
            itemId = "520",
            seriesId = 42,
            title = "The Mentalist · S5 E20 · Red Velvet Cupcakes",
            posterUrl = null,
            containerExtension = "mp4",
            season = 5,
            episode = 20,
            positionMs = 1_200_000L,
            durationMs = 2_400_000L,
            updatedAtMs = 1000L
        )
        store.save(ep20)
        assertEquals(1, store.entries().size)
        assertEquals("520", store.entries().first().itemId)

        // Ep 20 completes and next episode (Ep 21) is suggested
        val ep21 = VodProgressEntry(
            key = "episode:source-1:521",
            kind = VodProgressKind.EPISODE,
            sourceId = "source-1",
            itemId = "521",
            seriesId = 42,
            title = "The Mentalist · S5 E21 · Red and Itchy",
            posterUrl = null,
            containerExtension = "mp4",
            season = 5,
            episode = 21,
            positionMs = 0L,
            durationMs = 0L,
            updatedAtMs = 2000L
        )
        store.saveNextEpisode(ep21)

        val entries = store.entries()
        assertEquals(1, entries.size)
        assertEquals("521", entries.first().itemId)
        assertEquals(0L, entries.first().positionMs)
        assertEquals(0f, entries.first().progressFraction, 0.001f)
    }

    @Test
    fun removeDismissesEntryAndSaveUndismisses() {
        val store = VodProgressStore(
            prefsProvider = { TestSharedPreferences() },
            initialNamespace = "test_dismiss"
        )
        val entry = VodProgressEntry(
            key = "episode:source-1:521",
            kind = VodProgressKind.EPISODE,
            sourceId = "source-1",
            itemId = "521",
            seriesId = 42,
            title = "The Mentalist · S5 E21",
            posterUrl = null,
            containerExtension = "mp4",
            season = 5,
            episode = 21,
            positionMs = 0L,
            durationMs = 0L,
            updatedAtMs = 1000L
        )
        store.saveNextEpisode(entry)
        assertEquals(1, store.entries().size)

        // User dismisses/removes entry from continue watching
        store.remove(entry.key, entry.seriesId)
        assertEquals(0, store.entries().size)
        assertEquals(true, store.isDismissed(entry.key, "42"))

        // User starts watching next episode again -> undismisses automatically
        store.save(entry.copy(positionMs = 60_000L, durationMs = 2_400_000L))
        assertEquals(false, store.isDismissed(entry.key, "42"))
        assertEquals(1, store.entries().size)
    }
}

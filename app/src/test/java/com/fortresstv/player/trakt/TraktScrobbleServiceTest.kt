package com.fortresstv.player.trakt

import android.content.Context
import android.content.SharedPreferences
import com.fortresstv.player.VodPlaybackRequest
import com.fortresstv.player.library.VodProgressKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class TraktScrobbleServiceTest {

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
                temp.clear()
                toRemove.addAll(values.keys)
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                for (key in toRemove) {
                    values.remove(key)
                }
                values.putAll(temp)
                temp.clear()
                toRemove.clear()
            }
        }
    }

    private class MockContext : android.content.ContextWrapper(null) {
        val prefsMap = mutableMapOf<String, SharedPreferences>()

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            return prefsMap.getOrPut(name) { TestSharedPreferences() }
        }

        override fun getApplicationContext(): Context = this
    }

    @Test
    fun canScrobbleEnforcesStrictVerifiedIds() {
        val context = MockContext()
        val store = TraktSettingsStore(context, "test_ns")
        store.saveToken(TraktToken("acc", "ref"))
        store.saveClientId("client_123")

        val service = TraktScrobbleService(context, "test_ns", traktStore = store)

        // Movie with TMDB ID -> allowed
        val movieWithTmdb = VodPlaybackRequest(
            key = "m1", kind = VodProgressKind.MOVIE, sourceId = "src", itemId = "100",
            title = "Inception", posterUrl = null, containerExtension = "mp4", streamUrl = "http://test",
            tmdbId = "27205"
        )
        assertTrue(service.canScrobble(movieWithTmdb))

        // Movie with IMDB ID -> allowed
        val movieWithImdb = VodPlaybackRequest(
            key = "m2", kind = VodProgressKind.MOVIE, sourceId = "src", itemId = "101",
            title = "Inception", posterUrl = null, containerExtension = "mp4", streamUrl = "http://test",
            imdbId = "tt1375666"
        )
        assertTrue(service.canScrobble(movieWithImdb))

        // Movie without external IDs -> rejected in Release 1
        val movieNoId = VodPlaybackRequest(
            key = "m3", kind = VodProgressKind.MOVIE, sourceId = "src", itemId = "102",
            title = "Inception", posterUrl = null, containerExtension = "mp4", streamUrl = "http://test"
        )
        assertFalse(service.canScrobble(movieNoId))

        // Episode with show ID and season/episode -> allowed
        val validEpisode = VodPlaybackRequest(
            key = "e1", kind = VodProgressKind.EPISODE, sourceId = "src", itemId = "200",
            title = "Pilot", seriesName = "Breaking Bad", posterUrl = null, containerExtension = "mp4", streamUrl = "http://test",
            tmdbId = "1396", season = 1, episode = 1
        )
        assertTrue(service.canScrobble(validEpisode))

        // Episode missing season or episode -> rejected
        val episodeMissingSeason = validEpisode.copy(season = null)
        assertFalse(service.canScrobble(episodeMissingSeason))
        val episodeMissingNumber = validEpisode.copy(episode = null)
        assertFalse(service.canScrobble(episodeMissingNumber))

        // Episode missing show ID -> rejected
        val episodeNoShowId = validEpisode.copy(tmdbId = null, imdbId = null)
        assertFalse(service.canScrobble(episodeNoShowId))

        // Catchup -> rejected
        val catchupRequest = movieWithTmdb.copy(kind = VodProgressKind.CATCHUP)
        assertFalse(service.canScrobble(catchupRequest))
    }

    @Test
    fun canScrobbleRejectsWhenDisconnectedOrDisabled() {
        val context = MockContext()
        val store = TraktSettingsStore(context, "test_ns")
        val service = TraktScrobbleService(context, "test_ns", traktStore = store)

        val movie = VodPlaybackRequest(
            key = "m1", kind = VodProgressKind.MOVIE, sourceId = "src", itemId = "100",
            title = "Inception", posterUrl = null, containerExtension = "mp4", streamUrl = "http://test",
            tmdbId = "27205"
        )

        // Disconnected -> false
        assertFalse(service.canScrobble(movie))

        // Connected but scrobble toggle disabled -> false
        store.saveToken(TraktToken("acc", "ref"))
        store.setScrobbleEnabled(false)
        assertFalse(service.canScrobble(movie))

        // Enabled -> true
        store.setScrobbleEnabled(true)
        assertTrue(service.canScrobble(movie))
    }

    @Test
    fun buildScrobblePayloadMovieMatchesTraktSpecification() {
        val request = VodPlaybackRequest(
            key = "m1", kind = VodProgressKind.MOVIE, sourceId = "src", itemId = "100",
            title = "Interstellar", posterUrl = null, containerExtension = "mp4", streamUrl = "http://test",
            tmdbId = "157336", imdbId = "tt0816692"
        )

        val payload = TraktScrobbleService.buildScrobblePayload(request, 85.34f)
        assertEquals(85.3, payload.getDouble("progress"), 0.01)

        val movie = payload.getJSONObject("movie")
        assertEquals("Interstellar", movie.getString("title"))

        val ids = movie.getJSONObject("ids")
        assertEquals(157336, ids.getInt("tmdb"))
        assertEquals("tt0816692", ids.getString("imdb"))
    }

    @Test
    fun buildScrobblePayloadEpisodeMatchesTraktSpecification() {
        val request = VodPlaybackRequest(
            key = "e1", kind = VodProgressKind.EPISODE, sourceId = "src", itemId = "200",
            title = "Ozymandias", seriesName = "Breaking Bad", posterUrl = null, containerExtension = "mp4", streamUrl = "http://test",
            tmdbId = "1396", imdbId = "tt0903747", season = 5, episode = 14
        )

        val payload = TraktScrobbleService.buildScrobblePayload(request, 92.0f)
        assertEquals(92.0, payload.getDouble("progress"), 0.01)

        val show = payload.getJSONObject("show")
        assertEquals("Breaking Bad", show.getString("title"))
        assertEquals(1396, show.getJSONObject("ids").getInt("tmdb"))
        assertEquals("tt0903747", show.getJSONObject("ids").getString("imdb"))

        val episode = payload.getJSONObject("episode")
        assertEquals(5, episode.getInt("season"))
        assertEquals(14, episode.getInt("number"))
    }

    @Test
    fun debounceAndDeduplicationPreventsSpuriousNetworkCalls() = runBlocking {
        val context = MockContext()
        val store = TraktSettingsStore(context, "test_ns")
        store.saveToken(TraktToken("acc", "ref"))
        store.saveClientId("client_123")

        val startCalls = AtomicInteger(0)
        val pauseCalls = AtomicInteger(0)
        val stopCalls = AtomicInteger(0)

        val mockClient = object : TraktClient() {
            override fun scrobble(action: String, payload: JSONObject, accessToken: String, clientId: String): TraktAuthResult<JSONObject> {
                when (action) {
                    "start" -> startCalls.incrementAndGet()
                    "pause" -> pauseCalls.incrementAndGet()
                    "stop" -> stopCalls.incrementAndGet()
                }
                return TraktAuthResult.Success(JSONObject())
            }
        }

        val service = TraktScrobbleService(
            context = context,
            profileNamespace = "test_ns",
            traktStore = store,
            client = mockClient,
            scope = this,
            debounceDelayMs = 40L
        )

        val request = VodPlaybackRequest(
            key = "m1", kind = VodProgressKind.MOVIE, sourceId = "src", itemId = "100",
            title = "Interstellar", posterUrl = null, containerExtension = "mp4", streamUrl = "http://test",
            tmdbId = "157336"
        )

        // 1. Initial start
        service.onPlaybackStarted(10_000L, 100_000L, request)
        delay(30L)
        assertEquals(1, startCalls.get())

        // 2. Rapid duplicate start (e.g. from ExoPlayer track selection or orientation change)
        service.onPlaybackStarted(11_000L, 100_000L, request)
        delay(30L)
        assertEquals(1, startCalls.get()) // Deduplicated!

        // 3. Pause with isBuffering=true -> ignored completely
        service.onPlaybackPaused(15_000L, 100_000L, request, isBuffering = true)
        delay(60L)
        assertEquals(0, pauseCalls.get())

        // 4. User pause with quick resume before debounce (e.g. scrubbing)
        service.onPlaybackPaused(20_000L, 100_000L, request, isBuffering = false)
        delay(15L) // Wait only 15ms (debounce is 40ms)
        service.onPlaybackStarted(25_000L, 100_000L, request) // Resumes playback!
        delay(60L)
        assertEquals(0, pauseCalls.get()) // Pause was cancelled by resume!

        // 5. Sustained pause (> 40ms)
        service.onPlaybackPaused(30_000L, 100_000L, request, isBuffering = false)
        delay(70L)
        assertEquals(1, pauseCalls.get()) // Dispatched!

        // 6. Stop
        service.onPlaybackStopped(85_000L, 100_000L, request)
        delay(30L)
        assertEquals(1, stopCalls.get())
    }
}

package com.fortresstv.player.trakt

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class TraktSettingsStoreTest {

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
    fun profileIsolationEnsuresIndependentTokens() {
        val context = MockContext()
        val adultStore = TraktSettingsStore(context, "household_1_adult")
        val kidStore = TraktSettingsStore(context, "household_1_kid")

        val tokenAdult = TraktToken(
            accessToken = "adult_access_token",
            refreshToken = "adult_refresh_token",
            expiresInSeconds = 7776000L
        )
        val profileAdult = TraktUserProfile(
            username = "adult_user",
            name = "Adult User",
            isVip = true,
            avatarUrl = null
        )

        adultStore.saveConnection(tokenAdult, profileAdult)

        assertTrue(adultStore.isConnected())
        assertFalse(kidStore.isConnected())
        assertEquals("adult_access_token", adultStore.loadToken()?.accessToken)
        assertNull(kidStore.loadToken())
        assertEquals("adult_user", adultStore.loadProfile()?.username)
        assertNull(kidStore.loadProfile())

        // Clearing adult store doesn't affect kid store
        adultStore.clearConnection()
        assertFalse(adultStore.isConnected())
        assertNull(adultStore.loadToken())
    }

    @Test
    fun tokenExpiryWithBufferDetection() {
        val now = System.currentTimeMillis()

        // Token expiring in 10 minutes (600s)
        val freshToken = TraktToken(
            accessToken = "fresh_acc",
            refreshToken = "fresh_ref",
            createdAtEpochMs = now,
            expiresInSeconds = 600L
        )
        // With 5-minute buffer (300s), freshToken should NOT be expired
        assertFalse(freshToken.isExpired(bufferSeconds = 300L))
        // With 15-minute buffer (900s), it SHOULD be treated as expired
        assertTrue(freshToken.isExpired(bufferSeconds = 900L))

        // Already expired token
        val expiredToken = TraktToken(
            accessToken = "exp_acc",
            refreshToken = "exp_ref",
            createdAtEpochMs = now - 10_000L,
            expiresInSeconds = 5L
        )
        assertTrue(expiredToken.isExpired())
    }

    @Test
    fun concurrentTokenRefreshIsSerializedAndInvokesNetworkOnlyOnce() = runBlocking {
        val context = MockContext()
        val store = TraktSettingsStore(context, "test_profile")

        val now = System.currentTimeMillis()
        // Save an expired token
        val initialToken = TraktToken(
            accessToken = "initial_expired_acc",
            refreshToken = "initial_single_use_refresh",
            createdAtEpochMs = now - 20_000L,
            expiresInSeconds = 10L
        )
        store.saveToken(initialToken)

        val refreshCallCount = AtomicInteger(0)

        // Mock client that simulates network latency and counts refresh calls
        val mockClient = object : TraktClient() {
            override fun refreshToken(refreshToken: String, clientId: String): TraktAuthResult<TraktToken> {
                refreshCallCount.incrementAndGet()
                // Ensure a brief delay to simulate network transfer
                Thread.sleep(30)
                return TraktAuthResult.Success(
                    TraktToken(
                        accessToken = "newly_refreshed_access_token",
                        refreshToken = "newly_issued_single_use_refresh_token",
                        createdAtEpochMs = System.currentTimeMillis(),
                        expiresInSeconds = 7776000L
                    )
                )
            }
        }

        // Launch 10 concurrent requests to get valid access token
        val deferredList = List(10) {
            async {
                store.getValidAccessToken("test_client_id", mockClient, bufferSeconds = 300L)
            }
        }

        val results = deferredList.awaitAll()

        // Network refresh MUST be called exactly once
        assertEquals(1, refreshCallCount.get())

        // All 10 callers must have received the refreshed access token
        for (token in results) {
            assertEquals("newly_refreshed_access_token", token)
        }

        // Saved token in store must be updated with replacement access and refresh token
        val saved = store.loadToken()
        assertNotNull(saved)
        assertEquals("newly_refreshed_access_token", saved?.accessToken)
        assertEquals("newly_issued_single_use_refresh_token", saved?.refreshToken)
        assertFalse(saved!!.isExpired())
    }
}

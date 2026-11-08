package com.fortresstv.player.telemetry

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryStoreTest {

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
                if (clear) {
                    values.clear()
                    clear = false
                }
                for (key in toRemove) {
                    values.remove(key)
                }
                values.putAll(temp)
                toRemove.clear()
                temp.clear()
            }
        }
    }

    @Test
    fun enqueueAndRetrieveBatch() {
        val prefs = TestSharedPreferences()
        val store = TelemetryStore(prefsProvider = { prefs }, telemetryEnabledProvider = { true })

        val item1 = PlaybackHealthSummary.createForTest(sessionId = "s1")
        val item2 = PlaybackHealthSummary.createForTest(sessionId = "s2")

        store.enqueue(item1)
        store.enqueue(item2)

        assertEquals(2, store.size())
        val batch = store.pendingBatch(10)
        assertEquals(2, batch.size)
        assertEquals("s1", batch[0].sessionId)
        assertEquals("s2", batch[1].sessionId)
    }

    @Test
    fun fifoRetentionEnforcesMaximumLimitAndDropsOldest() {
        val prefs = TestSharedPreferences()
        val store = TelemetryStore(prefsProvider = { prefs }, telemetryEnabledProvider = { true })

        // Enqueue 550 items (exceeding MAX_RETENTION_ENTRIES = 500)
        for (i in 1..550) {
            store.enqueue(PlaybackHealthSummary.createForTest(sessionId = "session-$i"))
        }

        assertEquals(TelemetryStore.MAX_RETENTION_ENTRIES, store.size())
        val batch = store.pendingBatch(10)
        // Oldest 50 were dropped, so first in queue should now be session-51
        assertEquals("session-51", batch.first().sessionId)
    }

    @Test
    fun acknowledgeRemovesOnlySpecifiedIds() {
        val prefs = TestSharedPreferences()
        val store = TelemetryStore(prefsProvider = { prefs }, telemetryEnabledProvider = { true })

        val s1 = PlaybackHealthSummary.createForTest(sessionId = "s1")
        val s2 = PlaybackHealthSummary.createForTest(sessionId = "s2")
        val s3 = PlaybackHealthSummary.createForTest(sessionId = "s3")

        store.enqueue(s1)
        store.enqueue(s2)
        store.enqueue(s3)

        store.acknowledge(listOf("s1", "s2"))

        assertEquals(1, store.size())
        val remaining = store.pendingBatch(10)
        assertEquals(1, remaining.size)
        assertEquals("s3", remaining.first().sessionId)
    }

    @Test
    fun clearEmptiesEntireQueue() {
        val prefs = TestSharedPreferences()
        val store = TelemetryStore(prefsProvider = { prefs }, telemetryEnabledProvider = { true })

        store.enqueue(PlaybackHealthSummary.createForTest(sessionId = "s1"))
        store.enqueue(PlaybackHealthSummary.createForTest(sessionId = "s2"))
        assertEquals(2, store.size())

        store.clear()
        assertEquals(0, store.size())
        assertTrue(store.pendingBatch().isEmpty())
    }

    @Test
    fun optOutPreventsEnqueue() {
        val prefs = TestSharedPreferences()
        var telemetryEnabled = false
        val store = TelemetryStore(
            prefsProvider = { prefs },
            telemetryEnabledProvider = { telemetryEnabled }
        )

        store.enqueue(PlaybackHealthSummary.createForTest(sessionId = "opted-out-session"))
        assertEquals(0, store.size())
        assertTrue(store.pendingBatch().isEmpty())

        // Re-enable and verify enqueue works again
        telemetryEnabled = true
        store.enqueue(PlaybackHealthSummary.createForTest(sessionId = "opted-in-session"))
        assertEquals(1, store.size())
    }
}

package com.fortresstv.player.telemetry

import android.content.Context
import android.content.SharedPreferences
import com.fortresstv.player.source.SourceSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local FIFO queue for [PlaybackHealthSummary] items awaiting cloud sync.
 *
 * Implements strict bounded retention (max 500 entries, dropping oldest on overflow)
 * and thread-safe persistence using SharedPreferences-backed JSON storage.
 */
class TelemetryStore internal constructor(
    private val prefsProvider: () -> SharedPreferences,
    private val telemetryEnabledProvider: () -> Boolean = { true }
) {
    companion object {
        private const val PREFS_NAME = "fortress_playback_telemetry"
        private const val KEY_PENDING_QUEUE = "pending_summaries"
        const val MAX_RETENTION_ENTRIES = 500

        @Volatile
        private var instance: TelemetryStore? = null

        fun get(context: Context): TelemetryStore = instance ?: synchronized(this) {
            instance ?: TelemetryStore(
                prefsProvider = {
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                },
                telemetryEnabledProvider = {
                    SourceSettingsStore(context.applicationContext).telemetryEnabled()
                }
            ).also { instance = it }
        }
    }

    constructor(context: Context) : this(
        prefsProvider = {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        },
        telemetryEnabledProvider = {
            SourceSettingsStore(context.applicationContext).telemetryEnabled()
        }
    )

    private val lock = Any()
    private val preferences by lazy { prefsProvider() }

    /**
     * Enqueues a playback summary. No-op if user disabled telemetry in settings.
     * Drops oldest entries if queue size exceeds [MAX_RETENTION_ENTRIES].
     */
    fun enqueue(summary: PlaybackHealthSummary) {
        if (!telemetryEnabledProvider()) return
        synchronized(lock) {
            val list = loadAllInternal().toMutableList()
            // Deduplicate if session already exists
            list.removeAll { it.sessionId == summary.sessionId }
            list.add(summary)
            // Cap at MAX_RETENTION_ENTRIES (keep most recent)
            val bounded = if (list.size > MAX_RETENTION_ENTRIES) {
                list.takeLast(MAX_RETENTION_ENTRIES)
            } else {
                list
            }
            saveAllInternal(bounded)
        }
    }

    /**
     * Non-blocking coroutine variant of enqueue.
     */
    suspend fun enqueueAsync(summary: PlaybackHealthSummary) = withContext(Dispatchers.IO) {
        enqueue(summary)
    }

    /**
     * Returns up to [limit] oldest pending summaries awaiting sync.
     */
    fun pendingBatch(limit: Int = 50): List<PlaybackHealthSummary> = synchronized(lock) {
        loadAllInternal().take(limit)
    }

    /**
     * Acknowledges successfully synced summaries by removing them from local storage.
     */
    fun acknowledge(sessionIds: List<String>) {
        if (sessionIds.isEmpty()) return
        synchronized(lock) {
            val toRemove = sessionIds.toSet()
            val remaining = loadAllInternal().filterNot { toRemove.contains(it.sessionId) }
            saveAllInternal(remaining)
        }
    }

    /**
     * Clears all pending telemetry data.
     */
    fun clear() {
        synchronized(lock) {
            preferences.edit().remove(KEY_PENDING_QUEUE).apply()
        }
    }

    /**
     * Returns current count of queued items.
     */
    fun size(): Int = synchronized(lock) {
        loadAllInternal().size
    }

    private fun loadAllInternal(): List<PlaybackHealthSummary> {
        val raw = preferences.getString(KEY_PENDING_QUEUE, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val result = ArrayList<PlaybackHealthSummary>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                result.add(PlaybackHealthSummary.fromJson(obj))
            }
            result
        }.getOrDefault(emptyList())
    }

    private fun saveAllInternal(items: List<PlaybackHealthSummary>) {
        val array = JSONArray()
        for (item in items) {
            array.put(item.toJson())
        }
        preferences.edit().putString(KEY_PENDING_QUEUE, array.toString()).apply()
    }
}

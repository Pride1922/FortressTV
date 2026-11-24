package com.fortresstv.player.telemetry

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal const val MAX_CHANNEL_QUALITY_ENTRIES = 500

internal fun mergeChannelQualities(
    existing: Map<String, MeasuredVideoQuality>, incoming: Map<String, MeasuredVideoQuality>
): Map<String, MeasuredVideoQuality> {
    val merged = existing.toMutableMap()
    incoming.forEach { (variant, quality) ->
        if (validStreamVariantId(variant) != null && quality.observedAtEpochMs > (merged[variant]?.observedAtEpochMs ?: 0L)) {
            merged[variant] = quality
        }
    }
    return merged.entries.sortedByDescending { it.value.observedAtEpochMs }
        .take(MAX_CHANNEL_QUALITY_ENTRIES).associate { it.key to it.value }
}

/** Bounded replaceable cache, isolated by household/profile. All disk work runs off the UI thread. */
internal class ChannelQualityStore(private val prefsProvider: () -> SharedPreferences) {
    constructor(context: Context, namespace: String) : this({
        context.applicationContext.getSharedPreferences("channel_quality_$namespace", Context.MODE_PRIVATE)
    })

    private val preferences by lazy { prefsProvider() }
    private val mutableSnapshots = MutableStateFlow<Map<String, MeasuredVideoQuality>>(emptyMap())
    val snapshots = mutableSnapshots.asStateFlow()

    suspend fun observe(): Nothing = withContext(Dispatchers.IO) {
        coroutineScope {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "observations") launch { refresh() }
            }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            try {
                refresh()
                awaitCancellation()
            } finally {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
    }

    private fun refresh() = synchronized(preferences) { mutableSnapshots.value = readPersisted() }

    /** Call on an IO dispatcher. SharedPreferences is also the lock across sync/UI store instances. */
    fun mergeObservations(incoming: Map<String, MeasuredVideoQuality>) = synchronized(preferences) {
        val merged = mergeChannelQualities(readPersisted(), incoming)
        val array = JSONArray()
        merged.forEach { (variant, quality) ->
            array.put(JSONObject().put("stream_variant_id", variant).put("video_width", quality.width)
                .put("video_height", quality.height).put("quality_observed_at_ms", quality.observedAtEpochMs))
        }
        preferences.edit().putString("observations", array.toString()).apply()
        mutableSnapshots.value = merged
    }

    private fun readPersisted(): Map<String, MeasuredVideoQuality> = runCatching {
        val array = JSONArray(preferences.getString("observations", null) ?: "[]")
        val result = mutableMapOf<String, MeasuredVideoQuality>()
        for (i in 0 until array.length()) {
            val row = array.optJSONObject(i) ?: continue
            val variant = validStreamVariantId(row.optString("stream_variant_id")) ?: continue
            val quality = MeasuredVideoQuality.fromJson(row) ?: continue
            if (quality.observedAtEpochMs > (result[variant]?.observedAtEpochMs ?: 0L)) result[variant] = quality
        }
        mergeChannelQualities(emptyMap(), result)
    }.getOrDefault(emptyMap())
}

internal val LocalChannelQualityStore = staticCompositionLocalOf<ChannelQualityStore?> { null }

package com.fortresstv.player.trakt

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class TraktRecommendationsStore(
    context: Context,
    val profileNamespace: String,
    private val customPrefs: SharedPreferences? = null
) {
    private val preferences: SharedPreferences = customPrefs ?: context.getSharedPreferences(
        "fortress_trakt_recommendations_${sanitizeNamespace(profileNamespace)}",
        Context.MODE_PRIVATE
    )

    fun loadRecommendations(): List<TraktRecommendationItem> {
        val raw = preferences.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = ArrayList<TraktRecommendationItem>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                TraktRecommendationItem.fromJson(obj)?.let { list.add(it) }
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun saveRecommendations(items: List<TraktRecommendationItem>) {
        val array = JSONArray()
        for (item in items) {
            array.put(item.toJson())
        }
        preferences.edit()
            .putString(KEY_ITEMS, array.toString())
            .putLong(KEY_LAST_SYNCED, System.currentTimeMillis())
            .apply()
    }

    fun dismissRecommendation(item: TraktRecommendationItem) {
        val current = loadRecommendations().toMutableList()
        current.removeAll { existing ->
            existing.kind == item.kind && (
                (item.traktId != null && existing.traktId == item.traktId) ||
                (!item.tmdbId.isNullOrBlank() && existing.tmdbId == item.tmdbId) ||
                (!item.imdbId.isNullOrBlank() && existing.imdbId.equals(item.imdbId, ignoreCase = true)) ||
                (existing.title.equals(item.title, ignoreCase = true) && existing.year == item.year)
            )
        }
        val array = JSONArray()
        for (i in current) {
            array.put(i.toJson())
        }
        preferences.edit()
            .putString(KEY_ITEMS, array.toString())
            .apply()
    }

    fun lastSyncedEpochMs(): Long = preferences.getLong(KEY_LAST_SYNCED, 0L)

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_ITEMS = "recommendation_items"
        private const val KEY_LAST_SYNCED = "last_synced_epoch_ms"

        private fun sanitizeNamespace(ns: String): String {
            return ns.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        }
    }
}

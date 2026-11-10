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

    fun loadRecommendations(): List<TraktRecommendationItem> = loadItems(KEY_ITEMS)

    private fun loadItems(key: String): List<TraktRecommendationItem> {
        val raw = preferences.getString(key, null) ?: return emptyList()
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

    fun saveRecommendations(items: List<TraktRecommendationItem>, trendingFallback: Boolean) = synchronized(preferences) {
        val hidden = loadItems(KEY_HIDDEN)
        val visible = if (trendingFallback) items.filterNot { item -> hidden.any { it.matches(item) } } else items
        val stillHidden = if (trendingFallback) hidden else hidden.filterNot { item -> items.any { it.matches(item) } }
        preferences.edit()
            .putString(KEY_ITEMS, encode(visible))
            .putString(KEY_HIDDEN, encode(stillHidden))
            .putLong(KEY_LAST_SYNCED, System.currentTimeMillis())
            .apply()
    }

    fun dismissRecommendation(item: TraktRecommendationItem) = synchronized(preferences) {
        preferences.edit()
            .putString(KEY_ITEMS, encode(loadRecommendations().filterNot { it.matches(item) }))
            .putString(KEY_HIDDEN, encode(loadItems(KEY_HIDDEN).filterNot { it.matches(item) } + item))
            .apply()
    }

    fun restoreRecommendation(item: TraktRecommendationItem) = synchronized(preferences) {
        preferences.edit()
            .putString(KEY_ITEMS, encode(listOf(item) + loadRecommendations().filterNot { it.matches(item) }))
            .putString(KEY_HIDDEN, encode(loadItems(KEY_HIDDEN).filterNot { it.matches(item) }))
            .apply()
    }

    private fun encode(items: List<TraktRecommendationItem>): String =
        JSONArray().apply { items.forEach { put(it.toJson()) } }.toString()

    fun lastSyncedEpochMs(): Long = preferences.getLong(KEY_LAST_SYNCED, 0L)

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_ITEMS = "recommendation_items"
        private const val KEY_HIDDEN = "hidden_recommendation_items"
        private const val KEY_LAST_SYNCED = "last_synced_epoch_ms"

        private fun sanitizeNamespace(ns: String): String {
            return ns.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        }
    }
}

private fun TraktRecommendationItem.matches(other: TraktRecommendationItem): Boolean {
    if (kind != other.kind) return false
    if (traktId != null && other.traktId != null) return traktId == other.traktId
    if (!tmdbId.isNullOrBlank() && !other.tmdbId.isNullOrBlank()) return tmdbId == other.tmdbId
    if (!imdbId.isNullOrBlank() && !other.imdbId.isNullOrBlank()) return imdbId.equals(other.imdbId, ignoreCase = true)
    if (!slug.isNullOrBlank() && !other.slug.isNullOrBlank()) return slug == other.slug
    return title.equals(other.title, ignoreCase = true) && year == other.year
}

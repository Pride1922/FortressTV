package com.fortresstv.player.trakt

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class TraktWatchlistStore(
    context: Context,
    val profileNamespace: String,
    private val customPrefs: SharedPreferences? = null
) {
    private val preferences: SharedPreferences = customPrefs ?: context.getSharedPreferences(
        "fortress_trakt_watchlist_${sanitizeNamespace(profileNamespace)}",
        Context.MODE_PRIVATE
    )

    fun loadWatchlist(): List<TraktWatchlistItem> {
        val raw = preferences.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = ArrayList<TraktWatchlistItem>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                TraktWatchlistItem.fromJson(obj)?.let { list.add(it) }
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun saveWatchlist(items: List<TraktWatchlistItem>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("type", if (item.kind == TraktWatchlistKind.MOVIE) "movie" else "show")
                put("title", item.title)
                item.year?.let { put("year", it) }
                item.listedAt?.let { put("listed_at", it) }
                val ids = JSONObject()
                item.tmdbId?.let { ids.put("tmdb", it) }
                item.imdbId?.let { ids.put("imdb", it) }
                item.traktId?.let { ids.put("trakt", it) }
                item.slug?.let { ids.put("slug", it) }
                put("ids", ids)
            }
            array.put(obj)
        }
        preferences.edit()
            .putString(KEY_ITEMS, array.toString())
            .putLong(KEY_LAST_SYNCED, System.currentTimeMillis())
            .apply()
    }

    fun lastSyncedEpochMs(): Long = preferences.getLong(KEY_LAST_SYNCED, 0L)

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_ITEMS = "watchlist_items"
        private const val KEY_LAST_SYNCED = "last_synced_epoch_ms"

        private fun sanitizeNamespace(ns: String): String {
            return ns.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        }
    }
}

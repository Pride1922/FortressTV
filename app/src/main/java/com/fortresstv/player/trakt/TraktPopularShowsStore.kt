package com.fortresstv.player.trakt

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

open class TraktPopularStore(
    context: Context,
    val profileNamespace: String,
    private val kind: TraktWatchlistKind,
    private val customPrefs: SharedPreferences? = null
) {
    private val preferences: SharedPreferences = customPrefs ?: context.getSharedPreferences(
        prefsName(kind, profileNamespace),
        Context.MODE_PRIVATE
    )

    fun loadItems(): List<TraktRecommendationItem> {
        val raw = preferences.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = ArrayList<TraktRecommendationItem>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                TraktRecommendationItem.fromJson(obj, kind)?.let { list.add(it) }
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun saveItems(items: List<TraktRecommendationItem>, syncedAtEpochMs: Long = System.currentTimeMillis()) =
        synchronized(preferences) {
            val encoded = JSONArray().apply { items.forEach { put(it.toJson()) } }.toString()
            preferences.edit()
                .putString(KEY_ITEMS, encoded)
                .putLong(KEY_LAST_SYNCED, syncedAtEpochMs)
                .apply()
        }

    fun lastSyncedEpochMs(): Long = preferences.getLong(KEY_LAST_SYNCED, 0L)

    fun isCacheFresh(nowEpochMs: Long = System.currentTimeMillis(), ttlMs: Long = DEFAULT_TTL_MS): Boolean {
        val lastSynced = lastSyncedEpochMs()
        if (lastSynced <= 0L) return false
        return nowEpochMs - lastSynced in 0 until ttlMs
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_ITEMS = "popular_show_items"
        private const val KEY_LAST_SYNCED = "last_synced_epoch_ms"
        const val DEFAULT_TTL_MS = 6L * 60L * 60L * 1000L

        private fun prefsName(kind: TraktWatchlistKind, ns: String): String {
            val suffix = sanitizeNamespace(ns)
            return when (kind) {
                TraktWatchlistKind.SHOW -> "fortress_trakt_popular_shows_$suffix"
                TraktWatchlistKind.MOVIE -> "fortress_trakt_popular_movies_$suffix"
            }
        }

        private fun sanitizeNamespace(ns: String): String =
            ns.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }
}

class TraktPopularShowsStore(
    context: Context,
    profileNamespace: String,
    customPrefs: SharedPreferences? = null
) : TraktPopularStore(context, profileNamespace, TraktWatchlistKind.SHOW, customPrefs) {
    fun loadPopularShows(): List<TraktRecommendationItem> = loadItems()
    fun savePopularShows(items: List<TraktRecommendationItem>, syncedAtEpochMs: Long = System.currentTimeMillis()) =
        saveItems(items, syncedAtEpochMs)
}

class TraktPopularMoviesStore(
    context: Context,
    profileNamespace: String,
    customPrefs: SharedPreferences? = null
) : TraktPopularStore(context, profileNamespace, TraktWatchlistKind.MOVIE, customPrefs) {
    fun loadPopularMovies(): List<TraktRecommendationItem> = loadItems()
    fun savePopularMovies(items: List<TraktRecommendationItem>, syncedAtEpochMs: Long = System.currentTimeMillis()) =
        saveItems(items, syncedAtEpochMs)
}

package com.fortresstv.player.trakt

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class TraktWatchedStore(
    context: Context,
    val profileNamespace: String,
    private val customPrefs: SharedPreferences? = null
) {
    private val preferences: SharedPreferences = customPrefs ?: context.getSharedPreferences(
        "fortress_trakt_watched_${sanitizeNamespace(profileNamespace)}",
        Context.MODE_PRIVATE
    )

    fun loadWatchedMovies(): List<TraktWatchedMovie> {
        val raw = preferences.getString(KEY_WATCHED_MOVIES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = ArrayList<TraktWatchedMovie>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                TraktWatchedMovie.fromJson(obj)?.let { list.add(it) }
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun saveWatchedMovies(items: List<TraktWatchedMovie>) {
        val array = JSONArray()
        for (item in items) {
            array.put(item.toJson())
        }
        preferences.edit()
            .putString(KEY_WATCHED_MOVIES, array.toString())
            .apply()
    }

    fun loadWatchedShows(): List<TraktWatchedShow> {
        val raw = preferences.getString(KEY_WATCHED_SHOWS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = ArrayList<TraktWatchedShow>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                TraktWatchedShow.fromJson(obj)?.let { list.add(it) }
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun saveWatchedShows(items: List<TraktWatchedShow>) {
        val array = JSONArray()
        for (item in items) {
            array.put(item.toJson())
        }
        preferences.edit()
            .putString(KEY_WATCHED_SHOWS, array.toString())
            .apply()
    }

    fun addWatchedMovie(movie: TraktWatchedMovie) {
        val current = loadWatchedMovies().toMutableList()
        current.removeAll { isMovieMatch(it, movie.title, movie.year, movie.tmdbId, movie.imdbId) }
        current.add(0, movie)
        saveWatchedMovies(current)
    }

    fun removeWatchedMovie(title: String, year: Int?, tmdbId: String?, imdbId: String?) {
        val current = loadWatchedMovies().toMutableList()
        current.removeAll { isMovieMatch(it, title, year, tmdbId, imdbId) }
        saveWatchedMovies(current)
    }

    fun addWatchedEpisode(
        seriesTitle: String,
        seriesYear: Int?,
        seriesTmdbId: String?,
        seriesImdbId: String?,
        season: Int,
        episodeNumber: Int
    ) {
        val current = loadWatchedShows().toMutableList()
        val existingIndex = current.indexOfFirst {
            isShowMatch(it, seriesTitle, seriesYear, seriesTmdbId, seriesImdbId)
        }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            val seasonsMap = existing.seasons.mapValues { it.value.toMutableSet() }.toMutableMap()
            seasonsMap.getOrPut(season) { mutableSetOf() }.add(episodeNumber)
            current[existingIndex] = existing.copy(seasons = seasonsMap)
        } else {
            val seasonsMap = mapOf(season to setOf(episodeNumber))
            current.add(
                0,
                TraktWatchedShow(
                    title = seriesTitle,
                    year = seriesYear,
                    tmdbId = seriesTmdbId,
                    imdbId = seriesImdbId,
                    traktId = null,
                    seasons = seasonsMap
                )
            )
        }
        saveWatchedShows(current)
    }

    fun removeWatchedEpisode(
        seriesTitle: String,
        seriesYear: Int?,
        seriesTmdbId: String?,
        seriesImdbId: String?,
        season: Int,
        episodeNumber: Int
    ) {
        val current = loadWatchedShows().toMutableList()
        val existingIndex = current.indexOfFirst {
            isShowMatch(it, seriesTitle, seriesYear, seriesTmdbId, seriesImdbId)
        }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            val seasonsMap = existing.seasons.mapValues { it.value.toMutableSet() }.toMutableMap()
            seasonsMap[season]?.remove(episodeNumber)
            if (seasonsMap[season]?.isEmpty() == true) {
                seasonsMap.remove(season)
            }
            if (seasonsMap.isEmpty()) {
                current.removeAt(existingIndex)
            } else {
                current[existingIndex] = existing.copy(seasons = seasonsMap)
            }
            saveWatchedShows(current)
        }
    }

    fun lastSyncedEpochMs(): Long = preferences.getLong(KEY_LAST_SYNCED, 0L)

    fun setLastSyncedEpochMs(epochMs: Long) {
        preferences.edit().putLong(KEY_LAST_SYNCED, epochMs).apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun isMovieMatch(
        item: TraktWatchedMovie,
        title: String,
        year: Int?,
        tmdbId: String?,
        imdbId: String?
    ): Boolean {
        if (!tmdbId.isNullOrBlank() && !item.tmdbId.isNullOrBlank() && item.tmdbId == tmdbId) return true
        if (!imdbId.isNullOrBlank() && !item.imdbId.isNullOrBlank() && item.imdbId.equals(imdbId, ignoreCase = true)) return true
        return item.title.equals(title, ignoreCase = true) && item.year == year
    }

    private fun isShowMatch(
        item: TraktWatchedShow,
        title: String,
        year: Int?,
        tmdbId: String?,
        imdbId: String?
    ): Boolean {
        if (!tmdbId.isNullOrBlank() && !item.tmdbId.isNullOrBlank() && item.tmdbId == tmdbId) return true
        if (!imdbId.isNullOrBlank() && !item.imdbId.isNullOrBlank() && item.imdbId.equals(imdbId, ignoreCase = true)) return true
        return item.title.equals(title, ignoreCase = true) && item.year == year
    }

    companion object {
        private const val KEY_WATCHED_MOVIES = "watched_movies"
        private const val KEY_WATCHED_SHOWS = "watched_shows"
        private const val KEY_LAST_SYNCED = "last_synced_epoch_ms"

        private fun sanitizeNamespace(ns: String): String {
            return ns.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        }
    }
}

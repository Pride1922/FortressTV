package com.fortresstv.player.library

import android.content.Context
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.xtream.XtreamMovie
import com.fortresstv.player.xtream.XtreamSeries

import android.content.SharedPreferences

class UserLibraryStore internal constructor(
    private val prefsProvider: (String) -> SharedPreferences,
    initialNamespace: String = "local"
) {
    constructor(context: Context, initialNamespace: String = "local") : this(
        prefsProvider = { ns ->
            if (ns == "local" || ns == "default") {
                context.getSharedPreferences("fortress_user_library", Context.MODE_PRIVATE)
            } else {
                context.getSharedPreferences("fortress_user_library_$ns", Context.MODE_PRIVATE)
            }
        },
        initialNamespace = initialNamespace
    )

    private var namespace = sanitize(initialNamespace)
    private var preferences: SharedPreferences = prefsProvider(namespace)

    private fun sanitize(raw: String): String = raw.replace(Regex("[^a-zA-Z0-9_-]"), "_")

    fun switchNamespace(newNamespace: String) {
        namespace = sanitize(newNamespace)
        preferences = prefsProvider(namespace)
    }

    fun clearForRestore() {
        preferences.edit().clear().apply()
    }

    fun favoriteChannelIds(): Set<String> =
        preferences.getStringSet(KEY_FAVORITE_CHANNELS, emptySet()).orEmpty().toSet()

    fun setChannelFavorite(channel: PlaylistChannel, favorite: Boolean): Set<String> {
        val id = channel.libraryId() ?: return favoriteChannelIds()
        val hashedKey = if (channel.sourceId.isNotBlank() && channel.sourceItemId.isNotBlank()) {
            "channel:${channel.sourceId}:${CloudIdentityHasher.hashItemId(channel.sourceItemId)}"
        } else null
        val updated = favoriteChannelIds().toMutableSet().apply {
            if (favorite) {
                add(id)
            } else {
                remove(id)
                hashedKey?.let { remove(it) }
            }
        }
        preferences.edit().putStringSet(KEY_FAVORITE_CHANNELS, updated).apply()
        return updated
    }

    fun isChannelFavorite(channel: PlaylistChannel, favorites: Set<String> = favoriteChannelIds()): Boolean {
        val id = channel.libraryId()
        if (id != null && favorites.contains(id)) return true
        if (channel.sourceId.isNotBlank() && channel.sourceItemId.isNotBlank()) {
            val hashedKey = "channel:${channel.sourceId}:${CloudIdentityHasher.hashItemId(channel.sourceItemId)}"
            if (favorites.contains(hashedKey)) return true
        }
        val hashOnly = CloudIdentityHasher.hashItemId(channel.sourceItemId)
        return favorites.any { it.endsWith(":$hashOnly") || it.endsWith(":${channel.sourceItemId}") }
    }

    fun movieWatchlistIds(): Set<String> =
        preferences.getStringSet(KEY_MOVIE_WATCHLIST, emptySet()).orEmpty().toSet()

    fun isMovieWatchlisted(sourceId: String, movie: XtreamMovie, watchlist: Set<String> = movieWatchlistIds()): Boolean {
        if (watchlist.contains(movie.libraryId(sourceId))) return true
        val hashedKey = "movie:$sourceId:${CloudIdentityHasher.hashItemId(movie.id.toString())}"
        if (watchlist.contains(hashedKey)) return true
        val hashOnly = CloudIdentityHasher.hashItemId(movie.id.toString())
        return watchlist.any { it.endsWith(":$hashOnly") || it.endsWith(":${movie.id}") }
    }

    fun setMovieWatchlisted(sourceId: String, movie: XtreamMovie, watchlisted: Boolean): Set<String> {
        val rawId = movie.libraryId(sourceId)
        val hashedKey = "movie:$sourceId:${CloudIdentityHasher.hashItemId(movie.id.toString())}"
        val updated = movieWatchlistIds().toMutableSet().apply {
            if (watchlisted) {
                add(rawId)
            } else {
                remove(rawId)
                remove(hashedKey)
            }
        }
        preferences.edit().putStringSet(KEY_MOVIE_WATCHLIST, updated).apply()
        return updated
    }

    fun followedSeriesIds(): Set<String> =
        preferences.getStringSet(KEY_FOLLOWED_SERIES, emptySet()).orEmpty().toSet()

    fun isSeriesFollowed(sourceId: String, series: XtreamSeries, followed: Set<String> = followedSeriesIds()): Boolean {
        if (followed.contains(series.libraryId(sourceId))) return true
        val hashedKey = "series:$sourceId:${CloudIdentityHasher.hashItemId(series.id.toString())}"
        if (followed.contains(hashedKey)) return true
        val hashOnly = CloudIdentityHasher.hashItemId(series.id.toString())
        return followed.any { it.endsWith(":$hashOnly") || it.endsWith(":${series.id}") }
    }

    fun setSeriesFollowed(sourceId: String, series: XtreamSeries, followed: Boolean): Set<String> {
        val rawId = series.libraryId(sourceId)
        val hashedKey = "series:$sourceId:${CloudIdentityHasher.hashItemId(series.id.toString())}"
        val updated = followedSeriesIds().toMutableSet().apply {
            if (followed) {
                add(rawId)
            } else {
                remove(rawId)
                remove(hashedKey)
            }
        }
        preferences.edit().putStringSet(KEY_FOLLOWED_SERIES, updated).apply()
        return updated
    }

    fun watchedItemIds(): Set<String> =
        preferences.getStringSet(KEY_WATCHED_ITEMS, emptySet()).orEmpty().toSet()

    fun setItemWatched(libraryId: String, watched: Boolean): Set<String> {
        val parts = libraryId.split(":")
        val hashedKey = if (parts.size >= 3) {
            "${parts[0]}:${parts[1]}:${CloudIdentityHasher.hashItemId(parts[2])}"
        } else null
        val updated = watchedItemIds().toMutableSet().apply {
            if (watched) {
                add(libraryId)
            } else {
                remove(libraryId)
                hashedKey?.let { remove(it) }
            }
        }
        preferences.edit().putStringSet(KEY_WATCHED_ITEMS, updated).apply()
        return updated
    }

    fun isItemWatched(libraryId: String, watched: Set<String> = watchedItemIds()): Boolean {
        if (watched.contains(libraryId)) return true
        val parts = libraryId.split(":")
        if (parts.size >= 3) {
            val hashedKey = "${parts[0]}:${parts[1]}:${CloudIdentityHasher.hashItemId(parts[2])}"
            if (watched.contains(hashedKey)) return true
        }
        return false
    }

    fun recentChannelIds(): List<String> {
        val raw = preferences.getString(KEY_RECENT_CHANNELS, null) ?: return emptyList()
        return raw.split("|").filter { it.isNotBlank() }
    }

    fun recordChannelWatched(channel: PlaylistChannel): List<String> {
        val id = channel.libraryId() ?: return recentChannelIds()
        val current = recentChannelIds().toMutableList()
        current.remove(id)
        current.add(0, id)
        val trimmed = current.take(20)
        preferences.edit().putString(KEY_RECENT_CHANNELS, trimmed.joinToString("|")).apply()
        return trimmed
    }

    fun clearRecentChannels(): List<String> {
        preferences.edit().remove(KEY_RECENT_CHANNELS).apply()
        return emptyList()
    }

    fun hiddenCategories(profileId: String): Set<String> {
        return preferences.getStringSet("${KEY_HIDDEN_CATEGORIES_PREFIX}_$profileId", emptySet()).orEmpty().toSet()
    }

    fun setCategoryHidden(profileId: String, group: String, hidden: Boolean): Set<String> {
        val updated = hiddenCategories(profileId).toMutableSet().apply {
            if (hidden) add(group) else remove(group)
        }
        preferences.edit().putStringSet("${KEY_HIDDEN_CATEGORIES_PREFIX}_$profileId", updated).apply()
        return updated
    }

    fun unhideAllCategories(profileId: String): Set<String> {
        preferences.edit().remove("${KEY_HIDDEN_CATEGORIES_PREFIX}_$profileId").apply()
        return emptySet()
    }

    fun hideAllCategories(profileId: String, allCategories: Collection<String>): Set<String> {
        val updated = allCategories.toSet()
        preferences.edit().putStringSet("${KEY_HIDDEN_CATEGORIES_PREFIX}_$profileId", updated).apply()
        return updated
    }

    fun categoryOrder(profileId: String): List<String> {
        val raw = preferences.getString("${KEY_CATEGORY_ORDER_PREFIX}_$profileId", null) ?: return emptyList()
        return raw.split("|").filter { it.isNotBlank() }
    }

    fun setCategoryOrder(profileId: String, order: List<String>): List<String> {
        preferences.edit().putString("${KEY_CATEGORY_ORDER_PREFIX}_$profileId", order.joinToString("|")).apply()
        return order
    }

    fun setHiddenCategories(profileId: String, hidden: Set<String>) {
        preferences.edit().putStringSet("${KEY_HIDDEN_CATEGORIES_PREFIX}_$profileId", hidden).apply()
    }

    fun setFavoriteChannelIds(ids: Set<String>) {
        preferences.edit().putStringSet(KEY_FAVORITE_CHANNELS, ids).apply()
    }

    fun setMovieWatchlistIds(ids: Set<String>) {
        preferences.edit().putStringSet(KEY_MOVIE_WATCHLIST, ids).apply()
    }

    fun setFollowedSeriesIds(ids: Set<String>) {
        preferences.edit().putStringSet(KEY_FOLLOWED_SERIES, ids).apply()
    }

    fun setWatchedItemIds(ids: Set<String>) {
        preferences.edit().putStringSet(KEY_WATCHED_ITEMS, ids).apply()
    }

    fun setRecentChannelIds(ids: List<String>) {
        preferences.edit().putString(KEY_RECENT_CHANNELS, ids.take(20).joinToString("|")).apply()
    }

    fun resetCategoryOrder(profileId: String): List<String> {
        preferences.edit().remove("${KEY_CATEGORY_ORDER_PREFIX}_$profileId").apply()
        return emptyList()
    }

    companion object {
        private const val KEY_FAVORITE_CHANNELS = "favorite_channel_ids"
        private const val KEY_RECENT_CHANNELS = "recent_channel_ids"
        private const val KEY_MOVIE_WATCHLIST = "movie_watchlist_ids"
        private const val KEY_FOLLOWED_SERIES = "followed_series_ids"
        private const val KEY_WATCHED_ITEMS = "watched_item_ids"
        private const val KEY_HIDDEN_CATEGORIES_PREFIX = "hidden_categories"
        private const val KEY_CATEGORY_ORDER_PREFIX = "category_order"

        fun PlaylistChannel.libraryId(): String? {
            if (sourceId.isBlank() || sourceItemId.isBlank()) return null
            return "channel:$sourceId:$sourceItemId"
        }

        fun XtreamMovie.libraryId(sourceId: String): String = "movie:$sourceId:$id"

        fun XtreamSeries.libraryId(sourceId: String): String = "series:$sourceId:$id"

        fun com.fortresstv.player.xtream.XtreamEpisode.libraryId(sourceId: String): String = "episode:$sourceId:$id"
    }
}

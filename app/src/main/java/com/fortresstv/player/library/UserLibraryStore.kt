package com.fortresstv.player.library

import android.content.Context
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.xtream.XtreamMovie
import com.fortresstv.player.xtream.XtreamSeries

class UserLibraryStore(context: Context) {
    private val preferences = context.getSharedPreferences("fortress_user_library", Context.MODE_PRIVATE)

    fun favoriteChannelIds(): Set<String> =
        preferences.getStringSet(KEY_FAVORITE_CHANNELS, emptySet()).orEmpty().toSet()

    fun setChannelFavorite(channel: PlaylistChannel, favorite: Boolean): Set<String> {
        val id = channel.libraryId()
        if (id == null) return favoriteChannelIds()
        val updated = favoriteChannelIds().toMutableSet().apply {
            if (favorite) add(id) else remove(id)
        }
        preferences.edit().putStringSet(KEY_FAVORITE_CHANNELS, updated).apply()
        return updated
    }

    fun isChannelFavorite(channel: PlaylistChannel, favorites: Set<String> = favoriteChannelIds()): Boolean =
        channel.libraryId()?.let(favorites::contains) == true

    fun movieWatchlistIds(): Set<String> =
        preferences.getStringSet(KEY_MOVIE_WATCHLIST, emptySet()).orEmpty().toSet()

    fun setMovieWatchlisted(sourceId: String, movie: XtreamMovie, watchlisted: Boolean): Set<String> {
        val updated = movieWatchlistIds().toMutableSet().apply {
            if (watchlisted) add(movie.libraryId(sourceId)) else remove(movie.libraryId(sourceId))
        }
        preferences.edit().putStringSet(KEY_MOVIE_WATCHLIST, updated).apply()
        return updated
    }

    fun followedSeriesIds(): Set<String> =
        preferences.getStringSet(KEY_FOLLOWED_SERIES, emptySet()).orEmpty().toSet()

    fun setSeriesFollowed(sourceId: String, series: XtreamSeries, followed: Boolean): Set<String> {
        val updated = followedSeriesIds().toMutableSet().apply {
            if (followed) add(series.libraryId(sourceId)) else remove(series.libraryId(sourceId))
        }
        preferences.edit().putStringSet(KEY_FOLLOWED_SERIES, updated).apply()
        return updated
    }

    companion object {
        private const val KEY_FAVORITE_CHANNELS = "favorite_channel_ids"
        private const val KEY_MOVIE_WATCHLIST = "movie_watchlist_ids"
        private const val KEY_FOLLOWED_SERIES = "followed_series_ids"

        fun PlaylistChannel.libraryId(): String? {
            if (sourceId.isBlank() || sourceItemId.isBlank()) return null
            return "channel:$sourceId:$sourceItemId"
        }

        fun XtreamMovie.libraryId(sourceId: String): String = "movie:$sourceId:$id"

        fun XtreamSeries.libraryId(sourceId: String): String = "series:$sourceId:$id"
    }
}

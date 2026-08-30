package com.fortresstv.player.library

import android.content.Context
import com.fortresstv.player.playlist.PlaylistChannel

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

    companion object {
        private const val KEY_FAVORITE_CHANNELS = "favorite_channel_ids"

        fun PlaylistChannel.libraryId(): String? {
            if (sourceId.isBlank() || sourceItemId.isBlank()) return null
            return "channel:$sourceId:$sourceItemId"
        }
    }
}

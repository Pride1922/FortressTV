package com.fortresstv.player.playlist

import com.fortresstv.player.source.ContentSource

data class PlaylistChannel(
    val position: Int,
    val name: String,
    val streamUrl: String,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val logoUrl: String? = null,
    val group: String? = null
)

data class Playlist(
    val channels: List<PlaylistChannel>
) {
    val groups: List<String> = channels.mapNotNull { it.group }.distinct()
}

data class PlaylistProfile(
    val id: String,
    val name: String,
    val source: ContentSource,
    val lastUpdated: Long = 0L,
    val channelCount: Int = 0
)

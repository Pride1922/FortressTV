package com.fortresstv.player.playlist

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

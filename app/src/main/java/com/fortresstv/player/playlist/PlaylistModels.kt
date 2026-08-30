package com.fortresstv.player.playlist

import com.fortresstv.player.source.ContentSource

data class PlaylistChannel(
    val position: Int,
    val name: String,
    val streamUrl: String,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val logoUrl: String? = null,
    val group: String? = null,
    val sourceId: String = "",
    val sourcePosition: Int = position
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
    val channelCount: Int = 0,
    val enabled: Boolean = true,
    val role: SourceRole = SourceRole.LIVE_TV,
    val order: Int = 0
)

enum class SourceRole {
    AUTOMATIC,
    LIVE_TV,
    MOVIES,
    SERIES,
    MOVIES_AND_SERIES,
    EVERYTHING;

    fun contributesLiveTv(): Boolean = this == AUTOMATIC || this == LIVE_TV || this == EVERYTHING
}

fun mergeLivePlaylists(
    profiles: List<PlaylistProfile>,
    loaded: Map<String, Playlist>
): Playlist {
    val channels = profiles
        .asSequence()
        .filter { it.enabled && it.role.contributesLiveTv() }
        .sortedWith(compareBy<PlaylistProfile> { it.order }.thenBy { it.name.lowercase() })
        .flatMap { profile ->
            loaded[profile.id].orEmpty().asSequence().map { channel -> profile.id to channel }
        }
        .mapIndexed { position, (sourceId, channel) ->
            channel.copy(
                position = position,
                sourceId = sourceId,
                sourcePosition = channel.sourcePosition.takeIf { channel.sourceId.isNotEmpty() } ?: channel.position
            )
        }
        .toList()
    return Playlist(channels)
}

private fun Playlist?.orEmpty(): List<PlaylistChannel> = this?.channels.orEmpty()

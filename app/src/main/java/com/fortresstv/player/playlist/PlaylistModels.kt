package com.fortresstv.player.playlist

import com.fortresstv.player.source.ContentSource
import java.security.MessageDigest

data class PlaylistChannel(
    val position: Int,
    val name: String,
    val streamUrl: String,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val logoUrl: String? = null,
    val group: String? = null,
    val sourceId: String = "",
    val sourcePosition: Int = position,
    val sourceItemId: String = "",
    val hasCatchup: Boolean = false,
    val catchupDays: Int = 0
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
    fun contributesMovies(): Boolean = this == AUTOMATIC || this == MOVIES || this == MOVIES_AND_SERIES || this == EVERYTHING
    fun contributesSeries(): Boolean = this == AUTOMATIC || this == SERIES || this == MOVIES_AND_SERIES || this == EVERYTHING
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
                sourcePosition = channel.sourcePosition.takeIf { channel.sourceId.isNotEmpty() } ?: channel.position,
                sourceItemId = channel.sourceItemId.ifBlank { channel.stableSourceItemId() }
            )
        }
        .toList()
    return Playlist(channels)
}

private fun Playlist?.orEmpty(): List<PlaylistChannel> = this?.channels.orEmpty()

internal fun PlaylistChannel.stableSourceItemId(): String {
    tvgId?.trim()?.takeIf { it.isNotEmpty() }?.let { return "tvg:$it" }
    val identity = listOf(tvgName.orEmpty(), name, streamUrl).joinToString("\u0000")
    val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
    return "m3u:" + digest.joinToString("") { "%02x".format(it) }
}

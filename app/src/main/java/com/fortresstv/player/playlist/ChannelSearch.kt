package com.fortresstv.player.playlist

fun List<PlaylistChannel>.searchChannels(query: String): List<PlaylistChannel> {
    val term = query.trim()
    if (term.isEmpty()) return this
    return filter { channel ->
        channel.name.contains(term, ignoreCase = true) ||
            channel.tvgName?.contains(term, ignoreCase = true) == true ||
            channel.tvgId?.contains(term, ignoreCase = true) == true
    }
}

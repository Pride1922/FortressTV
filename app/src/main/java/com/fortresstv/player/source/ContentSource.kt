package com.fortresstv.player.source

sealed interface ContentSource {
    val value: String
    val displayName: String

    data class LocalFile(
        override val value: String,
        override val displayName: String
    ) : ContentSource

    data class RemoteUrl(
        override val value: String,
        override val displayName: String
    ) : ContentSource

    data class Xtream(
        override val value: String,
        override val displayName: String,
        val username: String,
        val password: String
    ) : ContentSource
}

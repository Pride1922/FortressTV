package com.fortresstv.player.subtitles

data class OpenSubtitlesItem(
    val id: String,
    val fileId: Int,
    val fileName: String,
    val language: String,
    val release: String,
    val downloads: Int,
    val rating: Double,
    val hearingImpaired: Boolean
)

data class OpenSubtitlesSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val username: String = "",
    val password: String = "",
    val preferPtPt: Boolean = true,
    val autoSearch: Boolean = true
)

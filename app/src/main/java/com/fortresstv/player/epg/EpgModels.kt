package com.fortresstv.player.epg

data class EpgChannel(
    val id: String,
    val displayName: String?,
    val iconUrl: String?
)

data class EpgProgramme(
    val channelId: String,
    val start: String,
    val stop: String?,
    val title: String,
    val description: String?,
    val category: String?
)

data class EpgGuide(
    val channels: Map<String, EpgChannel>,
    val programmes: Map<String, List<EpgProgramme>>
)

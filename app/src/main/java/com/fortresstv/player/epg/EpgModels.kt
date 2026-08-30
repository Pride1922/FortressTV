package com.fortresstv.player.epg

data class EpgChannel(
    val id: String,
    val displayName: String?,
    val iconUrl: String?
)

data class EpgProgramme(
    val channelId: String,
    val title: String,
    val description: String?,
    val category: String?,
    val startEpochMilli: Long,
    val stopEpochMilli: Long?
)

data class EpgGuide(
    val channels: Map<String, EpgChannel>,
    val programmes: Map<String, List<EpgProgramme>>
)

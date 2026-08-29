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
    val category: String?,
    val startEpochMilli: Long = XmlTvTime.parseToEpoch(start) ?: 0L,
    val stopEpochMilli: Long? = stop?.let { XmlTvTime.parseToEpoch(it) }
)

data class EpgGuide(
    val channels: Map<String, EpgChannel>,
    val programmes: Map<String, List<EpgProgramme>>
)

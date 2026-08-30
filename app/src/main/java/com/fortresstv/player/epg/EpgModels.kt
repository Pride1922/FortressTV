package com.fortresstv.player.epg

data class EpgChannel(
    val id: String,
    val displayName: String?,
    val iconUrl: String? = null,
    val displayNames: List<String> = if (displayName != null) listOf(displayName) else emptyList()
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
) {
    val normalizedChannelMap: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()
        for ((id, channel) in channels) {
            val normId = EpgMatcher.normalize(id)
            if (normId.isNotBlank() && !map.containsKey(normId)) {
                map[normId] = id
            }
            val idNoDomain = id.substringBeforeLast(".")
            val normIdNoDomain = EpgMatcher.normalize(idNoDomain)
            if (normIdNoDomain.isNotBlank() && !map.containsKey(normIdNoDomain)) {
                map[normIdNoDomain] = id
            }
            for (dn in channel.displayNames) {
                val normDn = EpgMatcher.normalize(dn)
                if (normDn.isNotBlank() && !map.containsKey(normDn)) {
                    map[normDn] = id
                }
            }
        }
        map
    }

    fun resolveChannelId(tvgId: String?, name: String?): String? {
        val nameDigits = EpgMatcher.extractDigits(name)

        // 1. Try matching by channel name first (most specific, avoids provider TVG-ID collisions across numbered channels like DAZN 1/2/4)
        if (!name.isNullOrBlank()) {
            val normName = EpgMatcher.normalize(name)
            val matchedByName = normalizedChannelMap[normName]
            if (matchedByName != null) return matchedByName
        }

        // 2. If tvgId is provided, check direct match or normalized match (ensuring digit consistency)
        if (!tvgId.isNullOrBlank()) {
            if (channels.containsKey(tvgId) || programmes.containsKey(tvgId)) {
                val tvgDigits = EpgMatcher.extractDigits(tvgId)
                if (nameDigits.isEmpty() || tvgDigits == nameDigits) {
                    return tvgId
                }
            }

            val normTvg = EpgMatcher.normalize(tvgId)
            val matchedByTvg = normalizedChannelMap[normTvg]
            if (matchedByTvg != null) {
                val matchedDigits = EpgMatcher.extractDigits(matchedByTvg)
                if (nameDigits.isEmpty() || matchedDigits == nameDigits) {
                    return matchedByTvg
                }
            }
        }

        // 3. Fallback: if tvgId exists directly in guide and no digit conflict
        if (!tvgId.isNullOrBlank() && (channels.containsKey(tvgId) || programmes.containsKey(tvgId))) {
            return tvgId
        }

        return null
    }

    fun findChannel(tvgId: String?, name: String?): EpgChannel? {
        val id = resolveChannelId(tvgId, name) ?: return null
        return channels[id]
    }

    fun findProgrammes(tvgId: String?, name: String?): List<EpgProgramme> {
        val id = resolveChannelId(tvgId, name) ?: return emptyList()
        return programmes[id].orEmpty()
    }
}

package com.fortresstv.player.epg

import java.util.concurrent.ConcurrentHashMap

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

data class EpgPartitionMetadata(
    val epochDay: Long,
    val filename: String,
    val programmeCount: Int,
    val compressedSize: Long = 0L,
    val checksum: String = ""
)

data class EpgManifest(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val generationId: String,
    val sourceFingerprint: String,
    val timezoneId: String,
    val loadedDays: Int,
    val todayEpochDay: Long,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val channels: Map<String, EpgChannel>,
    val partitions: Map<Long, EpgPartitionMetadata>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 5
    }
}

data class EpgDay(
    val epochDay: Long,
    val programmes: Map<String, List<EpgProgramme>>
)

data class EpgGuide(
    val channels: Map<String, EpgChannel>,
    val programmes: Map<String, List<EpgProgramme>> = emptyMap(),
    val manifest: EpgManifest? = null,
    val epochDay: Long? = null
) {
    private val resolutionCache = ConcurrentHashMap<String, String>()

    /**
     * Normalised channel names and ids to guide channel ids. Building it runs several regexes per
     * channel, so the result is shared by every guide built on the same channel map (each day of
     * one download), and [warmUp] lets loaders build it on a background thread.
     */
    val normalizedChannelMap: Map<String, String> by lazy { NormalizedChannelIndex.forChannels(channels) }

    /** Builds the channel lookup now, so the first channel row on the main thread doesn't pay for it. */
    fun warmUp(): EpgGuide = also { normalizedChannelMap }

    // A guide holds every programme of a day. Structural equality made Compose and StateFlow compare
    // two guides item by item (once per channel row keyed on the guide) whenever a new one arrived.
    // Guides are immutable snapshots, so a new instance always means new data: compare by identity.
    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)

    fun resolveChannelId(
        tvgId: String?,
        name: String?,
        manualXmltvId: String? = null,
        tvgName: String? = null
    ): String? {
        if (!manualXmltvId.isNullOrBlank()) {
            if (channels.containsKey(manualXmltvId) || programmes.containsKey(manualXmltvId)) {
                return manualXmltvId
            }
        }
        val cacheKey = "${tvgId.orEmpty()}\u0000${name.orEmpty()}\u0000${tvgName.orEmpty()}"
        val cached = resolutionCache[cacheKey]
        if (cached != null) return cached.takeUnless { it == NO_CHANNEL_MATCH }

        val resolved = resolveChannelIdUncached(tvgId, name, tvgName)
        resolutionCache.putIfAbsent(cacheKey, resolved ?: NO_CHANNEL_MATCH)
        return resolved
    }

    private fun resolveChannelIdUncached(tvgId: String?, name: String?, tvgName: String? = null): String? {
        val nameDigits = EpgMatcher.extractDigits(name)

        // 1. Try matching by channel name first (most specific, avoids provider TVG-ID collisions across numbered channels like DAZN 1/2/4)
        if (!name.isNullOrBlank()) {
            val normName = EpgMatcher.normalize(name)
            val matchedByName = normalizedChannelMap[normName]
            if (matchedByName != null) return matchedByName
        }

        // 2. Fallback to tvgName if provided and distinct from name (ensuring digit consistency)
        if (!tvgName.isNullOrBlank() && tvgName != name) {
            val normTvgName = EpgMatcher.normalize(tvgName)
            val matchedByTvgName = normalizedChannelMap[normTvgName]
            if (matchedByTvgName != null) {
                val matchedDigits = EpgMatcher.extractDigits(matchedByTvgName)
                if (nameDigits.isEmpty() || matchedDigits == nameDigits) {
                    return matchedByTvgName
                }
            }
        }

        // 3. If tvgId is provided, check direct match or normalized match (ensuring digit consistency)
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

        // 4. Fallback: if tvgId exists directly in guide and no digit conflict
        if (!tvgId.isNullOrBlank() && (channels.containsKey(tvgId) || programmes.containsKey(tvgId))) {
            return tvgId
        }

        // 5. Alias fallback for common Portuguese channel rebrands/short names
        if (!name.isNullOrBlank()) {
            val normName = EpgMatcher.normalize(name)
            for (alias in portugueseNormalizedAliases(normName)) {
                val matched = normalizedChannelMap[alias]
                if (matched != null && (nameDigits.isEmpty() || EpgMatcher.extractDigits(matched) == nameDigits)) {
                    return matched
                }
            }
        }

        return null
    }

    private fun portugueseNormalizedAliases(norm: String): List<String> {
        if (norm.isBlank()) return emptyList()
        val aliases = mutableListOf<String>()
        if (norm.startsWith("starchannel")) aliases += "fox"
        if (norm == "fox") aliases += "starchannel"
        if (norm.startsWith("star")) aliases += "fox" + norm.removePrefix("star")
        if (norm.startsWith("fox") && norm != "fox") aliases += "star" + norm.removePrefix("fox")
        if (norm.startsWith("dazn")) {
            val suffix = norm.removePrefix("dazn")
            aliases += "elevensports$suffix"
            aliases += "eleven$suffix"
        }
        if (norm.startsWith("elevensports")) aliases += "dazn" + norm.removePrefix("elevensports")
        if (norm.startsWith("eleven") && !norm.startsWith("elevensports")) aliases += "dazn" + norm.removePrefix("eleven")
        if (norm == "natgeo") aliases += "nationalgeographic"
        if (norm == "nationalgeographic") aliases += "natgeo"
        if (norm == "hollywood") aliases += "canalhollywood"
        if (norm == "canalhollywood") aliases += "hollywood"
        if (norm == "btv") aliases += "benficatv"
        if (norm == "benficatv") aliases += "btv"
        if (norm == "discovery") aliases += "discoverychannel"
        if (norm == "discoverychannel") aliases += "discovery"
        if (norm == "globo") {
            aliases += "tvglobo"
            aliases += "globoportugal"
        }
        if (norm == "tvglobo") aliases += "globo"
        if (norm.endsWith("portugal") && norm.length > "portugal".length + 2) {
            aliases += norm.removeSuffix("portugal")
        }
        return aliases
    }

    fun findChannel(tvgId: String?, name: String?, manualXmltvId: String? = null, tvgName: String? = null): EpgChannel? {
        val id = resolveChannelId(tvgId, name, manualXmltvId, tvgName) ?: return null
        return channels[id]
    }

    fun findProgrammes(tvgId: String?, name: String?, manualXmltvId: String? = null, tvgName: String? = null): List<EpgProgramme> {
        val id = resolveChannelId(tvgId, name, manualXmltvId, tvgName) ?: return emptyList()
        return programmes[id].orEmpty()
    }

    private companion object {
        const val NO_CHANNEL_MATCH = "\u0000"
    }
}

internal object NormalizedChannelIndex {
    // One guide's channels are active at a time; remember the last build by identity.
    @Volatile
    private var last: Pair<Map<String, EpgChannel>, Map<String, String>>? = null

    fun forChannels(channels: Map<String, EpgChannel>): Map<String, String> {
        last?.let { (cachedChannels, index) -> if (cachedChannels === channels) return index }
        return build(channels).also { last = channels to it }
    }

    private fun build(channels: Map<String, EpgChannel>): Map<String, String> {
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
        return map
    }
}

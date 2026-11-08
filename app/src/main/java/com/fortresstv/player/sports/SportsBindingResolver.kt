package com.fortresstv.player.sports

import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.player.ChannelFailoverResolver
import com.fortresstv.player.playlist.PlaylistChannel
import java.util.Locale

/**
 * Resolves an authorized [PlaylistChannel] from the user's active playlist for a [SportsFixture].
 *
 * Employs a 2-stage binding pipeline:
 * 1. Direct Broadcaster Match: Normalizes the fixture's broadcaster name (e.g. "Sport TV 1", "DAZN 1")
 *    and tests against the user's channels using IPTV naming normalization and known broadcaster aliases.
 * 2. EPG Schedule Fallback: If direct matching fails (e.g. customized or non-standard channel names),
 *    scans the user's sports channels in the [EpgGuide] within the kickoff window for matching team keywords.
 * 3. Returns null if no verified channel is available, allowing honest UI rendering without false links.
 */
object SportsBindingResolver {

    private val BROADCASTER_ALIASES = listOf(
        setOf("sporttv1", "sportv1", "spt1", "sporttv1pt", "sptv1"),
        setOf("sporttv2", "sportv2", "spt2", "sporttv2pt", "sptv2"),
        setOf("sporttv3", "sportv3", "spt3", "sporttv3pt", "sptv3"),
        setOf("sporttv4", "sportv4", "spt4", "sporttv4pt", "sptv4"),
        setOf("sporttv5", "sportv5", "spt5", "sporttv5pt", "sptv5"),
        setOf("sporttv6", "sportv6", "spt6", "sporttv6pt", "sptv6"),
        setOf("sporttvplus", "sportvplus", "sptplus", "sporttvmais"),
        setOf("dazn1", "dazn1pt", "elevensports1", "eleven1", "eleven1pt"),
        setOf("dazn2", "dazn2pt", "elevensports2", "eleven2", "eleven2pt"),
        setOf("dazn3", "dazn3pt", "elevensports3", "eleven3", "eleven3pt"),
        setOf("dazn4", "dazn4pt", "elevensports4", "eleven4", "eleven4pt"),
        setOf("dazn5", "dazn5pt", "elevensports5", "eleven5", "eleven5pt"),
        setOf("dazn6", "dazn6pt", "elevensports6", "eleven6", "eleven6pt"),
        setOf("eurosport1", "eurosport1pt", "eurosport1hd"),
        setOf("eurosport2", "eurosport2pt", "eurosport2hd"),
        setOf("benficatv", "btv", "benficatv1"),
        setOf("sportingtv", "stv"),
        setOf("portocanal"),
        setOf("rtp1", "rtp1pt", "rtp1hd"),
        setOf("rtp2", "rtp2pt", "rtp2hd"),
        setOf("sic", "sicpt", "sichd"),
        setOf("tvi", "tvipt", "tvihd"),
        setOf("canal11", "canal11pt", "c11"),
        setOf("vmais", "vmaistvi", "vplus", "vplustvi", "vtvi", "vpluspt")
    )

    private val TEAM_PREFIXES = Regex("(?i)\\b(?:sl|fc|sc|cf|cp|cd|ud|gd|ac|afc|sad|clube|futebol|de|e|the)\\b")

    fun resolve(
        fixture: SportsFixture,
        channels: List<PlaylistChannel>,
        epgGuide: EpgGuide? = null
    ): PlaylistChannel? {
        if (channels.isEmpty()) return null

        // 1. EPG Schedule Match on Portuguese channels first
        if (epgGuide != null) {
            val epgPt = matchViaEpg(fixture, channels, epgGuide, portugueseOnly = true)
            if (epgPt != null) return epgPt
        }

        // 2. Direct Broadcaster Match (prefers Portuguese)
        val direct = matchBroadcasterDirectly(fixture.broadcasterOfficial, channels)
        if (direct != null && isPortugueseChannel(direct)) return direct

        // 3. Fallback to foreign EPG match
        if (epgGuide != null) {
            val foreignEpg = matchViaEpg(fixture, channels, epgGuide, portugueseOnly = false)
            if (foreignEpg != null) return foreignEpg
        }

        return direct
    }

    /**
     * Resolves all fixtures at once, distributing simultaneous matches across broadcaster channels
     * (e.g. DAZN 1, DAZN 2, DAZN 3, DAZN 4 or Sport TV 1, Sport TV 2, Sport TV 3) so that
     * simultaneous games in the same competition are never bound to the exact same channel.
     * Strictly prioritizes Portuguese feeds (PT:) over foreign feeds (BR:, ES:, etc.).
     */
    fun resolveAll(
        fixtures: List<SportsFixture>,
        channels: List<PlaylistChannel>,
        epgGuide: EpgGuide? = null
    ): List<Pair<SportsFixture, PlaylistChannel>> {
        if (channels.isEmpty() || fixtures.isEmpty()) return emptyList()

        val results = mutableListOf<Pair<SportsFixture, PlaylistChannel>>()
        // Track channels used per kickoff slot and broadcaster family: "kickoffEpoch_family" -> setOf(channelNorm)
        val usedChannelsAtSlot = mutableMapOf<String, MutableSet<String>>()

        for (fixture in fixtures) {
            val family = extractBroadcasterFamily(fixture.broadcasterOfficial)
            val slotKey = "${fixture.kickoffUtc.epochSecond}_$family"
            val used = usedChannelsAtSlot.getOrPut(slotKey) { mutableSetOf() }

            // 1. EPG schedule match on Portuguese channel
            var matched: PlaylistChannel? = if (epgGuide != null) {
                matchViaEpg(fixture, channels, epgGuide, alreadyUsed = used, portugueseOnly = true)
            } else null

            // 2. Broadcaster family distribution fallback on Portuguese channels
            if (matched == null) {
                matched = findAvailableChannelInFamily(fixture.broadcasterOfficial, channels, used)
            }

            // 3. Fallback to foreign EPG match if no Portuguese channel exists
            if (matched == null && epgGuide != null) {
                matched = matchViaEpg(fixture, channels, epgGuide, alreadyUsed = used, portugueseOnly = false)
            }

            if (matched != null) {
                val norm = ChannelFailoverResolver.normalizeChannelName(matched.name)
                used.add(norm)
                results.add(fixture to matched)
            }
        }
        return results
    }

    private fun extractBroadcasterFamily(broadcaster: String): String {
        val lower = broadcaster.lowercase(Locale.ROOT)
        return when {
            lower.contains("dazn") || lower.contains("eleven") -> "dazn"
            lower.contains("sport") || lower.contains("spt") -> "sporttv"
            lower.contains("btv") || lower.contains("benfica") -> "btv"
            lower.contains("eurosport") -> "eurosport"
            lower.contains("v+") || lower.contains("vmais") || lower.contains("vplus") -> "vmais"
            else -> lower.filter { it.isLetter() }
        }
    }

    private fun findAvailableChannelInFamily(
        preferredBroadcaster: String,
        channels: List<PlaylistChannel>,
        alreadyUsed: Set<String>
    ): PlaylistChannel? {
        val family = extractBroadcasterFamily(preferredBroadcaster)

        val candidateNames = when (family) {
            "dazn" -> listOf("DAZN 1", "DAZN 2", "DAZN 3", "DAZN 4", "DAZN 5", "DAZN 6")
            "sporttv" -> listOf("Sport TV 1", "Sport TV 2", "Sport TV 3", "Sport TV 4", "Sport TV 5", "Sport TV 6")
            else -> listOf(preferredBroadcaster)
        }

        val orderedCandidates = listOf(preferredBroadcaster) + candidateNames.filterNot { it.equals(preferredBroadcaster, true) }

        for (cand in orderedCandidates) {
            val matched = matchBroadcasterDirectly(cand, channels)
            if (matched != null) {
                val norm = ChannelFailoverResolver.normalizeChannelName(matched.name)
                if (!alreadyUsed.contains(norm)) {
                    return matched
                }
            }
        }

        return null
    }

    fun matchBroadcasterDirectly(
        broadcasterOfficial: String,
        channels: List<PlaylistChannel>
    ): PlaylistChannel? {
        if (broadcasterOfficial.isBlank()) return null
        val targetNorm = ChannelFailoverResolver.normalizeChannelName(broadcasterOfficial)
        if (targetNorm.isBlank()) return null

        val aliasSet = BROADCASTER_ALIASES.firstOrNull { it.contains(targetNorm) } ?: setOf(targetNorm)

        val candidates = channels.filter { channel ->
            val channelNorm = ChannelFailoverResolver.normalizeChannelName(channel.name)
            aliasSet.contains(channelNorm)
        }
        if (candidates.isEmpty()) return null

        return candidates.firstOrNull { isPortugueseChannel(it) } ?: candidates.first()
    }

    fun isPortugueseChannel(channel: PlaylistChannel): Boolean {
        val n = channel.name.lowercase(Locale.ROOT).trim()
        val g = channel.group?.lowercase(Locale.ROOT).orEmpty().trim()
        if (n.startsWith("br:") || n.startsWith("br |") || n.startsWith("br -") ||
            n.startsWith("es:") || n.startsWith("fr:") || n.startsWith("it:") ||
            n.startsWith("de:") || n.startsWith("uk:") || n.startsWith("us:") ||
            g.startsWith("brasil") || g.startsWith("brazil")) {
            return false
        }
        return n.startsWith("pt:") || n.startsWith("pt |") || n.startsWith("pt-") ||
            n.contains("portugal") || g.contains("portugal") || g.startsWith("pt") ||
            n.contains(" (pt)") || n.endsWith(" pt")
    }

    private val COMMENTARY_CHANNELS_REGEX = Regex("(?i)(?:\\bcmtv\\b|\\bcnn\\b|not[ií]cias|informa[cç][aã]o|\\ba bola\\b|\\babolatv\\b|\\bnews\\b)")
    private val NON_LIVE_KEYWORDS = listOf("antevisão", "antevisao", "pós-jogo", "pos-jogo", "rescaldo", "comentários", "comentarios", "debate", "análise", "analise", "especial informação", "magazine", "diário", "diario")

    internal fun getChannelPriority(
        channel: PlaylistChannel,
        broadcasterOfficial: String
    ): Int {
        val n = channel.name.lowercase(Locale.ROOT)
        if (COMMENTARY_CHANNELS_REGEX.containsMatchIn(n)) {
            return -100 // Exclude non-live commentary/news channels
        }

        val norm = ChannelFailoverResolver.normalizeChannelName(channel.name)
        val targetNorm = ChannelFailoverResolver.normalizeChannelName(broadcasterOfficial)

        // Exact match with official broadcaster (e.g. "PT: SPORT TV 1" when official is "Sport TV 1")
        val aliasSet = BROADCASTER_ALIASES.firstOrNull { it.contains(targetNorm) } ?: (if (targetNorm.isNotBlank()) setOf(targetNorm) else emptySet())
        if (aliasSet.isNotEmpty() && aliasSet.contains(norm)) {
            return 100
        }

        // Same broadcaster family (e.g. other Sport TV channels when official is Sport TV 1)
        val family = extractBroadcasterFamily(broadcasterOfficial)
        val channelFamily = extractBroadcasterFamily(channel.name)
        if (family.isNotBlank() && channelFamily == family) {
            return 80
        }

        // Dedicated sports channel network
        if (channelFamily == "sporttv" || channelFamily == "dazn" || channelFamily == "btv" ||
            channelFamily == "eurosport" || norm.contains("canal11")) {
            return 50
        }

        // General TV channels (RTP 1, RTP 2, SIC, TVI)
        return 10
    }

    fun matchViaEpg(
        fixture: SportsFixture,
        channels: List<PlaylistChannel>,
        epgGuide: EpgGuide,
        alreadyUsed: Set<String> = emptySet(),
        portugueseOnly: Boolean = false
    ): PlaylistChannel? {
        val homeKeywords = extractTeamKeywords(fixture.homeTeam)
        val awayKeywords = extractTeamKeywords(fixture.awayTeam)
        if (homeKeywords.isEmpty() || awayKeywords.isEmpty()) return null

        val kickoffEpochMs = fixture.kickoffUtc.toEpochMilli()
        val windowStartMs = kickoffEpochMs - 25 * 60 * 1000L // 25m before
        val windowEndMs = kickoffEpochMs + 135 * 60 * 1000L   // 2h15m after kickoff

        // Prioritize sports channels for performance
        val sportsChannels = channels.filter { ch ->
            val g = ch.group?.lowercase(Locale.ROOT).orEmpty()
            val n = ch.name.lowercase(Locale.ROOT)
            g.contains("sport") || g.contains("desporto") || g.contains("futebol") ||
                n.contains("sport") || n.contains("dazn") || n.contains("eleven") ||
                n.contains("rtp") || n.contains("sic") || n.contains("tvi") || n.contains("canal 11")
        }.ifEmpty { channels }

        val nonCommentary = sportsChannels.filter { getChannelPriority(it, fixture.broadcasterOfficial) >= 0 }
        val candidatePool = if (nonCommentary.isNotEmpty()) nonCommentary else sportsChannels

        val candidates = candidatePool.sortedWith(
            compareByDescending<PlaylistChannel> { if (portugueseOnly) (if (isPortugueseChannel(it)) 1 else 0) else 0 }
                .thenByDescending { isPortugueseChannel(it) }
                .thenByDescending { getChannelPriority(it, fixture.broadcasterOfficial) }
        ).let { list ->
            if (portugueseOnly) list.filter { isPortugueseChannel(it) } else list
        }

        for (channel in candidates) {
            val norm = ChannelFailoverResolver.normalizeChannelName(channel.name)
            if (alreadyUsed.contains(norm)) continue

            val isDedicatedSports = getChannelPriority(channel, fixture.broadcasterOfficial) >= 50

            val programmes = epgGuide.findProgrammes(channel.tvgId, channel.name)
            for (prog in programmes) {
                val stopMs = prog.stopEpochMilli ?: (prog.startEpochMilli + 90 * 60 * 1000L)
                if (stopMs < windowStartMs || prog.startEpochMilli > windowEndMs) continue

                val titleLower = prog.title.lowercase(Locale.ROOT)
                val descLower = prog.description?.lowercase(Locale.ROOT).orEmpty()
                val fullText = "$titleLower $descLower"

                if (!isDedicatedSports) {
                    val isCommentary = NON_LIVE_KEYWORDS.any { titleLower.contains(it) || descLower.contains(it) }
                    if (isCommentary && !titleLower.contains("direto")) {
                        continue
                    }
                }

                val matchesHome = homeKeywords.any { fullText.contains(it) }
                val matchesAway = awayKeywords.any { fullText.contains(it) }

                if (matchesHome && matchesAway) {
                    return channel
                }
            }
        }
        return null
    }

    fun extractTeamKeywords(teamName: String): List<String> {
        val cleaned = teamName.replace(TEAM_PREFIXES, " ").trim()
        val tokens = cleaned.split(Regex("[^a-zA-Z0-9]+"))
            .map { it.lowercase(Locale.ROOT).trim() }
            .filter { it.length >= 3 }

        return tokens.ifEmpty {
            listOf(teamName.lowercase(Locale.ROOT).trim())
        }
    }
}

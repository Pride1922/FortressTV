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
        setOf("dazn1", "dazn1pt", "elevensports1", "eleven1", "eleven1pt"),
        setOf("dazn2", "dazn2pt", "elevensports2", "eleven2", "eleven2pt"),
        setOf("dazn3", "dazn3pt", "elevensports3", "eleven3", "eleven3pt"),
        setOf("dazn4", "dazn4pt", "elevensports4", "eleven4", "eleven4pt"),
        setOf("dazn5", "dazn5pt", "elevensports5", "eleven5", "eleven5pt"),
        setOf("dazn6", "dazn6pt", "elevensports6", "eleven6", "eleven6pt"),
        setOf("rtp1", "rtp1pt", "rtp1hd"),
        setOf("rtp2", "rtp2pt", "rtp2hd"),
        setOf("sic", "sicpt", "sichd"),
        setOf("tvi", "tvipt", "tvihd"),
        setOf("canal11", "canal11pt", "c11")
    )

    private val TEAM_PREFIXES = Regex("(?i)\\b(?:sl|fc|sc|cf|cp|cd|ud|gd|ac|afc|sad|clube|futebol|de|e|the)\\b")

    fun resolve(
        fixture: SportsFixture,
        channels: List<PlaylistChannel>,
        epgGuide: EpgGuide? = null
    ): PlaylistChannel? {
        if (channels.isEmpty()) return null

        // 1. Direct Broadcaster Match
        val directMatch = matchBroadcasterDirectly(fixture.broadcasterOfficial, channels)
        if (directMatch != null) return directMatch

        // 2. EPG Schedule Fallback
        if (epgGuide != null) {
            val epgMatch = matchViaEpg(fixture, channels, epgGuide)
            if (epgMatch != null) return epgMatch
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

        return channels.firstOrNull { channel ->
            val channelNorm = ChannelFailoverResolver.normalizeChannelName(channel.name)
            aliasSet.contains(channelNorm)
        }
    }

    fun matchViaEpg(
        fixture: SportsFixture,
        channels: List<PlaylistChannel>,
        epgGuide: EpgGuide
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

        for (channel in sportsChannels) {
            val programmes = epgGuide.findProgrammes(channel.tvgId, channel.name)
            for (prog in programmes) {
                val stopMs = prog.stopEpochMilli ?: (prog.startEpochMilli + 90 * 60 * 1000L)
                if (stopMs < windowStartMs || prog.startEpochMilli > windowEndMs) continue

                val titleLower = prog.title.lowercase(Locale.ROOT)
                val descLower = prog.description?.lowercase(Locale.ROOT).orEmpty()
                val fullText = "$titleLower $descLower"

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

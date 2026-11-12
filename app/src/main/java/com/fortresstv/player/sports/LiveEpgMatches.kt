package com.fortresstv.player.sports

import com.fortresstv.player.epg.EpgChannelMapping
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.epg.EpgMatcher
import com.fortresstv.player.playlist.PlaylistChannel
import java.time.Instant
import java.util.Locale

private val replayMarker = Regex("""\b(?:replay|repeat|repetição|resumo|highlights|antevisão|preview|analysis|análise|compacto|pré-jogo|pós-jogo)\b""", RegexOption.IGNORE_CASE)
private val regionPrefix = Regex("""^\s*(?:\[([a-z]{2,4})]|\(([a-z]{2,4})\)|([a-z]{2,4})\s*[:|–-])""", RegexOption.IGNORE_CASE)
private val foreignGroup = Regex("""^\s*(?:brasil|brazil|exyu)\b""", RegexOption.IGNORE_CASE)
private val portugueseCountryNames by lazy {
    val portuguese = Locale.forLanguageTag("pt-PT")
    Locale.getISOCountries().associate { code ->
        val country = Locale.Builder().setRegion(code).build()
        EpgMatcher.normalize(country.getDisplayCountry(Locale.ENGLISH)) to
            EpgMatcher.normalize(country.getDisplayCountry(portuguese))
    } + mapOf(
        "turkey" to "turquia",
        "turkiye" to "turquia",
        "czechrepublic" to "chequia",
        "england" to "inglaterra",
        "scotland" to "escocia",
        "wales" to "paisdegales",
        "northernireland" to "irlandadonorte"
    )
}

private fun teamNames(name: String): Set<String> {
    val normalized = EpgMatcher.normalize(name)
    return buildSet {
        add(normalized)
        portugueseCountryNames[normalized]?.let { add(it) }
        SportsBindingResolver.extractTeamKeywords(name).mapTo(this, EpgMatcher::normalize)
    }.filterTo(mutableSetOf(), String::isNotEmpty)
}

private fun isForeignFeed(label: String): Boolean {
    val region = regionPrefix.find(label)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }
    return region != null && !region.equals("PT", ignoreCase = true) || foreignGroup.containsMatchIn(label)
}

internal fun liveEpgMatches(
    fixtures: List<SportsFixture>,
    guide: EpgGuide,
    channels: List<PlaylistChannel>,
    mappings: Map<String, EpgChannelMapping>,
    now: Instant,
    correctionMinutes: Int = 0
): List<Pair<SportsFixture, PlaylistChannel>> {
    val liveFixtures = fixtures.filter(SportsFixture::isLive)
    if (liveFixtures.isEmpty()) return emptyList()
    val teamNamesByFixture = liveFixtures.associateWith { teamNames(it.homeTeam) to teamNames(it.awayTeam) }
    val nowMillis = now.toEpochMilli()
    val correctionMillis = correctionMinutes * 60_000L
    val guideIdsByName = buildMap {
        for ((id, channel) in guide.channels) {
            if (isForeignFeed(id) || channel.displayNames.any(::isForeignFeed)) continue
            for (name in channel.displayNames) {
                val normalized = EpgMatcher.normalize(name)
                if (normalized.isNotEmpty()) putIfAbsent(normalized, id)
            }
        }
    }
    val matches = channels.asSequence().filterNot { isForeignFeed(it.name) || isForeignFeed(it.group.orEmpty()) }.flatMap { channel ->
        val manualId = mappings["${channel.sourceId}:${channel.sourceItemId}"]?.xmltvChannelId
        val guideId = manualId?.takeIf { it in guide.channels }
            ?: guideIdsByName[EpgMatcher.normalize(channel.name)]
            ?: channel.tvgId?.takeIf { id ->
                id in guide.channels && EpgMatcher.extractDigits(channel.name) == EpgMatcher.extractDigits(id)
            }
        val guideChannel = guide.channels[guideId]
        if (guideChannel == null || isForeignFeed(guideChannel.id) || guideChannel.displayNames.any(::isForeignFeed)) {
            return@flatMap emptySequence()
        }
        guide.programmes[guideChannel.id].orEmpty().asSequence().flatMap programmeMatches@{ programme ->
            val start = programme.startEpochMilli + correctionMillis
            val end = (programme.stopEpochMilli ?: (programme.startEpochMilli + 7_200_000L)) + correctionMillis
            if (nowMillis !in start until end || replayMarker.containsMatchIn(programme.title)) {
                return@programmeMatches emptySequence()
            }
            // Keep a short team name before a dash from being treated as a channel prefix.
            val title = EpgMatcher.normalize("match ${programme.title}").removePrefix("match")
            liveFixtures.asSequence().filter { fixture ->
                val kickoff = fixture.kickoffUtc.toEpochMilli()
                val (home, away) = teamNamesByFixture.getValue(fixture)
                kickoff in (start - 25 * 60_000L) until end &&
                    home.any { title.contains(it) } && away.any { title.contains(it) }
            }.map { it to channel }
        }
    }.toList()
    return matches.distinctBy { it.first.id }.sortedBy { it.first.kickoffUtc }
}

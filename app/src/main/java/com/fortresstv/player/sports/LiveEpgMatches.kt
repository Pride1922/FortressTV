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
private val portugalFeed = Regex("""(?:\.pt$|\bportugal\b|^\s*(?:\[pt]|\(pt\)|pt\s*[:|–-]))""", RegexOption.IGNORE_CASE)
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
        "netherlands" to "paisesbaixos",
        "northmacedonia" to "macedoniadonorte",
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

private fun isForeignGuide(id: String, names: List<String>): Boolean =
    isForeignFeed(id) || !portugalFeed.containsMatchIn(id) && names.any(::isForeignFeed)

internal fun liveEpgMatches(
    fixtures: List<SportsFixture>,
    guide: EpgGuide,
    channels: List<PlaylistChannel>,
    mappings: Map<String, EpgChannelMapping>,
    now: Instant,
    correctionMinutes: Int = 0,
    upcomingUntil: Instant = now
): List<Pair<SportsFixture, PlaylistChannel>> {
    val includeUpcoming = upcomingUntil > now
    val candidates = fixtures.filter {
        if (includeUpcoming) it.sport == SportsKind.FOOTBALL && it.status == FixtureStatus.SCHEDULED
        else it.isLive()
    }
    if (candidates.isEmpty()) return emptyList()
    val teamNamesByFixture = candidates.associateWith { teamNames(it.homeTeam) to teamNames(it.awayTeam) }
    val nowMillis = now.toEpochMilli()
    val untilMillis = upcomingUntil.toEpochMilli()
    val correctionMillis = correctionMinutes * 60_000L
    val guideIdsByName = buildMap {
        for ((id, channel) in guide.channels) {
            if (isForeignGuide(id, channel.displayNames)) continue
            for (name in channel.displayNames) {
                val normalized = EpgMatcher.normalize(name)
                if (normalized.isNotEmpty()) {
                    val previous = get(normalized)
                    if (previous == null || includeUpcoming && portugalFeed.containsMatchIn(id) &&
                        !portugalFeed.containsMatchIn(previous)) put(normalized, id)
                }
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
            ?: guide.resolveChannelId(channel.tvgId, channel.name)
        val guideChannel = guide.channels[guideId]
        if (guideChannel == null || isForeignGuide(guideChannel.id, guideChannel.displayNames)) {
            return@flatMap emptySequence()
        }
        if (includeUpcoming && listOf(channel.name, channel.group.orEmpty(), guideChannel.id)
                .none(portugalFeed::containsMatchIn) && guideChannel.displayNames.none(portugalFeed::containsMatchIn)) {
            return@flatMap emptySequence()
        }
        guide.programmes[guideChannel.id].orEmpty().asSequence().flatMap programmeMatches@{ programme ->
            val start = programme.startEpochMilli + correctionMillis
            val end = (programme.stopEpochMilli ?: (programme.startEpochMilli + 7_200_000L)) + correctionMillis
            val airing = nowMillis in start until end
            val upcoming = start in nowMillis until untilMillis
            if (!(if (includeUpcoming) upcoming else airing) || replayMarker.containsMatchIn(programme.title)) {
                return@programmeMatches emptySequence()
            }
            // Keep a short team name before a dash from being treated as a channel prefix.
            val title = EpgMatcher.normalize("match ${programme.title}").removePrefix("match")
            val matched = candidates.firstOrNull { fixture ->
                val kickoff = fixture.kickoffUtc.toEpochMilli()
                val (home, away) = teamNamesByFixture.getValue(fixture)
                kickoff in (start - 25 * 60_000L) until end &&
                    (!includeUpcoming || kickoff in nowMillis until untilMillis) &&
                    home.any { title.contains(it) } && away.any { title.contains(it) }
            }
            if (matched == null) emptySequence() else sequenceOf(matched to channel)
        }
    }.toList()
    return matches.distinctBy { it.first.id }.sortedBy { it.first.kickoffUtc }
}

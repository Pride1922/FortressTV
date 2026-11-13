package com.fortresstv.player.sports

import com.fortresstv.player.epg.EpgChannel
import com.fortresstv.player.epg.EpgChannelMapping
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.epg.EpgMatcher
import com.fortresstv.player.playlist.PlaylistChannel
import java.time.Instant
import java.util.Locale

internal val replayMarker = Regex("""\b(?:replay|repeat|repetição|resumo|highlights|antevisão|preview|analysis|análise|compacto|pré-jogo|pós-jogo)\b""", RegexOption.IGNORE_CASE)
private val regionPrefix = Regex("""^\s*(?:\[([a-z]{2,4})]|\(([a-z]{2,4})\)|([a-z]{2,4})\s*[:|–-])""", RegexOption.IGNORE_CASE)
private val foreignGroup = Regex("""^\s*(?:brasil|brazil|exyu)\b""", RegexOption.IGNORE_CASE)
internal val portugalFeed = Regex("""(?:\.pt$|\bportugal\b|^\s*(?:\[pt]|\(pt\)|pt\s*[:|–-]))""", RegexOption.IGNORE_CASE)
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

internal fun teamNames(name: String): Set<String> {
    val normalized = EpgMatcher.normalize(name)
    return buildSet {
        add(normalized)
        portugueseCountryNames[normalized]?.let { add(it) }
        SportsBindingResolver.extractTeamKeywords(name).mapTo(this, EpgMatcher::normalize)
    }.filterTo(mutableSetOf(), String::isNotEmpty)
}

internal fun isForeignFeed(label: String): Boolean {
    val region = regionPrefix.find(label)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }
    return region != null && !region.equals("PT", ignoreCase = true) || foreignGroup.containsMatchIn(label)
}

internal fun isForeignGuide(id: String, names: List<String>): Boolean =
    isForeignFeed(id) || !portugalFeed.containsMatchIn(id) && names.any(::isForeignFeed)

/** A fixture paired with the playlist channel showing it. [live] comes from the guide, not the API status. */
data class SportsEpgMatch(
    val fixture: SportsFixture,
    val channel: PlaylistChannel,
    val live: Boolean,
    /** Other playlist channels carrying the same event (e.g. the free-to-air simulcast). */
    val alternateChannels: List<PlaylistChannel> = emptyList(),
    /** Sport named by the guide ("Futsal", "Andebol Fem."); set only when the event has no ESPN fixture. */
    val sportLabel: String? = null,
    /** Corrected broadcast windows of the guide programmes behind a guide event. */
    val guideAirings: List<GuideAiringWindow> = emptyList()
)

data class GuideAiringWindow(val channel: PlaylistChannel, val start: Long, val end: Long)

/** Guide day partitions to search: the day before `now` (matches that started before midnight) up to [until]. */
internal fun epgDaysForMatches(
    now: Instant,
    until: Instant,
    correctionMinutes: Int,
    zone: java.time.ZoneId
): LongRange {
    val correction = correctionMinutes * 60L
    val firstDay = now.minusSeconds(correction).atZone(zone).toLocalDate().toEpochDay() - 1
    val lastDay = until.minusSeconds(correction).minusNanos(1).atZone(zone).toLocalDate().toEpochDay()
    return firstDay..maxOf(firstDay, lastDay)
}

/** Airing matches first, then by kickoff; one entry per fixture, preferring the airing one. */
private fun List<SportsEpgMatch>.rankedMatches(): List<SportsEpgMatch> =
    sortedWith(compareByDescending<SportsEpgMatch> { it.live }.thenBy { it.fixture.kickoffUtc })
        .distinctBy { it.fixture.id }

/**
 * Playlist channels paired with their guide channel, after the Portugal-feed rules: foreign feeds are
 * dropped, and when upcoming programmes are wanted only channels tied to a Portuguese feed are kept.
 */
internal fun resolveGuideChannels(
    guide: EpgGuide,
    channels: List<PlaylistChannel>,
    mappings: Map<String, EpgChannelMapping>,
    includeUpcoming: Boolean
): Sequence<Pair<PlaylistChannel, EpgChannel>> {
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
    return channels.asSequence().filterNot { isForeignFeed(it.name) || isForeignFeed(it.group.orEmpty()) }.mapNotNull { channel ->
        val manualId = mappings["${channel.sourceId}:${channel.sourceItemId}"]?.xmltvChannelId
        val guideId = manualId?.takeIf { it in guide.channels }
            ?: guideIdsByName[EpgMatcher.normalize(channel.name)]
            ?: channel.tvgId?.takeIf { id ->
                id in guide.channels && EpgMatcher.extractDigits(channel.name) == EpgMatcher.extractDigits(id)
            }
            ?: guide.resolveChannelId(channel.tvgId, channel.name)
        val guideChannel = guide.channels[guideId]
        if (guideChannel == null || isForeignGuide(guideChannel.id, guideChannel.displayNames)) {
            return@mapNotNull null
        }
        if (includeUpcoming && listOf(channel.name, channel.group.orEmpty(), guideChannel.id)
                .none(portugalFeed::containsMatchIn) && guideChannel.displayNames.none(portugalFeed::containsMatchIn)) {
            return@mapNotNull null
        }
        channel to guideChannel
    }
}

/** Matches across several day partitions of the same guide. */
internal fun liveEpgMatches(
    fixtures: List<SportsFixture>,
    guides: List<EpgGuide>,
    channels: List<PlaylistChannel>,
    mappings: Map<String, EpgChannelMapping>,
    now: Instant,
    correctionMinutes: Int = 0,
    upcomingUntil: Instant = now
): List<SportsEpgMatch> = guides.flatMap {
    liveEpgMatches(fixtures, it, channels, mappings, now, correctionMinutes, upcomingUntil)
}.rankedMatches()

/**
 * Fixtures whose matched guide programme is on air at [now], or starts before [upcomingUntil].
 * The API status only excludes finished/postponed fixtures: it can lag, so "live" is the guide's call.
 */
internal fun liveEpgMatches(
    fixtures: List<SportsFixture>,
    guide: EpgGuide,
    channels: List<PlaylistChannel>,
    mappings: Map<String, EpgChannelMapping>,
    now: Instant,
    correctionMinutes: Int = 0,
    upcomingUntil: Instant = now
): List<SportsEpgMatch> {
    val includeUpcoming = upcomingUntil > now
    val candidates = fixtures.filter {
        it.status == FixtureStatus.SCHEDULED || it.status == FixtureStatus.LIVE
    }
    if (candidates.isEmpty()) return emptyList()
    val teamNamesByFixture = candidates.associateWith { teamNames(it.homeTeam) to teamNames(it.awayTeam) }
    val nowMillis = now.toEpochMilli()
    val untilMillis = upcomingUntil.toEpochMilli()
    val correctionMillis = correctionMinutes * 60_000L
    val matches = resolveGuideChannels(guide, channels, mappings, includeUpcoming).flatMap { (channel, guideChannel) ->
        guide.programmes[guideChannel.id].orEmpty().asSequence().flatMap programmeMatches@{ programme ->
            val start = programme.startEpochMilli + correctionMillis
            val end = (programme.stopEpochMilli ?: (programme.startEpochMilli + 7_200_000L)) + correctionMillis
            val airing = nowMillis in start until end
            val upcoming = start in nowMillis until untilMillis
            if (!(airing || upcoming) || replayMarker.containsMatchIn(programme.title)) {
                return@programmeMatches emptySequence()
            }
            // Keep a short team name before a dash from being treated as a channel prefix.
            val title = EpgMatcher.normalize("match ${programme.title}").removePrefix("match")
            val matched = candidates.firstOrNull { fixture ->
                val kickoff = fixture.kickoffUtc.toEpochMilli()
                val (home, away) = teamNamesByFixture.getValue(fixture)
                // The kickoff must fall inside this broadcast, so a later unlabelled replay never matches.
                kickoff in (start - 25 * 60_000L) until end &&
                    (airing || fixture.sport == SportsKind.FOOTBALL) &&
                    home.any { title.contains(it) } && away.any { title.contains(it) }
            }
            if (matched == null) emptySequence() else sequenceOf(SportsEpgMatch(matched, channel, airing))
        }
    }.toList()
    return matches.rankedMatches()
}

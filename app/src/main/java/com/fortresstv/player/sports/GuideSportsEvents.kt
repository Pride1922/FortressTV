package com.fortresstv.player.sports

import com.fortresstv.player.epg.EpgChannel
import com.fortresstv.player.epg.EpgChannelMapping
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.epg.EpgMatcher
import com.fortresstv.player.epg.EpgProgramme
import com.fortresstv.player.playlist.PlaylistChannel
import java.time.Instant
import kotlin.math.abs

// Guide-first source for the "Matches on TV" row: the guide's own sports programmes become events, and
// ESPN fixtures only enrich them (kickoff, crests, score) or vouch for an airing the guide left unlabelled.
//
// Title grammar (Portuguese XMLTV, <category> is almost never set):
//   [Sport[ Fem.]: ]Home x Away - Competition[ - Extra] [(Direto)]
//   [Sport: ]Event name[ N.º Dia] - Competition [(Direto)]

private const val LIVE_SUFFIX = "(Direto)"
private const val MIN_MATCH_AIRING_MILLIS = 30 * 60_000L
private const val SAME_EVENT_START_MILLIS = 15 * 60_000L
private const val KICKOFF_LEAD_MILLIS = 25 * 60_000L
private const val DEFAULT_AIRING_MILLIS = 7_200_000L
internal const val MATCHES_ON_TV_LIMIT = 20

/** Fixture ids with this prefix have no ESPN event behind them ([SportsMatchSummaryApi] skips them). */
internal const val GUIDE_FIXTURE_ID_PREFIX = "epg-"

private val sportsChannelIds: Set<String> = buildSet {
    for (n in 1..6) {
        add("sporttv$n.pt")
        add("elevensports$n.pt")
    }
    addAll(
        listOf(
            "sporttvplus.pt", "spt7hd", "eurosport1.pt", "eurosport2.pt", "benficatv.pt", "sportingtv.pt",
            "porto.pt", "canal11.pt", "abolatv.pt", "skysportmotogp.it", "nbatv"
        )
    )
}

/** Generalist channels that carry big matches: only a match title or a known `Sport:` prefix counts there. */
private val freeToAirChannelIds = setOf("rtp1.pt", "rtpafrica.pt", "sic.pt", "tvi.pt")

/** Post-match shows, highlights, placeholders and magazines that [replayMarker] does not already cover. */
private val nonEventMarker = Regex(
    """rescaldo|melhores momentos|sem transmiss|not[ií]cias|noite desportiva|a bola da noite|info sporting|""" +
        """benfica 24 horas|mais futebol|\bstories\b|rico momento|\bmagazine\b""",
    RegexOption.IGNORE_CASE
)
private val sportPrefix = Regex("""^([^:]{2,40}):\s+(.+)$""")
private val womenSuffix = Regex("""\s+fem\.?$""", RegexOption.IGNORE_CASE)
private val teamSeparator = Regex("""\s+x\s+""")
private val guideAgeGroup = Regex("""\bsub[-\s]?(\d{2})\b""", RegexOption.IGNORE_CASE)
private val fixtureAgeGroup = Regex("""\b(?:u|sub|under)[-\s]?(\d{2})\b""", RegexOption.IGNORE_CASE)
private val fixtureWomen = Regex("""women|femin|\bfem\b|\(w\)""", RegexOption.IGNORE_CASE)
private val fixtureTeamMarker = Regex("""\b(?:(?:u|sub|under)[-\s]?\d{2}|women)\b|\(w\)""", RegexOption.IGNORE_CASE)
private val americanFootball = Regex("""\bNFL\b""")
private val nationalChampionship = Regex("""\bCamp(?:eonato|\.)\s*Nac(?:ional\b|\.)""", RegexOption.IGNORE_CASE)
private val nationsLeague = Regex("""^Liga das Na[cç][oõ]es\b""", RegexOption.IGNORE_CASE)

/** Sport prefixes the guide uses, keyed by [EpgMatcher.normalize]. */
private val knownSports: Map<String, SportsKind> = mapOf(
    "futebol" to SportsKind.FOOTBALL,
    "futsal" to SportsKind.OTHER,
    "andebol" to SportsKind.OTHER,
    "hoquei" to SportsKind.OTHER,
    "hoqueiempatins" to SportsKind.OTHER,
    "voleibol" to SportsKind.OTHER,
    "basquetebol" to SportsKind.BASKETBALL,
    "rugby" to SportsKind.OTHER,
    "padel" to SportsKind.OTHER,
    "tenis" to SportsKind.TENNIS,
    "golfe" to SportsKind.OTHER,
    "judo" to SportsKind.OTHER,
    "esquialpino" to SportsKind.OTHER,
    "triatlo" to SportsKind.OTHER,
    "combate" to SportsKind.OTHER,
    "motogp" to SportsKind.MOTORSPORT,
    "ciclocrosse" to SportsKind.OTHER,
    "powerlifting" to SportsKind.OTHER
)

private val clubAffixes = setOf("sl", "fc", "sc", "cf", "cp", "cd", "ud", "gd", "ac", "afc", "sad")
private val teamAliases = mapOf(
    "atlmadrid" to "atleticomadrid",
    "atleticodemadrid" to "atleticomadrid",
    "repirlanda" to "irlanda",
    "republicadairlanda" to "irlanda"
)

/** [EpgMatcher.normalize], guarded so a short first word before a dash is not read as a channel prefix. */
private fun normalizeText(text: String): String = EpgMatcher.normalize("match $text").removePrefix("match")

/** "Sporting CP" and "Sporting", "FC Porto" and "Porto", "Atl. Madrid" and "Atlético Madrid" share a key. */
internal fun guideTeamKey(name: String): String {
    val words = name.trim().split(Regex("""\s+"""))
    val core = words.filterNot { EpgMatcher.normalize("match $it").removePrefix("match") in clubAffixes }
    val key = normalizeText((core.ifEmpty { words }).joinToString(" "))
    return teamAliases[key] ?: key
}

internal fun guideCompetitionName(raw: String): String {
    val name = nationalChampionship.replace(raw.trim(), "Campeonato Nacional")
    return if (nationsLeague.containsMatchIn(name)) "UEFA Nations League" else name
}

/** One sports programme of the guide, parsed once per loaded guide day. Times are uncorrected. */
internal data class GuideAiring(
    val channelId: String,
    val title: String,
    val startEpochMilli: Long,
    val stopEpochMilli: Long?,
    /** The title carries the exact `(Direto)` suffix. */
    val live: Boolean,
    /** The channel is a sports channel rather than a generalist one. */
    val sportsChannel: Boolean,
    val sportLabel: String?,
    val kind: SportsKind,
    /** The sport comes from a known `Sport:` prefix, so an ESPN fixture must be of that sport too. */
    val explicitSport: Boolean,
    /** Home team, or the event name when [away] is empty. */
    val home: String,
    val away: String,
    val competition: String,
    val women: Boolean,
    /** "21" for a `Sub-21` side: an under-21 match is never the senior teams' match. */
    val ageGroup: String?,
    /** Same event on another channel has the same key; the competition text is not part of it. */
    val key: String,
    /** Normalised text an ESPN fixture's team names are looked up in. */
    val matchText: String
) {
    val hasTeams: Boolean get() = away.isNotEmpty()
}

/** Parses one programme of [channel], or null when it is not a sports event (or not on a sports channel). */
internal fun parseGuideAiring(programme: EpgProgramme, channel: EpgChannel): GuideAiring? {
    val channelId = channel.id.lowercase()
    val sportsChannel = channelId in sportsChannelIds
    if (!sportsChannel && channelId !in freeToAirChannelIds) return null

    val raw = programme.title.trim()
    val live = raw.endsWith(LIVE_SUFFIX)
    val base = raw.removeSuffix(LIVE_SUFFIX).trim()
    if (base.isEmpty() || replayMarker.containsMatchIn(base) || nonEventMarker.containsMatchIn(base)) return null
    // Placeholder entries are titled after the channel itself ("DAZN 1").
    val normalizedBase = normalizeText(base)
    if (normalizedBase.isEmpty() || normalizedBase == EpgMatcher.normalize(channel.id.substringBeforeLast('.')) ||
        channel.displayNames.any { EpgMatcher.normalize(it) == normalizedBase }) return null

    var rest = base
    var prefixLabel: String? = null
    var prefixKind: SportsKind? = null
    var women = false
    val prefix = sportPrefix.matchEntire(base)
    if (prefix != null) {
        val label = prefix.groupValues[1].trim()
        val sport = womenSuffix.replace(label, "")
        val kind = knownSports[EpgMatcher.normalize(sport)]
        // An unknown prefix ("NASCAR Cup Series:") is only trusted on a sports channel.
        if (kind != null || sportsChannel) {
            rest = prefix.groupValues[2].trim()
            prefixLabel = label
            prefixKind = kind
            women = sport.length != label.length
        }
    }

    val parts = rest.split(" - ").map(String::trim).filter(String::isNotEmpty)
    val head = parts.firstOrNull() ?: return null
    val sides = head.split(teamSeparator, limit = 2).map(String::trim)
    val hasTeams = sides.size == 2 && sides.all(String::isNotEmpty)
    if (!sportsChannel && !hasTeams && prefixKind == null) return null

    val competition = guideCompetitionName(parts.drop(1).joinToString(" - "))
    val sportLabel: String?
    val kind: SportsKind
    when {
        prefixKind != null -> {
            sportLabel = prefixLabel
            kind = prefixKind
        }
        hasTeams && americanFootball.containsMatchIn(competition) -> {
            sportLabel = "Futebol Americano"
            kind = SportsKind.OTHER
        }
        // No prefix and "x": football. An unknown prefix on a match title is a competition, not a sport.
        hasTeams -> {
            sportLabel = "Futebol"
            kind = SportsKind.FOOTBALL
        }
        else -> {
            sportLabel = prefixLabel
            kind = SportsKind.OTHER
        }
    }
    val ageGroup = guideAgeGroup.find(base)?.groupValues?.get(1)
    val sportKey = EpgMatcher.normalize(sportLabel?.let { womenSuffix.replace(it, "") }.orEmpty())
    val eventKey = if (hasTeams) "${guideTeamKey(sides[0])}|${guideTeamKey(sides[1])}" else normalizeText(head)
    return GuideAiring(
        channelId = channel.id,
        title = raw,
        startEpochMilli = programme.startEpochMilli,
        stopEpochMilli = programme.stopEpochMilli,
        live = live,
        sportsChannel = sportsChannel,
        sportLabel = sportLabel,
        kind = kind,
        explicitSport = prefixKind != null,
        home = if (hasTeams) sides[0] else head,
        away = if (hasTeams) sides[1] else "",
        competition = competition.ifEmpty { prefixLabel?.takeIf { hasTeams && prefixKind == null }.orEmpty() },
        women = women,
        ageGroup = ageGroup,
        key = "$sportKey|$eventKey|${ageGroup.orEmpty()}",
        matchText = normalizeText(if (hasTeams) head else base)
    )
}

/**
 * Parsed sports airings per loaded guide day. The home row recomputes every 30 s; the title parsing runs
 * once per day partition (identified by its programme map, which the repository keeps for the day's life).
 */
internal object GuideSportsAirings {
    private class Entry(
        val programmes: Map<String, List<EpgProgramme>>,
        val channels: Map<String, EpgChannel>,
        val airings: List<GuideAiring>
    )

    private const val MAX_ENTRIES = 6
    private val entries = ArrayDeque<Entry>()

    /** Number of day partitions parsed so far; lets tests check that a guide day is parsed once. */
    @Volatile
    var parseCount = 0
        private set

    @Synchronized
    fun of(guide: EpgGuide): List<GuideAiring> {
        entries.firstOrNull { it.programmes === guide.programmes && it.channels === guide.channels }
            ?.let { return it.airings }
        val airings = parse(guide)
        parseCount++
        entries.addLast(Entry(guide.programmes, guide.channels, airings))
        if (entries.size > MAX_ENTRIES) entries.removeFirst()
        return airings
    }

    private fun parse(guide: EpgGuide): List<GuideAiring> {
        val seen = HashSet<Triple<String, Long, String>>()
        return guide.programmes.flatMap { (channelId, programmes) ->
            val channel = guide.channels[channelId] ?: return@flatMap emptyList()
            if (isForeignGuide(channel.id, channel.displayNames)) return@flatMap emptyList()
            programmes.mapNotNull { programme ->
                parseGuideAiring(programme, channel)
                    // The guide lists some entries twice (same channel, start and title).
                    ?.takeIf { seen.add(Triple(it.channelId, it.startEpochMilli, it.title)) }
            }
        }
    }
}

private class GuideHit(
    val airing: GuideAiring,
    val channel: PlaylistChannel,
    val start: Long,
    val end: Long,
    val airingNow: Boolean,
    val fixture: SportsFixture?
)

/** "England U21" and "Portugal Women" are looked up as the country; the marker is compared separately. */
private fun fixtureTeamNames(name: String): Set<String> =
    teamNames(fixtureTeamMarker.replace(name, " ").trim().ifEmpty { name })

private fun fixtureMatchesAiring(
    fixture: SportsFixture,
    names: Pair<Set<String>, Set<String>>,
    airing: GuideAiring,
    start: Long,
    end: Long
): Boolean {
    // The kickoff must fall inside this broadcast, so a later unlabelled rerun never matches.
    if (fixture.kickoffUtc.toEpochMilli() !in (start - KICKOFF_LEAD_MILLIS) until end) return false
    if (airing.explicitSport && airing.kind != fixture.sport) return false
    val (home, away) = names
    if (home.none { airing.matchText.contains(it) } || away.none { airing.matchText.contains(it) }) return false
    val fixtureText = "${fixture.homeTeam} ${fixture.awayTeam} ${fixture.competition}"
    return fixtureAgeGroup.find(fixtureText)?.groupValues?.get(1) == airing.ageGroup &&
        fixtureWomen.containsMatchIn(fixtureText) == airing.women
}

/**
 * Sports events read from the guide that are on air at [now] or start before [upcomingUntil].
 *
 * - `(Direto)` airings are live events and need no ESPN fixture.
 * - Airings without it are reruns unless an ESPN fixture with the same teams kicks off inside them
 *   (live football on DAZN and F1 are listed without the suffix).
 * - Match airings under 30 minutes are cut-downs.
 * - The same event on several channels is one entry: sports channel first, free-to-air as alternate.
 */
internal fun guideSportsMatches(
    fixtures: List<SportsFixture>,
    guides: List<EpgGuide>,
    channels: List<PlaylistChannel>,
    mappings: Map<String, EpgChannelMapping>,
    now: Instant,
    correctionMinutes: Int = 0,
    upcomingUntil: Instant = now
): List<SportsEpgMatch> {
    val candidates = fixtures.filter {
        it.status == FixtureStatus.SCHEDULED || it.status == FixtureStatus.LIVE
    }
    val namesByFixture = candidates.associateWith { fixtureTeamNames(it.homeTeam) to fixtureTeamNames(it.awayTeam) }
    val nowMillis = now.toEpochMilli()
    val untilMillis = upcomingUntil.toEpochMilli()
    val correctionMillis = correctionMinutes * 60_000L
    val includeUpcoming = upcomingUntil > now

    val hits = mutableListOf<GuideHit>()
    val seen = HashSet<Triple<String, Long, String>>()
    for (guide in guides) {
        val airings = GuideSportsAirings.of(guide)
        if (airings.isEmpty()) continue
        val channelByGuideId = HashMap<String, PlaylistChannel>()
        for ((channel, guideChannel) in resolveGuideChannels(guide, channels, mappings, includeUpcoming)) {
            channelByGuideId.putIfAbsent(guideChannel.id, channel)
        }
        for (airing in airings) {
            val channel = channelByGuideId[airing.channelId] ?: continue
            val start = airing.startEpochMilli + correctionMillis
            val end = (airing.stopEpochMilli ?: (airing.startEpochMilli + DEFAULT_AIRING_MILLIS)) + correctionMillis
            val airingNow = nowMillis in start until end
            if (!airingNow && start !in nowMillis until untilMillis) continue
            if (airing.hasTeams && end - start < MIN_MATCH_AIRING_MILLIS) continue
            // A programme running past midnight can sit in two day partitions.
            if (!seen.add(Triple(airing.channelId, airing.startEpochMilli, airing.title))) continue
            val fixture = candidates.firstOrNull {
                fixtureMatchesAiring(it, namesByFixture.getValue(it), airing, start, end)
            }
            if (!airing.live && fixture == null) continue
            hits += GuideHit(airing, channel, start, end, airingNow, fixture)
        }
    }

    val groups = mutableListOf<MutableList<GuideHit>>()
    for (hit in hits.sortedBy { it.start }) {
        val group = groups.firstOrNull { group ->
            hit.fixture != null && group.any { it.fixture?.id == hit.fixture.id } ||
                group.first().airing.key == hit.airing.key &&
                abs(group.first().start - hit.start) <= SAME_EVENT_START_MILLIS
        }
        if (group != null) group += hit else groups += mutableListOf(hit)
    }

    return groups.map { group ->
        val ordered = group.sortedWith(
            compareByDescending<GuideHit> { it.airingNow }
                .thenByDescending { it.airing.sportsChannel }
                .thenBy { it.start }
        )
        val primary = ordered.first()
        val live = primary.airingNow
        val playlistChannels = ordered.map { it.channel }.distinct()
        val espn = group.firstNotNullOfOrNull { it.fixture }
        val fixture = espn ?: primary.airing.let { airing ->
            val firstStart = group.minOf { it.airing.startEpochMilli }
            SportsFixture(
                id = "$GUIDE_FIXTURE_ID_PREFIX${airing.key}-${firstStart / 60_000L}",
                sport = airing.kind,
                competition = airing.competition,
                competitionLogoUrl = null,
                homeTeam = airing.home,
                awayTeam = airing.away,
                homeTeamLogoUrl = null,
                awayTeamLogoUrl = null,
                kickoffUtc = Instant.ofEpochMilli(group.minOf { it.start }),
                broadcasterOfficial = primary.channel.name,
                status = if (live) FixtureStatus.LIVE else FixtureStatus.SCHEDULED
            )
        }
        SportsEpgMatch(
            fixture = fixture,
            channel = primary.channel,
            live = live,
            alternateChannels = playlistChannels.drop(1),
            sportLabel = if (espn == null) primary.airing.sportLabel else null,
            guideAirings = group.map { GuideAiringWindow(it.channel, it.start, it.end) }
        )
    }
}

/**
 * The "Matches on TV" row: guide events first, plus ESPN fixtures matched to a programme the guide parser
 * did not turn into an event (other channels, other title formats). Live first, then by start time.
 */
internal fun matchesOnTv(
    fixtures: List<SportsFixture>,
    guides: List<EpgGuide>,
    channels: List<PlaylistChannel>,
    mappings: Map<String, EpgChannelMapping>,
    now: Instant,
    correctionMinutes: Int = 0,
    upcomingUntil: Instant = now,
    limit: Int = MATCHES_ON_TV_LIMIT
): List<SportsEpgMatch> {
    val guideEvents = guideSportsMatches(fixtures, guides, channels, mappings, now, correctionMinutes, upcomingUntil)
    val fixtureIds = guideEvents.mapTo(HashSet()) { it.fixture.id }
    val espnOnly = liveEpgMatches(fixtures, guides, channels, mappings, now, correctionMinutes, upcomingUntil)
        .filter { match ->
            val kickoff = match.fixture.kickoffUtc.toEpochMilli()
            // Skip a fixture already shown, and one matched to a programme a guide event already covers.
            match.fixture.id !in fixtureIds && guideEvents.none { event ->
                event.guideAirings.any {
                    it.channel == match.channel && kickoff in (it.start - KICKOFF_LEAD_MILLIS) until it.end
                }
            }
        }
    return (guideEvents + espnOnly)
        .sortedWith(compareByDescending<SportsEpgMatch> { it.live }.thenBy { it.fixture.kickoffUtc })
        .take(limit)
}

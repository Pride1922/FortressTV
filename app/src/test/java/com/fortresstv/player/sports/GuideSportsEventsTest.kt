package com.fortresstv.player.sports

import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.epg.XmlTvParser
import com.fortresstv.player.playlist.PlaylistChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/** Titles and times are copied from the real guide of 12–16 Nov 2026. */
class GuideSportsEventsTest {

    private val guide: EpgGuide = XmlTvParser.parse(
        XMLTV.byteInputStream(),
        referenceTimeMillis = at("2026-11-13T12:00:00Z").toEpochMilli(),
        fallbackSourceZone = ZoneOffset.UTC
    )

    private val channels = listOf(
        "PT: Sport TV 1" to "sporttv1.pt", "PT: Sport TV 2" to "sporttv2.pt", "PT: Sport TV 3" to "sporttv3.pt",
        "PT: Sport TV 4" to "sporttv4.pt", "PT: Sport TV 5" to "sporttv5.pt", "PT: Sport TV 6" to "sporttv6.pt",
        "PT: Sport TV 7" to "SPT7HD", "PT: DAZN 1" to "elevensports1.pt", "PT: DAZN 5" to "elevensports5.pt",
        "PT: Eurosport 1" to "eurosport1.pt", "PT: BTV" to "benficatv.pt", "PT: Sporting TV" to "sportingtv.pt",
        "PT: Porto Canal" to "porto.pt", "PT: Canal 11" to "canal11.pt", "PT: A Bola TV" to "abolatv.pt",
        "PT: RTP 1" to "rtp1.pt", "PT: SIC" to "sic.pt", "PT: TVI" to "tvi.pt"
    ).mapIndexed { index, (name, tvgId) -> PlaylistChannel(index, name, "", tvgId = tvgId) }

    private fun channel(tvgId: String) = channels.first { it.tvgId == tvgId }

    private fun events(now: String, fixtures: List<SportsFixture> = emptyList(), hours: Long = 24) =
        guideSportsMatches(
            fixtures, listOf(guide), channels, emptyMap(), at(now), upcomingUntil = at(now).plusSeconds(hours * 3600)
        )

    private fun List<SportsEpgMatch>.titled(home: String, away: String) =
        filter { it.fixture.homeTeam == home && it.fixture.awayTeam == away }

    private fun fixture(
        home: String,
        away: String,
        kickoff: String,
        sport: SportsKind = SportsKind.FOOTBALL,
        id: String = "espn-$home-$away"
    ) = SportsFixture(
        id, sport, "ESPN competition", "https://logo/competition.png", home, away,
        "https://logo/home.png", "https://logo/away.png", at(kickoff), "", FixtureStatus.SCHEDULED
    )

    @Test
    fun directoAiringIsALiveEventWithoutAnyEspnFixture() {
        val match = events("2026-11-14T20:00:00Z").titled("Portugal", "Dinamarca").single()

        assertTrue(match.live)
        assertTrue(match.fixture.id.startsWith(GUIDE_FIXTURE_ID_PREFIX))
        assertEquals("Dinamarca", match.fixture.awayTeam)
        assertEquals("UEFA Nations League", match.fixture.competition)
        assertEquals(SportsKind.FOOTBALL, match.fixture.sport)
        assertEquals("Futebol", match.sportLabel)
        assertEquals(FixtureStatus.LIVE, match.fixture.status)
        assertNull(match.fixture.homeTeamLogoUrl)
    }

    @Test
    fun upcomingDirectoAiringIsListedButNotLive() {
        val match = events("2026-11-14T12:00:00Z").titled("Portugal", "Dinamarca").single()

        assertFalse(match.live)
        assertEquals(FixtureStatus.SCHEDULED, match.fixture.status)
    }

    @Test
    fun sameMatchOnSportsAndFreeToAirChannelsIsOneCardWithBothChannels() {
        // sporttv1 says "UEFA Nations League" from 19:47, rtp1 says "Liga Das Nações" from 19:45.
        val match = events("2026-11-14T20:00:00Z").titled("Portugal", "Dinamarca").single()

        assertEquals(channel("sporttv1.pt"), match.channel)
        assertEquals(listOf(channel("rtp1.pt")), match.alternateChannels)
        assertEquals(at("2026-11-14T19:45:00Z"), match.fixture.kickoffUtc)
    }

    @Test
    fun rerunsWithTheSameTitleButNoDiretoSuffixAreHidden() {
        // 15 Nov 02:00 and 10:00 on sporttv1: identical title and description, no "(Direto)".
        val later = "2026-11-15T01:30:00Z"
        assertEquals(emptyList<SportsEpgMatch>(), events(later, hours = 12).titled("Portugal", "Dinamarca"))
        // Even an ESPN fixture for the match does not revive a rerun: its kickoff is not inside the rerun.
        val espn = fixture("Portugal", "Denmark", "2026-11-14T19:45:00Z")
        val withEspn = events(later, listOf(espn), 12)
        assertEquals(emptyList<SportsEpgMatch>(), withEspn.filter { it.channel == channel("sporttv1.pt") })
        assertEquals(emptyList<SportsEpgMatch>(), withEspn.filter { it.fixture.id == espn.id })
        // While the live airing is on, the same fixture enriches it instead (crests, kickoff, both channels).
        val live = events("2026-11-14T20:00:00Z", listOf(espn)).single { it.fixture.id == espn.id }
        assertEquals(listOf(channel("sporttv1.pt"), channel("rtp1.pt")), listOf(live.channel) + live.alternateChannels)
        assertNull(live.sportLabel)
    }

    @Test
    fun previewsHighlightsMagazinesAndPlaceholdersAreNeverEvents() {
        val rejected = listOf(
            "Antevisão: Portugal x Dinamarca - UEFA Nations League",
            "Rescaldo: Portugal x Dinamarca - UEFA Nations League",
            "MotoGP: GP Qatar (Resumo)",
            "NASCAR Cup Series: Championship Race (Resumo)",
            "Noite Desportiva",
            "A Bola Da Noite",
            "Info Sporting TV",
            "Benfica 24 Horas",
            "Últimas Notícias",
            "CNN Mais Futebol",
            "Premier League Stories",
            "Rico Momento",
            "Fora de Horas: Sem Transmissão",
            "DAZN 1",
            "Big Brother All Stars - Direto",
            "Santa Missa - Direto",
            "SIC Notícias Direto",
            "V+ Futebol: Livre E Direto"
        )
        val guideTitles = guide.programmes.values.flatten().map { it.title }.toSet()
        assertEquals("fixture is missing titles", emptyList<String>(), rejected.filterNot { it in guideTitles })

        val parsed = GuideSportsAirings.of(guide).map { it.title }.toSet()
        assertEquals(emptyList<String>(), rejected.filter { it in parsed })
        // Nothing but "(Direto)" airings reaches the row when ESPN knows no fixture.
        val shown = events("2026-11-13T06:00:00Z", hours = 72)
        assertTrue(shown.isNotEmpty())
        assertTrue(shown.all { it.fixture.id.startsWith(GUIDE_FIXTURE_ID_PREFIX) })
        assertTrue(GuideSportsAirings.of(guide).filter { it.live }.all { it.title.endsWith("(Direto)") })
    }

    @Test
    fun vMaisFutebolLivreEDiretoIsRejected() {
        val programme = guide.programmes.getValue("tvi.pt").single { it.title == "V+ Futebol: Livre E Direto" }
        assertNull(parseGuideAiring(programme, guide.channels.getValue("tvi.pt")))
        assertEquals(emptyList<GuideAiring>(), GuideSportsAirings.of(guide).filter { it.channelId == "tvi.pt" })
        assertEquals(
            emptyList<SportsEpgMatch>(),
            events("2026-11-13T06:00:00Z", hours = 72).filter { it.channel == channel("tvi.pt") }
        )
    }

    @Test
    fun daznFootballNeedsAnEspnKickoffInsideTheAiring() {
        // elevensports1, Fri 13 Nov: football carries no "(Direto)", only basketball does.
        val now = "2026-11-13T18:30:00Z"
        val withoutEspn = events(now, hours = 8)
        assertEquals(listOf("Vitória Guimarães"), withoutEspn.map { it.fixture.homeTeam })
        assertEquals("Basquetebol", withoutEspn.single().sportLabel)
        assertEquals(SportsKind.BASKETBALL, withoutEspn.single().fixture.sport)

        // 22:35–00:30 "Atl. Madrid x Barcelona - LaLiga", listed twice in the guide.
        val live = fixture("Atlético Madrid", "Barcelona", "2026-11-13T22:45:00Z")
        val match = events(now, listOf(live), 8).single { it.fixture.id == live.id }
        assertEquals(live, match.fixture)
        assertEquals(channel("elevensports1.pt"), match.channel)
        assertNull(match.sportLabel)

        // Same teams, but the kickoff is a day later: the airing is a rerun.
        val tomorrow = fixture("Atlético Madrid", "Barcelona", "2026-11-14T22:45:00Z")
        assertEquals(1, events(now, listOf(tomorrow), 8).size)
        // 18:00–19:50 "Barcelona x Aston Villa" with no fixture at all stays hidden while it airs.
        assertEquals(emptyList<SportsEpgMatch>(), withoutEspn.titled("Barcelona", "Aston Villa"))
    }

    @Test
    fun cutDownAiringIsHiddenEvenWithAMatchingKickoff() {
        // 19:50–20:10 "Shakhtar Donetsk x Sporting CP - UEFA Champions League": 20 minutes.
        val espn = fixture("Shakhtar Donetsk", "Sporting CP", "2026-11-13T19:55:00Z")
        assertEquals(
            emptyList<SportsEpgMatch>(),
            events("2026-11-13T19:55:00Z", listOf(espn), 1).filter { it.fixture.id == espn.id }
        )
    }

    @Test
    fun formulaOneWithoutDiretoIsShownThroughItsEspnFixture() {
        val now = "2026-11-15T17:10:00Z"
        assertEquals(emptyList<SportsEpgMatch>(), events(now).filter { it.channel == channel("elevensports5.pt") })

        val race = fixture("São Paulo Grand Prix", "Corrida Principal", "2026-11-15T17:00:00Z", SportsKind.MOTORSPORT)
        val match = events(now, listOf(race)).single { it.channel == channel("elevensports5.pt") }
        assertEquals(race, match.fixture)
        assertTrue(match.live)
    }

    @Test
    fun identicalGuideEntriesAreOneEvent() {
        // The rugby airing is in the guide twice (same channel, start and title).
        val title = "Rugby: França x África do Sul - Nations Championship (Direto)"
        assertEquals(2, guide.programmes.getValue("sporttv5.pt").count { it.title == title })
        assertEquals(1, GuideSportsAirings.of(guide).count { it.title == title })

        val match = events("2026-11-14T20:30:00Z").titled("França", "África do Sul").single()
        assertEquals("Rugby", match.sportLabel)
        assertEquals(SportsKind.OTHER, match.fixture.sport)
        assertEquals(emptyList<PlaylistChannel>(), match.alternateChannels)
    }

    @Test
    fun underTwentyOneMatchIsNotMergedWithTheSeniorTeams() {
        // canal11: "Inglaterra x Portugal - Seleção Sub-21 - Jogo Prep. (Direto)", 20:30–22:30.
        val senior = fixture("England", "Portugal", "2026-11-14T20:45:00Z")
        val now = at("2026-11-14T21:00:00Z")
        val row = matchesOnTv(listOf(senior), listOf(guide), channels, emptyMap(), now, upcomingUntil = now)

        val under21 = row.single { it.channel == channel("canal11.pt") }
        assertTrue(under21.fixture.id.startsWith(GUIDE_FIXTURE_ID_PREFIX))
        assertEquals("Seleção Sub-21 - Jogo Prep.", under21.fixture.competition)
        assertEquals(emptyList<SportsEpgMatch>(), row.filter { it.fixture.id == senior.id })

        // A fixture that says U21 itself does enrich the airing.
        val espnUnder21 = fixture("England U21", "Portugal U21", "2026-11-14T20:45:00Z")
        val enriched = matchesOnTv(listOf(espnUnder21), listOf(guide), channels, emptyMap(), now, upcomingUntil = now)
        assertEquals(espnUnder21, enriched.single { it.channel == channel("canal11.pt") }.fixture)

        // And the event key keeps the age group, so the senior match is a different event.
        val canal11 = guide.channels.getValue("canal11.pt")
        val programme = guide.programmes.getValue("canal11.pt").first { it.title.startsWith("Inglaterra x Portugal") }
        val seniorAiring = parseGuideAiring(programme.copy(title = "Inglaterra x Portugal - Jogo Prep. (Direto)"), canal11)
        assertNotEquals(parseGuideAiring(programme, canal11)!!.key, seniorAiring!!.key)
    }

    @Test
    fun sportComesFromThePrefixAndDefaultsToFootballForMatchTitles() {
        val airings = GuideSportsAirings.of(guide).filter { it.live }.associateBy { it.title.removeSuffix(" (Direto)") }
        fun airing(title: String) = airings.getValue(title)

        val expected = mapOf(
            "Sérvia x Alemanha - UEFA Nations League" to "Futebol",
            "Cabo Verde x Libéria - Taça Nações Africanas Qualificação" to "Futebol",
            "Farense x Sporting - Liga Next Gen" to "Futebol",
            "Futebol Fem.: Portugal x Rep. Irlanda - Torneio Sub-16" to "Futebol Fem.",
            "Futsal: Benfica x Rio Ave - Liga Placard" to "Futsal",
            "Futsal: Sporting CP x Bintang Timur - Copa Intercontinental" to "Futsal",
            "Andebol Fem.: Benfica x Házená Kynzvart - EHF European League" to "Andebol Fem.",
            "Hóquei: AD Valongo x Sporting CP - Camp. Nacional" to "Hóquei",
            "Voleibol Fem.: FC Porto x SC Braga - Camp. Nac. 1.ª Divisão" to "Voleibol Fem.",
            "Basquetebol: Philadelphia 76ERS x Toronto Raptors - NBA" to "Basquetebol",
            "Giants x Commanders - NFL Domestic" to "Futebol Americano",
            "Padel: Dubai P1 - Premier Padel" to "Padel",
            "Ténis: Estocolmo - ATP World Tour 250" to "Ténis",
            "Golfe: DP World Tour Championship 2.º Dia - DP World Tour" to "Golfe",
            "Judo: Zagreb 1.º Dia - Judo Grand Prix" to "Judo",
            "Esqui Alpino: Taça Mundo - Levi - Slalom Masculino Manga 1" to "Esqui Alpino",
            "Triatlo: Abu Dhabi Estafeta - World Triathlon Champ. Series" to "Triatlo",
            "Combate: Zuffa Boxing 13 Preliminares - Zuffa Boxing" to "Combate"
        )
        assertEquals(expected, expected.mapValues { (title, _) -> airing(title).sportLabel })

        assertEquals(SportsKind.TENNIS, airing("Ténis: Estocolmo - ATP World Tour 250").kind)
        assertEquals(SportsKind.BASKETBALL, airing("Basquetebol: Philadelphia 76ERS x Toronto Raptors - NBA").kind)
        assertEquals(SportsKind.OTHER, airing("Giants x Commanders - NFL Domestic").kind)

        val women = airing("Futebol Fem.: Portugal x Rep. Irlanda - Torneio Sub-16")
        assertTrue(women.women)
        assertEquals("16", women.ageGroup)
        assertEquals("Portugal" to "Rep. Irlanda", women.home to women.away)

        val golf = airing("Golfe: DP World Tour Championship 2.º Dia - DP World Tour")
        assertEquals("DP World Tour Championship 2.º Dia" to "", golf.home to golf.away)
        assertEquals("DP World Tour", golf.competition)

        // No "x" and no competition: an event on its own.
        val fight = airing("One Friday Fight")
        assertEquals("One Friday Fight" to "", fight.home to fight.away)
        assertEquals("", fight.competition)
        assertNull(fight.sportLabel)
    }

    @Test
    fun teamAndCompetitionAliasesNormalise() {
        assertEquals(guideTeamKey("Sporting"), guideTeamKey("Sporting CP"))
        assertEquals(guideTeamKey("Porto"), guideTeamKey("FC Porto"))
        assertEquals(guideTeamKey("Benfica"), guideTeamKey("SL Benfica"))
        assertEquals(guideTeamKey("Atlético Madrid"), guideTeamKey("Atl. Madrid"))
        assertEquals(guideTeamKey("Irlanda"), guideTeamKey("Rep. Irlanda"))
        assertNotEquals(guideTeamKey("Sporting"), guideTeamKey("SC Braga"))

        assertEquals("UEFA Nations League", guideCompetitionName("Liga Das Nações"))
        assertEquals("Campeonato Nacional", guideCompetitionName("Camp. Nacional"))
        assertEquals("Campeonato Nacional", guideCompetitionName("Campeonato Nacional"))
        assertEquals("Campeonato Nacional 1.ª Divisão", guideCompetitionName("Camp. Nac. 1.ª Divisão"))
        assertEquals("Liga Placard", guideCompetitionName("Liga Placard"))
    }

    @Test
    fun rowListsLiveEventsFirstThenByStartAndIsCapped() {
        val now = at("2026-11-14T20:00:00Z")
        val row = matchesOnTv(emptyList(), listOf(guide), channels, emptyMap(), now, upcomingUntil = now.plusSeconds(86_400))

        assertTrue(row.size > 3)
        assertEquals(row.sortedWith(compareByDescending<SportsEpgMatch> { it.live }.thenBy { it.fixture.kickoffUtc }), row)
        assertEquals(setOf("Portugal"), row.filter { it.live }.map { it.fixture.homeTeam }.toSet())
        assertEquals(row.size, row.map { it.fixture.id }.toSet().size)
        assertTrue(row.all { it.fixture.kickoffUtc < now.plusSeconds(86_400) })

        val capped = matchesOnTv(
            emptyList(), listOf(guide), channels, emptyMap(), now, upcomingUntil = now.plusSeconds(86_400), limit = 3
        )
        assertEquals(row.take(3), capped)
    }

    @Test
    fun guideDayIsParsedOnceAcrossRecomputes() {
        // HomeScreen wraps each loaded day in a fresh EpgGuide every 30 s; the programme map is the same.
        events("2026-11-14T20:00:00Z")
        val parsed = GuideSportsAirings.parseCount
        events("2026-11-14T20:00:30Z")
        guideSportsMatches(
            emptyList(), listOf(guide.copy(epochDay = 1)), channels, emptyMap(), at("2026-11-14T20:01:00Z")
        )
        assertEquals(parsed, GuideSportsAirings.parseCount)
    }

    private companion object {
        fun at(instant: String): Instant = Instant.parse(instant)

        private val channelNames = linkedMapOf(
            "sporttv1.pt" to "Sport TV 1", "sporttv2.pt" to "Sport TV 2", "sporttv3.pt" to "Sport TV 3",
            "sporttv4.pt" to "Sport TV 4", "sporttv5.pt" to "Sport TV 5", "sporttv6.pt" to "Sport TV 6",
            "SPT7HD" to "Sport TV 7", "elevensports1.pt" to "DAZN 1", "elevensports5.pt" to "DAZN 5",
            "eurosport1.pt" to "Eurosport 1", "benficatv.pt" to "BTV", "sportingtv.pt" to "Sporting TV",
            "porto.pt" to "Porto Canal", "canal11.pt" to "Canal 11", "abolatv.pt" to "A Bola TV",
            "rtp1.pt" to "RTP 1", "sic.pt" to "SIC", "tvi.pt" to "TVI"
        )

        /** channel | start | stop (yyyyMMddHHmm, UTC) | title */
        private val programmes = """
            sporttv1.pt|202611141830|202611141947|Antevisão: Portugal x Dinamarca - UEFA Nations League
            sporttv1.pt|202611141947|202611142157|Portugal x Dinamarca - UEFA Nations League (Direto)
            sporttv1.pt|202611142157|202611142230|Rescaldo: Portugal x Dinamarca - UEFA Nations League
            sporttv1.pt|202611150200|202611150350|Portugal x Dinamarca - UEFA Nations League
            sporttv1.pt|202611151000|202611151150|Portugal x Dinamarca - UEFA Nations League
            sporttv1.pt|202611151400|202611151600|Sérvia x Alemanha - UEFA Nations League (Direto)
            sporttv1.pt|202611151600|202611151700|Noite Desportiva
            rtp1.pt|202611141945|202611142155|Portugal x Dinamarca - Liga Das Nações (Direto)
            rtp1.pt|202611151000|202611151100|Santa Missa - Direto
            sic.pt|202611141800|202611141900|SIC Notícias Direto
            tvi.pt|202611141900|202611142000|V+ Futebol: Livre E Direto
            tvi.pt|202611142000|202611142300|Big Brother All Stars - Direto
            sporttv2.pt|202611141600|202611141800|Cabo Verde x Libéria - Taça Nações Africanas Qualificação (Direto)
            sporttv2.pt|202611141800|202611142000|Futsal: Benfica x Rio Ave - Liga Placard (Direto)
            sporttv2.pt|202611150000|202611150230|Basquetebol: Philadelphia 76ERS x Toronto Raptors - NBA (Direto)
            sporttv2.pt|202611150230|202611150330|NASCAR Cup Series: Championship Race (Resumo)
            sporttv3.pt|202611140800|202611141300|Golfe: DP World Tour Championship 2.º Dia - DP World Tour (Direto)
            sporttv4.pt|202611141200|202611141500|Ténis: Estocolmo - ATP World Tour 250 (Direto)
            sporttv4.pt|202611150700|202611150900|Triatlo: Abu Dhabi Estafeta - World Triathlon Champ. Series (Direto)
            sporttv4.pt|202611150900|202611151000|MotoGP: GP Qatar (Resumo)
            sporttv5.pt|202611131230|202611131500|One Friday Fight (Direto)
            sporttv5.pt|202611142010|202611142200|Rugby: França x África do Sul - Nations Championship (Direto)
            sporttv5.pt|202611142010|202611142200|Rugby: França x África do Sul - Nations Championship (Direto)
            sporttv5.pt|202611150100|202611150300|Combate: Zuffa Boxing 13 Preliminares - Zuffa Boxing (Direto)
            sporttv6.pt|202611140900|202611141200|Judo: Zagreb 1.º Dia - Judo Grand Prix (Direto)
            SPT7HD|202611141300|202611141600|Padel: Dubai P1 - Premier Padel (Direto)
            eurosport1.pt|202611150900|202611151000|Esqui Alpino: Taça Mundo - Levi - Slalom Masculino Manga 1 (Direto)
            canal11.pt|202611142030|202611142230|Inglaterra x Portugal - Seleção Sub-21 - Jogo Prep. (Direto)
            canal11.pt|202611151100|202611151300|Futebol Fem.: Portugal x Rep. Irlanda - Torneio Sub-16 (Direto)
            canal11.pt|202611151500|202611151700|Farense x Sporting - Liga Next Gen (Direto)
            sportingtv.pt|202611141700|202611141900|Futsal: Sporting CP x Bintang Timur - Copa Intercontinental (Direto)
            sportingtv.pt|202611141900|202611141930|Info Sporting TV
            benficatv.pt|202611141500|202611141700|Andebol Fem.: Benfica x Házená Kynzvart - EHF European League (Direto)
            benficatv.pt|202611141700|202611141800|Benfica 24 Horas
            porto.pt|202611141600|202611141800|Voleibol Fem.: FC Porto x SC Braga - Camp. Nac. 1.ª Divisão (Direto)
            abolatv.pt|202611142200|202611142300|A Bola Da Noite
            abolatv.pt|202611142300|202611142330|Últimas Notícias
            abolatv.pt|202611142330|202611150000|CNN Mais Futebol
            abolatv.pt|202611150000|202611150030|Premier League Stories
            abolatv.pt|202611150030|202611150100|Rico Momento
            elevensports1.pt|202611131800|202611131950|Barcelona x Aston Villa - UEFA Champions League
            elevensports1.pt|202611131950|202611132010|Shakhtar Donetsk x Sporting CP - UEFA Champions League
            elevensports1.pt|202611132025|202611132235|Basquetebol: Vitória Guimarães x FC Porto - Liga Portuguesa (Direto)
            elevensports1.pt|202611132235|202611140030|Atl. Madrid x Barcelona - LaLiga
            elevensports1.pt|202611132235|202611140030|Atl. Madrid x Barcelona - LaLiga
            elevensports1.pt|202611140030|202611140600|DAZN 1
            elevensports1.pt|202611140600|202611141500|Fora de Horas: Sem Transmissão
            elevensports1.pt|202611141500|202611141700|Hóquei: AD Valongo x Sporting CP - Camp. Nacional (Direto)
            elevensports1.pt|202611151800|202611152115|Giants x Commanders - NFL Domestic (Direto)
            elevensports5.pt|202611151655|202611151900|Grande Prêmio De São Paulo - Corrida - Formula 1
        """.trimIndent()

        /** A small XMLTV document in the shape of the real guide: no <category>, same <desc> for reruns. */
        val XMLTV: String = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tv>\n")
            for ((id, name) in channelNames) {
                append("  <channel id=\"$id\"><display-name>$name</display-name></channel>\n")
            }
            for (line in programmes.lines()) {
                val (channel, start, stop, title) = line.split("|")
                append("  <programme start=\"${start}00 +0000\" stop=\"${stop}00 +0000\" channel=\"$channel\">")
                append("<title lang=\"pt\">$title</title>")
                append("<desc lang=\"pt\">Transmissão do encontro a contar para a competição.</desc>")
                append("</programme>\n")
            }
            append("</tv>\n")
        }
    }
}

package com.fortresstv.player.sports

import com.fortresstv.player.epg.EpgChannel
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.epg.EpgProgramme
import com.fortresstv.player.playlist.PlaylistChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class SportsBindingResolverTest {

    private fun sampleFixture(
        broadcaster: String = "Sport TV 1",
        home: String = "SL Benfica",
        away: String = "FC Porto",
        kickoff: Instant = Instant.now().plusSeconds(3600)
    ) = SportsFixture(
        id = "fix-1",
        sport = SportsKind.FOOTBALL,
        competition = "Primeira Liga",
        competitionLogoUrl = null,
        homeTeam = home,
        awayTeam = away,
        homeTeamLogoUrl = null,
        awayTeamLogoUrl = null,
        kickoffUtc = kickoff,
        broadcasterOfficial = broadcaster,
        status = FixtureStatus.SCHEDULED
    )

    private fun sampleChannel(
        name: String,
        url: String = "http://stream/$name",
        group: String = "Desporto",
        tvgId: String? = null
    ) = PlaylistChannel(
        position = 1,
        name = name,
        streamUrl = url,
        group = group,
        tvgId = tvgId
    )

    @Test
    fun matchBroadcasterDirectly_matchesStandardIptvChannelFormats() {
        val fixture = sampleFixture("Sport TV 1")

        val channels = listOf(
            sampleChannel("PT: RTP 1 HD"),
            sampleChannel("PT | SPORT TV 1 FHD (50fps)"),
            sampleChannel("PT: SPORT TV 2 HD")
        )

        val matched = SportsBindingResolver.matchBroadcasterDirectly(fixture.broadcasterOfficial, channels)
        assertNotNull(matched)
        assertEquals("PT | SPORT TV 1 FHD (50fps)", matched!!.name)
    }

    @Test
    fun matchBroadcasterDirectly_matchesLegacyElevenSportsForDazn() {
        val fixture = sampleFixture("DAZN 1")

        val channels = listOf(
            sampleChannel("PT: ELEVEN SPORTS 1 HD"),
            sampleChannel("PT: SPORT TV 1 HD")
        )

        val matched = SportsBindingResolver.matchBroadcasterDirectly(fixture.broadcasterOfficial, channels)
        assertNotNull(matched)
        assertEquals("PT: ELEVEN SPORTS 1 HD", matched!!.name)
    }

    @Test
    fun matchBroadcasterDirectly_returnsNullWhenChannelNotInList() {
        val fixture = sampleFixture("DAZN 4")

        val channels = listOf(
            sampleChannel("PT: SPORT TV 1 HD"),
            sampleChannel("PT: DAZN 1 HD"),
            sampleChannel("PT: DAZN 2 HD")
        )

        val matched = SportsBindingResolver.matchBroadcasterDirectly(fixture.broadcasterOfficial, channels)
        assertNull(matched)
    }

    @Test
    fun matchViaEpg_resolvesWhenBroadcasterNameIsCustomOrObscure() {
        val kickoff = Instant.now().plusSeconds(1800)
        val fixture = sampleFixture(
            broadcaster = "Canal Desconhecido",
            home = "Sporting CP",
            away = "SC Braga",
            kickoff = kickoff
        )

        val customChannel = sampleChannel(
            name = "Stream Alternativo 03",
            group = "Desporto Especial",
            tvgId = "alt.stream.03"
        )
        val otherChannel = sampleChannel("Noticias 24", group = "Info")

        val epgGuide = EpgGuide(
            channels = mapOf(
                "alt.stream.03" to EpgChannel("alt.stream.03", "Stream Alternativo 03")
            ),
            programmes = mapOf(
                "alt.stream.03" to listOf(
                    EpgProgramme(
                        channelId = "alt.stream.03",
                        title = "Liga Portugal Betclic: Sporting CP vs SC Braga",
                        description = "Transmissão em direto da jornada 11",
                        category = "Desporto",
                        startEpochMilli = kickoff.minusSeconds(900).toEpochMilli(),
                        stopEpochMilli = kickoff.plusSeconds(7200).toEpochMilli()
                    )
                )
            )
        )

        val resolved = SportsBindingResolver.resolve(
            fixture = fixture,
            channels = listOf(otherChannel, customChannel),
            epgGuide = epgGuide
        )

        assertNotNull(resolved)
        assertEquals("Stream Alternativo 03", resolved!!.name)
    }

    @Test
    fun resolve_returnsNullWhenNeitherDirectNorEpgMatches() {
        val fixture = sampleFixture("Sport TV 5")
        val channels = listOf(
            sampleChannel("PT: RTP 1 HD"),
            sampleChannel("PT: SIC HD")
        )

        val resolved = SportsBindingResolver.resolve(fixture, channels, epgGuide = null)
        assertNull(resolved)
    }

    @Test
    fun matchBroadcasterDirectly_matchesBtvAndEurosportAliases() {
        val btvFixture = sampleFixture("BTV", home = "SL Benfica", away = "FC Arouca")
        val euroFixture = sampleFixture("Eurosport 1", home = "Roland Garros", away = "Final")

        val channels = listOf(
            sampleChannel("PT: BENFICA TV HD"),
            sampleChannel("PT: EUROSPORT 1 FHD")
        )

        val matchedBtv = SportsBindingResolver.matchBroadcasterDirectly(btvFixture.broadcasterOfficial, channels)
        val matchedEuro = SportsBindingResolver.matchBroadcasterDirectly(euroFixture.broadcasterOfficial, channels)

        assertNotNull(matchedBtv)
        assertEquals("PT: BENFICA TV HD", matchedBtv!!.name)
        assertNotNull(matchedEuro)
        assertEquals("PT: EUROSPORT 1 FHD", matchedEuro!!.name)
    }

    @Test
    fun filterToPlayableStreams_excludesFixturesWithoutMatchingStream() {
        val f1Playable = sampleFixture("DAZN 1", home = "F1 GP Monaco", away = "Race")
        val f2Unplayable = sampleFixture("Sport TV 6", home = "Padel WPT", away = "Quarterfinal")

        val channels = listOf(
            sampleChannel("PT | DAZN 1 HD"),
            sampleChannel("PT | RTP 1 HD")
        )

        val fixtures = listOf(f1Playable, f2Unplayable)

        val playable = fixtures.mapNotNull { f ->
            val resolved = SportsBindingResolver.resolve(f, channels)
            if (resolved != null) f to resolved else null
        }

        assertEquals(1, playable.size)
        assertEquals("fix-1", playable[0].first.id)
        assertEquals("PT | DAZN 1 HD", playable[0].second.name)
    }

    @Test
    fun matchViaEpg_prefersOfficialSportChannelOverCommentaryChannel() {
        val kickoff = Instant.now().plusSeconds(1800)
        val fixture = sampleFixture(
            broadcaster = "Sport TV 1",
            home = "Vitória SC",
            away = "FC Porto",
            kickoff = kickoff
        )

        val vPlusChannel = sampleChannel(
            name = "PT: V+ TVI",
            tvgId = "vplus.pt"
        )
        val sportTv1Channel = sampleChannel(
            name = "PT: SPORT TV 1 HD",
            tvgId = "sporttv1.pt"
        )

        val epgGuide = EpgGuide(
            channels = mapOf(
                "vplus.pt" to EpgChannel("vplus.pt", "V+ TVI"),
                "sporttv1.pt" to EpgChannel("sporttv1.pt", "SPORT TV 1")
            ),
            programmes = mapOf(
                "vplus.pt" to listOf(
                    EpgProgramme(
                        channelId = "vplus.pt",
                        title = "V+ Futebol: Vitória SC x FC Porto - Antevisão e Análise",
                        description = "Comentários ao jogo entre Vitória e FC Porto",
                        category = "Informação",
                        startEpochMilli = kickoff.minusSeconds(600).toEpochMilli(),
                        stopEpochMilli = kickoff.plusSeconds(5400).toEpochMilli()
                    )
                ),
                "sporttv1.pt" to listOf(
                    EpgProgramme(
                        channelId = "sporttv1.pt",
                        title = "Vitória SC x FC Porto - Primeira Liga (Direto)",
                        description = "Transmissão em direto do jogo da Liga",
                        category = "Desporto",
                        startEpochMilli = kickoff.toEpochMilli(),
                        stopEpochMilli = kickoff.plusSeconds(6300).toEpochMilli()
                    )
                )
            )
        )

        // Put V+ first in the channel list to ensure sorting logic takes precedence over list order
        val resolved = SportsBindingResolver.resolve(
            fixture = fixture,
            channels = listOf(vPlusChannel, sportTv1Channel),
            epgGuide = epgGuide
        )

        assertNotNull(resolved)
        assertEquals("PT: SPORT TV 1 HD", resolved!!.name)
    }

    @Test
    fun matchViaEpg_matchesVPlusWhenItIsOfficialBroadcasterForMoreirense() {
        val kickoff = Instant.now().plusSeconds(1800)
        val fixture = sampleFixture(
            broadcaster = "V+",
            home = "Moreirense FC",
            away = "SL Benfica",
            kickoff = kickoff
        )

        val vPlusChannel = sampleChannel(
            name = "PT: V+ TVI",
            tvgId = "vplus.pt"
        )
        val sportTv1Channel = sampleChannel(
            name = "PT: SPORT TV 1 HD",
            tvgId = "sporttv1.pt"
        )

        val epgGuide = EpgGuide(
            channels = mapOf(
                "vplus.pt" to EpgChannel("vplus.pt", "V+ TVI"),
                "sporttv1.pt" to EpgChannel("sporttv1.pt", "SPORT TV 1")
            ),
            programmes = mapOf(
                "vplus.pt" to listOf(
                    EpgProgramme(
                        channelId = "vplus.pt",
                        title = "Liga Portugal Betclic: Moreirense vs Benfica (Direto)",
                        description = "Transmissão exclusiva em direto",
                        category = "Desporto",
                        startEpochMilli = kickoff.toEpochMilli(),
                        stopEpochMilli = kickoff.plusSeconds(7200).toEpochMilli()
                    )
                )
            )
        )

        val resolved = SportsBindingResolver.resolve(
            fixture = fixture,
            channels = listOf(sportTv1Channel, vPlusChannel),
            epgGuide = epgGuide
        )

        assertNotNull(resolved)
        assertEquals("PT: V+ TVI", resolved!!.name)
    }

    @Test
    fun matchViaEpg_prefersSportTv2OverSportTvPlusStudioShow() {
        val kickoff = Instant.now().plusSeconds(1800)
        val fixture = sampleFixture(
            broadcaster = "Sport TV 2",
            home = "Vitória SC",
            away = "SL Benfica",
            kickoff = kickoff
        )

        val sportTvPlusChannel = sampleChannel(
            name = "PT: SPORT TV + HD",
            tvgId = "sporttvplus.pt"
        )
        val sportTv2Channel = sampleChannel(
            name = "PT: SPORT TV 2 HD",
            tvgId = "sporttv2.pt"
        )

        val epgGuide = EpgGuide(
            channels = mapOf(
                "sporttvplus.pt" to EpgChannel("sporttvplus.pt", "SPORT TV +"),
                "sporttv2.pt" to EpgChannel("sporttv2.pt", "SPORT TV 2")
            ),
            programmes = mapOf(
                "sporttvplus.pt" to listOf(
                    EpgProgramme(
                        channelId = "sporttvplus.pt",
                        title = "Grande Jornada",
                        description = "Acompanhamento ao minuto do jogo Vitória SC vs SL Benfica",
                        category = "Informação",
                        startEpochMilli = kickoff.minusSeconds(900).toEpochMilli(),
                        stopEpochMilli = kickoff.plusSeconds(7200).toEpochMilli()
                    )
                ),
                "sporttv2.pt" to listOf(
                    EpgProgramme(
                        channelId = "sporttv2.pt",
                        title = "Liga Portugal Betclic: Vitória SC x SL Benfica (Direto)",
                        description = "Transmissão exclusiva em direto",
                        category = "Desporto",
                        startEpochMilli = kickoff.toEpochMilli(),
                        stopEpochMilli = kickoff.plusSeconds(6300).toEpochMilli()
                    )
                )
            )
        )

        // Put Sport TV + before Sport TV 2 to ensure channel priority and scoring beat list order
        val resolved = SportsBindingResolver.resolve(
            fixture = fixture,
            channels = listOf(sportTvPlusChannel, sportTv2Channel),
            epgGuide = epgGuide
        )

        assertNotNull(resolved)
        assertEquals("PT: SPORT TV 2 HD", resolved!!.name)
    }

    @Test
    fun matchBroadcasterDirectly_doesNotMatchSportTvPlusWhenTargetIsSportTv() {
        val channels = listOf(
            sampleChannel("PT: SPORT TV + HD"),
            sampleChannel("PT: SPORT TV 1 HD"),
            sampleChannel("PT: SPORT TV 2 HD")
        )

        // Matching "Sport TV 2" must not hit Sport TV +
        val matched2 = SportsBindingResolver.matchBroadcasterDirectly("Sport TV 2", channels)
        assertNotNull(matched2)
        assertEquals("PT: SPORT TV 2 HD", matched2!!.name)

        // Matching "Sport TV +" explicitly must hit Sport TV +
        val matchedPlus = SportsBindingResolver.matchBroadcasterDirectly("Sport TV +", channels)
        assertNotNull(matchedPlus)
        assertEquals("PT: SPORT TV + HD", matchedPlus!!.name)
    }

    @Test
    fun resolveAll_sanitizesScrapedSportTvPlusToSportTvAndBindsToSportTv2ViaEpg() {
        val kickoff = Instant.now().plusSeconds(1800)
        // Scraped card fixture says "Sport TV +" for a Benfica league match
        val fixture = sampleFixture(
            broadcaster = "Sport TV +",
            home = "Vitória SC",
            away = "SL Benfica",
            kickoff = kickoff
        )

        val sportTvPlus = sampleChannel("PT: SPORT TV + HD", tvgId = "sporttvplus.pt")
        val sportTv1 = sampleChannel("PT: SPORT TV 1 HD", tvgId = "sporttv1.pt")
        val sportTv2 = sampleChannel("PT: SPORT TV 2 HD", tvgId = "sporttv2.pt")

        val epgGuide = EpgGuide(
            channels = mapOf(
                "sporttvplus.pt" to EpgChannel("sporttvplus.pt", "SPORT TV +"),
                "sporttv1.pt" to EpgChannel("sporttv1.pt", "SPORT TV 1"),
                "sporttv2.pt" to EpgChannel("sporttv2.pt", "SPORT TV 2")
            ),
            programmes = mapOf(
                "sporttvplus.pt" to listOf(
                    EpgProgramme(
                        channelId = "sporttvplus.pt",
                        title = "Super Futebol",
                        description = "Debate e acompanhamento do jogo Vitória SC vs SL Benfica",
                        category = "Informação",
                        startEpochMilli = kickoff.toEpochMilli(),
                        stopEpochMilli = kickoff.plusSeconds(7200).toEpochMilli()
                    )
                ),
                "sporttv1.pt" to listOf(
                    EpgProgramme(
                        channelId = "sporttv1.pt",
                        title = "Fórmula 1: GP Brasil",
                        description = "Corrida",
                        category = "Desporto",
                        startEpochMilli = kickoff.toEpochMilli(),
                        stopEpochMilli = kickoff.plusSeconds(7200).toEpochMilli()
                    )
                ),
                "sporttv2.pt" to listOf(
                    EpgProgramme(
                        channelId = "sporttv2.pt",
                        title = "Liga Portugal Betclic: Vitória SC x SL Benfica (Direto)",
                        description = "Transmissão da 11ª jornada",
                        category = "Desporto",
                        startEpochMilli = kickoff.toEpochMilli(),
                        stopEpochMilli = kickoff.plusSeconds(7200).toEpochMilli()
                    )
                )
            )
        )

        val results = SportsBindingResolver.resolveAll(
            fixtures = listOf(fixture),
            channels = listOf(sportTvPlus, sportTv1, sportTv2),
            epgGuide = epgGuide
        )

        assertEquals(1, results.size)
        assertEquals("PT: SPORT TV 2 HD", results[0].second.name)
    }
}

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
}

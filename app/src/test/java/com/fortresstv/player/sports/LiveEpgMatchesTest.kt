package com.fortresstv.player.sports

import com.fortresstv.player.epg.EpgChannel
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.epg.EpgProgramme
import com.fortresstv.player.playlist.PlaylistChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LiveEpgMatchesTest {
    @Test
    fun requiresAnUpcomingFixtureToDistinguishMatchesFromUnlabelledReplays() {
        val now = Instant.parse("2026-11-13T20:00:00Z")
        val tomorrow = now.plusSeconds(5 * 3600)
        val channel = PlaylistChannel(0, "PT: Sport TV 3", "", tvgId = "sporttv3.pt")
        fun programme(title: String, start: Instant) =
            EpgProgramme("sporttv3.pt", title, null, "Futebol", start.toEpochMilli(), null)
        val guide = EpgGuide(
            channels = mapOf("sporttv3.pt" to EpgChannel("sporttv3.pt", "Sport TV 3")),
            programmes = mapOf("sporttv3.pt" to listOf(
                programme("Liga das Nações: Países Baixos - Grécia", tomorrow),
                programme("Resumo: Sérvia - Alemanha", tomorrow),
                programme("Tunísia - Líbia", now.plusSeconds(86_400))
            ))
        )
        assertEquals(emptyList<SportsEpgMatch>(), liveEpgMatches(
            emptyList(), guide, listOf(channel), emptyMap(), now, upcomingUntil = now.plusSeconds(86_400)
        ))
        val scheduled = fixture(tomorrow, FixtureStatus.SCHEDULED).copy(homeTeam = "Netherlands", awayTeam = "Greece")
        val (match, resolvedChannel, live) = liveEpgMatches(
            listOf(scheduled), guide, listOf(channel), emptyMap(), now, upcomingUntil = now.plusSeconds(86_400)
        ).single()
        assertFalse(live)
        assertEquals(scheduled, match)
        assertEquals(tomorrow, match.kickoffUtc)
        assertEquals(FixtureStatus.SCHEDULED, match.status)
        assertEquals(channel, resolvedChannel)
    }

    private fun fixture(now: Instant, status: FixtureStatus = FixtureStatus.LIVE) = SportsFixture(
        "espn-1", SportsKind.FOOTBALL, "Nations League", null,
        "Italy", "France", null, null, now, "Sport TV 3", status, homeScore = 1, awayScore = 0
    )
    @Test
    fun usesThePortugalGuideFeedWhenForeignPlaylistChannelsShareItsName() {
        val now = Instant.parse("2026-11-12T18:00:00Z")
        val channels = listOf(
            PlaylistChannel(0, "EXYU: Sport TV3", "", tvgId = "sporttv3.pt"),
            PlaylistChannel(1, "BR: DAZN 3", "", tvgId = "dazn3.pt"),
            PlaylistChannel(2, "PT: Sport TV3", "", tvgId = "sporttv3.pt"),
            PlaylistChannel(3, "PT: Sport TV 4", "", tvgId = "sporttv3.pt")
        )
        val guide = EpgGuide(
            channels = mapOf(
                "sporttv3.pt" to EpgChannel("sporttv3.pt", "Sport TV 3"),
                "dazn3.pt" to EpgChannel("dazn3.pt", "DAZN 3")
            ),
            programmes = listOf("sporttv3.pt", "dazn3.pt").associateWith {
                listOf(EpgProgramme(it, "Itália - França", null, "Football", now.toEpochMilli(), null))
            }
        )

        assertEquals(channels[2], liveEpgMatches(listOf(fixture(now)), guide, channels, emptyMap(), now).single().channel)
    }

    @Test
    fun requiresACurrentCorrectedEpgProgrammeButNotALiveApiStatus() {
        val now = Instant.parse("2026-11-12T18:00:00Z")
        val channel = PlaylistChannel(0, "Sport 1", "", tvgId = "sport")
        fun programme(title: String, minutesAway: Long) =
            EpgProgramme("sport", title, null, "Football", now.plusSeconds(minutesAway * 60).toEpochMilli(), null)

        val guide = EpgGuide(
            channels = mapOf("sport" to EpgChannel("sport", "Sport 1")),
            programmes = mapOf("sport" to listOf(
                programme("Italy vs France", 30),
                programme("Replay: Italy vs France", -30),
                programme("UEFA Nations League: Italy vs France", -40),
                programme("Türkiye vs Belgium", -60)
            ))
        )

        val (match, resolvedChannel, live) = liveEpgMatches(
            listOf(fixture(now)), guide, listOf(channel), emptyMap(), now, correctionMinutes = 30
        ).single()
        assertTrue(live)
        assertTrue(liveEpgMatches(
            listOf(fixture(now, FixtureStatus.SCHEDULED)), guide, listOf(channel), emptyMap(), now, 30
        ).single().live)
        for (status in listOf(FixtureStatus.FINISHED, FixtureStatus.POSTPONED)) {
            assertEquals(emptyList<SportsEpgMatch>(), liveEpgMatches(
                listOf(fixture(now, status)), guide, listOf(channel), emptyMap(), now, 30
            ))
        }
        assertEquals("Italy", match.homeTeam)
        assertEquals("France", match.awayTeam)
        assertEquals(1, match.homeScore)
        assertEquals(channel, resolvedChannel)
    }

    private val sportTv = PlaylistChannel(0, "PT: Sport TV 1", "", tvgId = "sporttv1.pt")

    private fun sportTvGuide(vararg programmes: EpgProgramme) = EpgGuide(
        channels = mapOf("sporttv1.pt" to EpgChannel("sporttv1.pt", "Sport TV 1")),
        programmes = mapOf("sporttv1.pt" to programmes.toList())
    )

    private fun broadcast(title: String, start: Instant, minutes: Long = 135) = EpgProgramme(
        "sporttv1.pt", title, null, "Futebol", start.toEpochMilli(), start.plusSeconds(minutes * 60).toEpochMilli()
    )

    private fun derby(kickoff: Instant, status: FixtureStatus = FixtureStatus.SCHEDULED, id: String = "derby") =
        SportsFixture(
            id, SportsKind.FOOTBALL, "Primeira Liga", null,
            "Benfica", "Porto", null, null, kickoff, "Sport TV 1", status
        )

    private fun homeRow(fixtures: List<SportsFixture>, guide: EpgGuide, now: Instant, correctionMinutes: Int = 0) =
        liveEpgMatches(
            fixtures, guide, listOf(sportTv), emptyMap(), now, correctionMinutes, upcomingUntil = now.plusSeconds(86_400)
        )

    @Test
    fun staysInTheRowWhenTheProgrammeStartsBeforeKickoff() {
        val kickoff = Instant.parse("2026-11-14T20:15:00Z")
        val programmeStart = kickoff.minusSeconds(15 * 60)
        val guide = sportTvGuide(broadcast("Liga Portugal: Benfica - Porto", programmeStart))
        val fixtures = listOf(derby(kickoff))

        val before = homeRow(fixtures, guide, programmeStart.minusSeconds(60)).single()
        assertFalse(before.live)
        assertEquals(sportTv, before.channel)

        // The fixture is still SCHEDULED and kickoff is 15 minutes away, but the broadcast is on air.
        for (now in listOf(programmeStart, programmeStart.plusSeconds(5 * 60))) {
            val match = homeRow(fixtures, guide, now).single()
            assertTrue(match.live)
            assertEquals(sportTv, match.channel)
        }
    }

    @Test
    fun showsAMatchAtHalfTimeWithItsMinuteAndScore() {
        val kickoff = Instant.parse("2026-11-14T20:15:00Z")
        val guide = sportTvGuide(broadcast("Liga Portugal: Benfica - Porto", kickoff.minusSeconds(15 * 60)))
        val halfTime = derby(kickoff, FixtureStatus.LIVE).copy(minute = "HT", homeScore = 1, awayScore = 1)

        val match = homeRow(listOf(halfTime), guide, kickoff.plusSeconds(50 * 60)).single()
        assertTrue(match.live)
        assertEquals("HT", match.fixture.minute)
        assertEquals(1, match.fixture.homeScore)
        assertEquals(1, match.fixture.awayScore)
    }

    @Test
    fun treatsAnAiringProgrammeAsLiveWhenTheFixtureStatusIsStale() {
        val kickoff = Instant.parse("2026-11-14T20:15:00Z")
        val guide = sportTvGuide(broadcast("Liga Portugal: Benfica - Porto", kickoff.minusSeconds(15 * 60)))
        val now = kickoff.plusSeconds(70 * 60)

        val match = homeRow(listOf(derby(kickoff, FixtureStatus.SCHEDULED)), guide, now).single()
        assertTrue(match.live)
        assertEquals(FixtureStatus.SCHEDULED, match.fixture.status)

        // Once the broadcast is over the stale fixture leaves the row.
        assertEquals(emptyList<SportsEpgMatch>(), homeRow(listOf(derby(kickoff)), guide, kickoff.plusSeconds(3 * 3600)))
    }

    @Test
    fun appliesTheScheduleCorrectionToTheLiveFlag() {
        val kickoff = Instant.parse("2026-11-14T20:15:00Z")
        // The guide runs 30 minutes early; the correction moves the broadcast to 20:00–22:15.
        val guide = sportTvGuide(broadcast("Liga Portugal: Benfica - Porto", kickoff.minusSeconds(45 * 60)))
        val fixtures = listOf(derby(kickoff))

        assertFalse(homeRow(fixtures, guide, kickoff.minusSeconds(20 * 60), correctionMinutes = 30).single().live)
        assertTrue(homeRow(fixtures, guide, kickoff.minusSeconds(10 * 60), correctionMinutes = 30).single().live)
    }

    @Test
    fun keepsAMatchThatStartedBeforeMidnightFromThePreviousDayPartition() {
        val zone = java.time.ZoneId.of("Europe/Lisbon")
        val kickoff = Instant.parse("2026-11-14T22:45:00Z")
        val now = Instant.parse("2026-11-15T00:20:00Z")
        val until = now.plusSeconds(86_400)
        val yesterday = kickoff.atZone(zone).toLocalDate().toEpochDay()

        assertEquals(yesterday..yesterday + 2, epgDaysForMatches(now, until, 0, zone))
        // A positive correction can push `now` back over midnight; the day before is still included.
        assertEquals(yesterday - 1..yesterday + 1, epgDaysForMatches(now, until, 30, zone))

        val nextKickoff = Instant.parse("2026-11-15T18:00:00Z")
        val next = derby(nextKickoff, id = "next").copy(homeTeam = "Sporting", awayTeam = "Braga")
        val previousDay = sportTvGuide(broadcast("Liga Portugal: Benfica - Porto", kickoff.minusSeconds(15 * 60)))
            .copy(epochDay = yesterday)
        val today = sportTvGuide(broadcast("Liga Portugal: Sporting - Braga", nextKickoff))
            .copy(epochDay = yesterday + 1)
        val fixtures = listOf(next, derby(kickoff, FixtureStatus.LIVE))

        // Today's partition alone has lost the match that is still being played.
        assertEquals(listOf("next"), homeRow(fixtures, today, now).map { it.fixture.id })

        val row = liveEpgMatches(
            fixtures, listOf(previousDay, today, today), listOf(sportTv), emptyMap(), now, upcomingUntil = until
        )
        // Airing first, then by kickoff, and one card per fixture.
        assertEquals(listOf("derby", "next"), row.map { it.fixture.id })
        assertEquals(listOf(true, false), row.map { it.live })
    }

    @Test
    fun sortsAiringMatchesBeforeEarlierUpcomingKickoffs() {
        val now = Instant.parse("2026-11-14T20:30:00Z")
        val liveKickoff = now.minusSeconds(15 * 60)
        val soon = now.plusSeconds(3600)
        val later = now.plusSeconds(4 * 3600)
        val guide = sportTvGuide(
            broadcast("Sporting - Braga", later),
            broadcast("Benfica - Porto", liveKickoff),
            EpgProgramme("sporttv1.pt", "Rio Ave - Famalicão", null, "Futebol", soon.toEpochMilli(), null)
        )
        val fixtures = listOf(
            derby(later, id = "later").copy(homeTeam = "Sporting", awayTeam = "Braga"),
            derby(soon, id = "soon").copy(homeTeam = "Rio Ave", awayTeam = "Famalicão"),
            derby(liveKickoff, id = "live")
        )

        val row = homeRow(fixtures, guide, now)
        assertEquals(listOf("live", "soon", "later"), row.map { it.fixture.id })
        assertEquals(listOf(true, false, false), row.map { it.live })
    }

    @Test
    fun hidesAReplayOfYesterdaysMatch() {
        val now = Instant.parse("2026-11-15T15:00:00Z")
        val kickoff = Instant.parse("2026-11-14T20:15:00Z")
        val guide = sportTvGuide(
            // Airing now and later today, with and without a replay label.
            broadcast("Benfica - Porto", now.minusSeconds(30 * 60)),
            broadcast("Repetição: Benfica - Porto", now.minusSeconds(30 * 60)),
            broadcast("Liga Portugal: Benfica - Porto", now.plusSeconds(3 * 3600))
        )

        // Stale statuses included: the sync may never have marked yesterday's match as finished.
        for (status in FixtureStatus.values()) {
            assertEquals(emptyList<SportsEpgMatch>(), homeRow(listOf(derby(kickoff, status)), guide, now))
        }
    }

    @Test
    fun hidesMatchesMoreThanADayAhead() {
        val now = Instant.parse("2026-11-14T12:00:00Z")
        val inRange = now.plusSeconds(23 * 3600)
        val tooFar = now.plusSeconds(25 * 3600)
        val guide = sportTvGuide(
            broadcast("Benfica - Porto", tooFar.minusSeconds(15 * 60)),
            broadcast("Sporting - Braga", inRange)
        )
        val fixtures = listOf(
            derby(tooFar),
            derby(inRange, id = "in-range").copy(homeTeam = "Sporting", awayTeam = "Braga")
        )

        assertEquals(listOf("in-range"), homeRow(fixtures, guide, now).map { it.fixture.id })
    }

    @Test
    fun keepsUpcomingMatchesFootballOnlyAndOnPortugalFeeds() {
        val now = Instant.parse("2026-11-14T12:00:00Z")
        val tipOff = now.plusSeconds(2 * 3600)
        val guide = sportTvGuide(broadcast("NBA: Lakers - Celtics", tipOff), broadcast("Benfica - Porto", tipOff))
        val nba = derby(tipOff, id = "nba").copy(sport = SportsKind.BASKETBALL, homeTeam = "Lakers", awayTeam = "Celtics")
        assertEquals(emptyList<SportsEpgMatch>(), homeRow(listOf(nba), guide, now))

        val foreign = EpgGuide(
            channels = mapOf("sky" to EpgChannel("sky", "Sky Sports 1")),
            programmes = mapOf("sky" to listOf(
                EpgProgramme("sky", "Benfica - Porto", null, "Football", tipOff.toEpochMilli(), null)
            ))
        )
        assertEquals(emptyList<SportsEpgMatch>(), liveEpgMatches(
            listOf(derby(tipOff)), foreign, listOf(PlaylistChannel(0, "Sky Sports 1", "", tvgId = "sky")),
            emptyMap(), now, upcomingUntil = now.plusSeconds(86_400)
        ))
    }
}

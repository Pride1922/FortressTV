package com.fortresstv.player.sports

import com.fortresstv.player.epg.EpgChannel
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.epg.EpgProgramme
import com.fortresstv.player.playlist.PlaylistChannel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class LiveEpgMatchesTest {
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

        assertEquals(channels[2], liveEpgMatches(listOf(fixture(now)), guide, channels, emptyMap(), now).single().second)
    }

    @Test
    fun requiresLiveApiStatusAndACurrentCorrectedEpgProgramme() {
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

        val (match, resolvedChannel) = liveEpgMatches(
            listOf(fixture(now)), guide, listOf(channel), emptyMap(), now, correctionMinutes = 30
        ).single()
        assertEquals(emptyList<Pair<SportsFixture, PlaylistChannel>>(), liveEpgMatches(
            listOf(fixture(now, FixtureStatus.SCHEDULED)), guide, listOf(channel), emptyMap(), now, 30
        ))
        assertEquals("Italy", match.homeTeam)
        assertEquals("France", match.awayTeam)
        assertEquals(1, match.homeScore)
        assertEquals(channel, resolvedChannel)
    }
}

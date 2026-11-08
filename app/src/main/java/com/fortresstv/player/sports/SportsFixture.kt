package com.fortresstv.player.sports

import org.json.JSONObject
import java.time.Instant

enum class SportsKind {
    FOOTBALL,
    MOTORSPORT,
    BASKETBALL,
    TENNIS,
    OTHER
}

enum class FixtureStatus {
    SCHEDULED,
    LIVE,
    FINISHED,
    POSTPONED
}

data class SportsFixture(
    val id: String,
    val sport: SportsKind,
    val competition: String,
    val competitionLogoUrl: String?,
    val homeTeam: String,
    val awayTeam: String,
    val homeTeamLogoUrl: String?,
    val awayTeamLogoUrl: String?,
    val kickoffUtc: Instant,
    val broadcasterOfficial: String,
    val status: FixtureStatus,
    val minute: String? = null,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val venue: String? = null
) {
    fun isLive(): Boolean = status == FixtureStatus.LIVE

    companion object {
        fun fromJson(json: JSONObject): SportsFixture {
            val sportStr = json.optString("sport", "football").lowercase()
            val sport = when (sportStr) {
                "football", "soccer" -> SportsKind.FOOTBALL
                "motorsport", "f1", "motogp" -> SportsKind.MOTORSPORT
                "basketball" -> SportsKind.BASKETBALL
                "tennis" -> SportsKind.TENNIS
                else -> SportsKind.OTHER
            }
            val statusStr = json.optString("status", "scheduled").lowercase()
            val status = when (statusStr) {
                "live" -> FixtureStatus.LIVE
                "finished" -> FixtureStatus.FINISHED
                "postponed" -> FixtureStatus.POSTPONED
                else -> FixtureStatus.SCHEDULED
            }
            val kickoffStr = json.optString("kickoff_utc", "")
            val kickoff = runCatching { Instant.parse(kickoffStr) }.getOrDefault(Instant.now())

            return SportsFixture(
                id = json.getString("id"),
                sport = sport,
                competition = json.optString("competition", ""),
                competitionLogoUrl = json.optString("competition_logo_url").takeIf { it.isNotBlank() },
                homeTeam = json.optString("home_team", ""),
                awayTeam = json.optString("away_team", ""),
                homeTeamLogoUrl = json.optString("home_team_logo_url").takeIf { it.isNotBlank() },
                awayTeamLogoUrl = json.optString("away_team_logo_url").takeIf { it.isNotBlank() },
                kickoffUtc = kickoff,
                broadcasterOfficial = json.optString("broadcaster_official", ""),
                status = status,
                minute = json.optString("minute").takeIf { it.isNotBlank() },
                homeScore = if (json.has("home_score") && !json.isNull("home_score")) json.getInt("home_score") else null,
                awayScore = if (json.has("away_score") && !json.isNull("away_score")) json.getInt("away_score") else null,
                venue = json.optString("venue").takeIf { it.isNotBlank() }
            )
        }
    }
}

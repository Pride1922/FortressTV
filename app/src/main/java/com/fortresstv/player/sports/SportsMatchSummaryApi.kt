package com.fortresstv.player.sports

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class MatchEventType {
    GOAL,
    PENALTY,
    OWN_GOAL,
    RED_CARD,
    YELLOW_CARD
}

data class MatchTimelineEvent(
    val minute: String,
    val type: MatchEventType,
    val teamName: String,
    val playerName: String,
    val description: String
)

data class MatchSummaryDetails(
    val events: List<MatchTimelineEvent> = emptyList(),
    val venue: String? = null,
    val leaders: List<String> = emptyList()
)

object SportsMatchSummaryApi {
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    suspend fun fetchSummary(fixture: SportsFixture): MatchSummaryDetails = withContext(Dispatchers.IO) {
        runCatching {
            val eventId = extractEventId(fixture.id)
            if (fixture.id.startsWith("epg-") || eventId.isBlank() || fixture.sport == SportsKind.RUGBY || fixture.id.contains("rugby")) {
                return@withContext MatchSummaryDetails()
            }

            val urlString = when {
                fixture.sport == SportsKind.BASKETBALL || fixture.id.contains("nba") ->
                    "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/summary?event=$eventId"
                fixture.sport == SportsKind.MOTORSPORT || fixture.id.contains("f1") ->
                    "https://site.api.espn.com/apis/site/v2/sports/racing/f1/summary?event=$eventId"
                else ->
                    "https://site.api.espn.com/apis/site/v2/sports/soccer/all/summary?event=$eventId"
            }

            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode != 200) {
                return@withContext MatchSummaryDetails()
            }

            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(responseText)

            // 1. Extract Venue
            val venue = root.optJSONObject("gameInfo")?.optJSONObject("venue")?.optString("fullName")
                ?: fixture.venue

            // 2. Soccer: Key Events (Goals, Penalties, Red Cards)
            val eventsList = mutableListOf<MatchTimelineEvent>()
            val keyEvents = root.optJSONArray("keyEvents")
            if (keyEvents != null) {
                for (i in 0 until keyEvents.length()) {
                    val ev = keyEvents.optJSONObject(i) ?: continue
                    val typeObj = ev.optJSONObject("type")
                    val typeStr = typeObj?.optString("type", "")?.lowercase() ?: ""
                    val scoringPlay = ev.optBoolean("scoringPlay", false)
                    val isGoal = scoringPlay || typeStr.contains("goal")
                    val isRed = typeStr.contains("red-card")
                    if (!isGoal && !isRed) continue

                    val clock = ev.optJSONObject("clock")?.optString("displayValue", "") ?: ""
                    val teamName = ev.optJSONObject("team")?.optString("displayName", "") ?: ""
                    val participants = ev.optJSONArray("participants")
                    val athleteName = participants?.optJSONObject(0)?.optJSONObject("athlete")?.optString("displayName", "") ?: ""

                    val eventType = when {
                        typeStr.contains("penalty") -> MatchEventType.PENALTY
                        typeStr.contains("own-goal") -> MatchEventType.OWN_GOAL
                        isRed -> MatchEventType.RED_CARD
                        else -> MatchEventType.GOAL
                    }
                    val text = ev.optString("text", "")
                    eventsList.add(
                        MatchTimelineEvent(
                            minute = clock.ifBlank { "•" },
                            type = eventType,
                            teamName = teamName,
                            playerName = athleteName.ifBlank { text },
                            description = text
                        )
                    )
                }
            }

            // 3. NBA: Game Leaders
            val leaderList = mutableListOf<String>()
            val leaders = root.optJSONArray("leaders")
            if (leaders != null) {
                for (i in 0 until leaders.length()) {
                    val teamLeaders = leaders.optJSONObject(i) ?: continue
                    val teamObj = teamLeaders.optJSONObject("team")
                    val teamAbbr = teamObj?.optString("abbreviation", "")?.ifBlank {
                        teamObj.optString("displayName", "")
                    } ?: ""
                    val leadersArr = teamLeaders.optJSONArray("leaders") ?: continue
                    for (j in 0 until leadersArr.length()) {
                        val cat = leadersArr.optJSONObject(j) ?: continue
                        val catName = cat.optString("displayName", "")
                        val topLeader = cat.optJSONArray("leaders")?.optJSONObject(0)
                        val athleteName = topLeader?.optJSONObject("athlete")?.optString("displayName", "") ?: ""
                        val valStr = topLeader?.optString("displayValue", "") ?: ""
                        if (athleteName.isNotBlank() && valStr.isNotBlank()) {
                            leaderList.add("$teamAbbr: $athleteName ($valStr $catName)")
                        }
                    }
                }
            }

            MatchSummaryDetails(
                events = eventsList,
                venue = venue,
                leaders = leaderList
            )
        }.getOrDefault(MatchSummaryDetails(venue = fixture.venue))
    }

    private fun extractEventId(rawId: String): String {
        return rawId
            .removePrefix("espn-f1-")
            .removePrefix("espn-nba-")
            .removePrefix("espn-")
            .removePrefix("sdb-")
            .trim()
    }
}

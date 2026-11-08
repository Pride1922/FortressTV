package com.fortresstv.player.sports

import android.content.Context
import android.content.SharedPreferences
import com.fortresstv.player.library.SupabaseSyncApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.time.Instant

class SportsRepository internal constructor(
    private val prefsProvider: () -> SharedPreferences
) {
    companion object {
        private const val PREFS_NAME = "fortress_sports_fixtures"
        private const val KEY_FIXTURES = "fixtures_json"

        @Volatile
        private var instance: SportsRepository? = null

        fun get(context: Context): SportsRepository = instance ?: synchronized(this) {
            instance ?: SportsRepository(
                prefsProvider = {
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            ).also { instance = it }
        }
    }

    private val preferences by lazy { prefsProvider() }
    private val _fixtures = MutableStateFlow<List<SportsFixture>>(emptyList())
    val fixtures: StateFlow<List<SportsFixture>> = _fixtures.asStateFlow()

    init {
        loadPersisted()
        if (_fixtures.value.isEmpty()) {
            _fixtures.value = createDefaultSeedFixtures()
        }
    }

    fun setFixtures(list: List<SportsFixture>) {
        if (list.isEmpty()) return
        _fixtures.value = list
        persistAsync(list)
    }

    internal fun refreshFromNetwork(api: SupabaseSyncApi, accessToken: String? = null) {
        val fetched = api.fetchSportsFixtures(accessToken)
        if (fetched.isNotEmpty()) {
            setFixtures(fetched)
        }
    }

    private fun loadPersisted() {
        val raw = preferences.getString(KEY_FIXTURES, null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<SportsFixture>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(SportsFixture.fromJson(obj))
            }
            if (list.isNotEmpty()) {
                _fixtures.value = list
            }
        }
    }

    private fun persistAsync(list: List<SportsFixture>) {
        runCatching {
            val arr = JSONArray()
            for (f in list) {
                val obj = org.json.JSONObject().apply {
                    put("id", f.id)
                    put("sport", f.sport.name.lowercase())
                    put("competition", f.competition)
                    put("competition_logo_url", f.competitionLogoUrl)
                    put("home_team", f.homeTeam)
                    put("away_team", f.awayTeam)
                    put("home_team_logo_url", f.homeTeamLogoUrl)
                    put("away_team_logo_url", f.awayTeamLogoUrl)
                    put("kickoff_utc", f.kickoffUtc.toString())
                    put("broadcaster_official", f.broadcasterOfficial)
                    put("status", f.status.name.lowercase())
                    put("minute", f.minute)
                    if (f.homeScore != null) put("home_score", f.homeScore)
                    if (f.awayScore != null) put("away_score", f.awayScore)
                    put("venue", f.venue)
                }
                arr.put(obj)
            }
            preferences.edit().putString(KEY_FIXTURES, arr.toString()).apply()
        }
    }

    private fun createDefaultSeedFixtures(): List<SportsFixture> {
        val now = Instant.now()
        return listOf(
            SportsFixture(
                id = "pl-benfica-porto",
                sport = SportsKind.FOOTBALL,
                competition = "Primeira Liga",
                competitionLogoUrl = "https://upload.wikimedia.org/wikipedia/pt/e/ee/Liga_Portugal_Betclic_logo.png",
                homeTeam = "SL Benfica",
                awayTeam = "FC Porto",
                homeTeamLogoUrl = "https://upload.wikimedia.org/wikipedia/pt/2/2a/Sport_Lisboa_e_Benfica_logo.png",
                awayTeamLogoUrl = "https://upload.wikimedia.org/wikipedia/pt/1/1b/FC_Porto.png",
                kickoffUtc = now.plusSeconds(7200),
                broadcasterOfficial = "Sport TV 1",
                status = FixtureStatus.SCHEDULED,
                venue = "Estádio da Luz, Lisboa"
            ),
            SportsFixture(
                id = "pl-sporting-braga",
                sport = SportsKind.FOOTBALL,
                competition = "Primeira Liga",
                competitionLogoUrl = "https://upload.wikimedia.org/wikipedia/pt/e/ee/Liga_Portugal_Betclic_logo.png",
                homeTeam = "Sporting CP",
                awayTeam = "SC Braga",
                homeTeamLogoUrl = "https://upload.wikimedia.org/wikipedia/pt/1/13/Sporting_Clube_de_Portugal_%28logo%29.png",
                awayTeamLogoUrl = "https://upload.wikimedia.org/wikipedia/pt/7/79/Sporting_Clube_de_Braga_logo.png",
                kickoffUtc = now.plusSeconds(18000),
                broadcasterOfficial = "Sport TV 1",
                status = FixtureStatus.SCHEDULED,
                venue = "Estádio José Alvalade, Lisboa"
            ),
            SportsFixture(
                id = "ucl-arsenal-realmadrid",
                sport = SportsKind.FOOTBALL,
                competition = "UEFA Champions League",
                competitionLogoUrl = "https://upload.wikimedia.org/wikipedia/en/b/bf/UEFA_Champions_League_logo_2.svg",
                homeTeam = "Arsenal",
                awayTeam = "Real Madrid",
                homeTeamLogoUrl = "https://upload.wikimedia.org/wikipedia/en/5/53/Arsenal_FC.svg",
                awayTeamLogoUrl = "https://upload.wikimedia.org/wikipedia/en/5/56/Real_Madrid_CF.svg",
                kickoffUtc = now.plusSeconds(86400 + 25200),
                broadcasterOfficial = "DAZN 1",
                status = FixtureStatus.SCHEDULED,
                venue = "Emirates Stadium, Londres"
            ),
            SportsFixture(
                id = "f1-interlagos",
                sport = SportsKind.MOTORSPORT,
                competition = "Formula 1",
                competitionLogoUrl = "https://upload.wikimedia.org/wikipedia/commons/3/33/F1.svg",
                homeTeam = "GP São Paulo (Interlagos)",
                awayTeam = "Corrida Principal",
                homeTeamLogoUrl = "https://upload.wikimedia.org/wikipedia/commons/3/33/F1.svg",
                awayTeamLogoUrl = "https://upload.wikimedia.org/wikipedia/commons/3/33/F1.svg",
                kickoffUtc = now.plusSeconds(10800),
                broadcasterOfficial = "Sport TV 4",
                status = FixtureStatus.SCHEDULED,
                venue = "Autódromo José Carlos Pace"
            )
        )
    }
}

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
    }

    fun setFixtures(list: List<SportsFixture>) {
        val realFixtures = list.filterNot { isFakeOrSeedId(it.id) }
        if (_fixtures.value == realFixtures) return
        android.util.Log.d("FortressTV", "[SportsRepository] Setting ${realFixtures.size} real fixtures (received: ${list.size})")
        _fixtures.value = realFixtures
        persistAsync(realFixtures)
    }

    suspend fun refresh(accessToken: String? = null) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val api = SupabaseSyncApi()
            val fetched = api.fetchSportsFixtures(accessToken)
            android.util.Log.d("FortressTV", "[SportsRepository] Fetched ${fetched.size} fixtures from Supabase")
            if (fetched.isNotEmpty()) {
                setFixtures(fetched)
            }
        }.onFailure { err ->
            android.util.Log.e("FortressTV", "[SportsRepository] Failed to fetch fixtures from Supabase", err)
        }
    }

    internal fun refreshFromNetwork(api: SupabaseSyncApi, accessToken: String? = null) {
        val fetched = api.fetchSportsFixtures(accessToken)
        if (fetched.isNotEmpty()) {
            setFixtures(fetched)
        }
    }

    fun clearCache() {
        _fixtures.value = emptyList()
        preferences.edit().remove(KEY_FIXTURES).apply()
    }

    private fun loadPersisted() {
        val raw = preferences.getString(KEY_FIXTURES, null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<SportsFixture>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "")
                if (isFakeOrSeedId(id)) continue
                list.add(SportsFixture.fromJson(obj))
            }
            _fixtures.value = list
        }
    }

    private fun isFakeOrSeedId(id: String): Boolean {
        val lower = id.lowercase()
        return lower.startsWith("fixture-") || lower.startsWith("pl-") || lower.startsWith("ucl-") ||
            lower.startsWith("mock-") || lower.startsWith("fake-") || lower.startsWith("seed-") ||
            lower.startsWith("sdb-")
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
}

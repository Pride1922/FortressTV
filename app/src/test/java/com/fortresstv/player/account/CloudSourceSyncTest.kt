package com.fortresstv.player.account

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class CloudSourceSyncTest {

    private class FakeSourceTransport : com.fortresstv.player.profile.ProfileTransport {
        val savedSources = mutableListOf<HouseholdSourceRecord>()
        var getSourcesResponse: String = "[]"
        var throwOnSave: Boolean = false
        var throwOnGet: Boolean = false

        override fun request(
            method: String,
            path: String,
            body: JSONObject?,
            accessToken: String?,
            headers: Map<String, String>?
        ): String {
            if (path.contains("fortress_save_household_source")) {
                if (throwOnSave) throw IOException("Failed to save source connection")
                val record = HouseholdSourceRecord(
                    id = body!!.getString("p_source_id"),
                    sourceType = body.getString("p_source_type"),
                    displayName = body.getString("p_display_name"),
                    enabled = body.optBoolean("p_enabled", true),
                    sourceOrder = body.optInt("p_source_order", 0),
                    config = body.getJSONObject("p_config")
                )
                savedSources.add(record)
                return "\"${record.id}\""
            }
            if (path.contains("fortress_get_household_sources")) {
                if (throwOnGet) throw IOException("Network timeout or auth failure")
                return getSourcesResponse
            }
            return "{}"
        }
    }

    @Test
    fun saveHouseholdSourceEnforcesStableSourceUuid() {
        val transport = FakeSourceTransport()
        val api = SupabaseSourceApi(transport)

        val stableId = "11111111-2222-3333-4444-555555555555"
        val config = JSONObject().apply {
            put("server_url", "http://xtream.test")
            put("username", "testuser")
            put("password", "testpass")
        }
        val record = HouseholdSourceRecord(
            id = stableId,
            sourceType = "xtream",
            displayName = "Test Xtream",
            enabled = true,
            sourceOrder = 1,
            config = config
        )

        val resultId = api.saveHouseholdSource(record, "unlock-token", "access-token")
        assertEquals(stableId, resultId)
        assertEquals(1, transport.savedSources.size)
        assertEquals(stableId, transport.savedSources[0].id)
        assertEquals("testuser", transport.savedSources[0].config.getString("username"))
    }

    @Test
    fun saveHouseholdSourceFailureThrowsAndIsNotSwallowed() {
        val transport = FakeSourceTransport().apply { throwOnSave = true }
        val api = SupabaseSourceApi(transport)

        val record = HouseholdSourceRecord(
            id = "11111111-2222-3333-4444-555555555555",
            sourceType = "xtream",
            displayName = "Test Xtream",
            enabled = true,
            sourceOrder = 1,
            config = JSONObject()
        )

        try {
            api.saveHouseholdSource(record, "unlock-token", "access-token")
            fail("Expected exception when save fails")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("Failed to save source connection"))
        }
    }

    @Test
    fun parseHouseholdSourcesPreservesProperties() {
        val transport = FakeSourceTransport().apply {
            getSourcesResponse = """
                [
                    {
                        "id": "22222222-3333-4444-5555-666666666666",
                        "source_type": "xtream",
                        "display_name": "Cloud Restored Provider",
                        "enabled": true,
                        "source_order": 0,
                        "config": {
                            "server_url": "http://provider.test",
                            "username": "user1",
                            "password": "pass1"
                        }
                    }
                ]
            """.trimIndent()
        }
        val api = SupabaseSourceApi(transport)
        val sources = api.getHouseholdSources("token", "jwt")

        assertEquals(1, sources.size)
        val restored = sources[0]
        assertEquals("22222222-3333-4444-5555-666666666666", restored.id)
        assertEquals("Cloud Restored Provider", restored.displayName)
        assertEquals("http://provider.test", restored.config.getString("server_url"))
        assertEquals("user1", restored.config.getString("username"))
    }

    @Test
    fun getHouseholdSourcesFailureThrowsAndDoesNotReturnEmptyList() {
        val transport = FakeSourceTransport().apply { throwOnGet = true }
        val api = SupabaseSourceApi(transport)

        try {
            api.getHouseholdSources("token", "jwt")
            fail("Expected network failure to throw")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("Network timeout or auth failure"))
        }
    }

    @Test
    fun saveAndGetOpenSubtitlesCredentialsPreservesProperties() {
        var savedBody: JSONObject? = null
        val transport = object : com.fortresstv.player.profile.ProfileTransport {
            override fun request(
                method: String,
                path: String,
                body: JSONObject?,
                accessToken: String?,
                headers: Map<String, String>?
            ): String {
                if (path.contains("fortress_save_opensubtitles")) {
                    savedBody = body
                    return "{\"success\":true}"
                }
                if (path.contains("fortress_get_opensubtitles")) {
                    return """
                        {
                            "configured": true,
                            "enabled": true,
                            "prefer_pt_pt": true,
                            "auto_search": false,
                            "api_key": "my-api-key",
                            "username": "my-user",
                            "password": "my-password"
                        }
                    """.trimIndent()
                }
                return "{}"
            }
        }
        val api = SupabaseSourceApi(transport)
        val testSettings = com.fortresstv.player.subtitles.OpenSubtitlesSettings(
            enabled = true,
            apiKey = "my-api-key",
            username = "my-user",
            password = "my-password",
            preferPtPt = true,
            autoSearch = false
        )
        val saveResult = api.saveOpenSubtitles(testSettings, "unlock-tok", "access-tok")
        assertTrue(saveResult)
        assertNotNull(savedBody)
        assertEquals("my-api-key", savedBody!!.getString("p_api_key"))
        assertEquals("my-user", savedBody!!.getString("p_username"))
        assertEquals("my-password", savedBody!!.getString("p_password"))
        assertTrue(savedBody!!.getBoolean("p_prefer_pt_pt"))
        assertFalse(savedBody!!.getBoolean("p_auto_search"))

        val restored = api.getOpenSubtitles(null, "access-tok")
        assertNotNull(restored)
        assertEquals("my-api-key", restored!!.apiKey)
        assertEquals("my-user", restored.username)
        assertEquals("my-password", restored.password)
        assertTrue(restored.preferPtPt)
        assertFalse(restored.autoSearch)
    }

    @Test
    fun saveAndGetTmdbSettingsPreservesProperties() {
        var savedBody: JSONObject? = null
        val transport = object : com.fortresstv.player.profile.ProfileTransport {
            override fun request(
                method: String,
                path: String,
                body: JSONObject?,
                accessToken: String?,
                headers: Map<String, String>?
            ): String {
                if (path.contains("fortress_save_tmdb")) {
                    savedBody = body
                    return "{\"success\":true}"
                }
                if (path.contains("fortress_get_tmdb")) {
                    return """
                        {
                            "configured": true,
                            "enabled": true,
                            "language": "pt-PT",
                            "api_key": "tmdb-test-key"
                        }
                    """.trimIndent()
                }
                return "{}"
            }
        }
        val api = SupabaseSourceApi(transport)
        val testSettings = com.fortresstv.player.tmdb.TmdbSettings(
            enabled = true,
            apiKey = "tmdb-test-key",
            language = "pt-PT"
        )
        val saveResult = api.saveTmdb(testSettings, "unlock-tok", "access-tok")
        assertTrue(saveResult)
        assertNotNull(savedBody)
        assertEquals("tmdb-test-key", savedBody!!.getString("p_api_key"))
        assertEquals("pt-PT", savedBody!!.getString("p_language"))
        assertTrue(savedBody!!.getBoolean("p_enabled"))

        val restored = api.getTmdb(null, "access-tok")
        assertNotNull(restored)
        assertEquals("tmdb-test-key", restored!!.apiKey)
        assertEquals("pt-PT", restored.language)
        assertTrue(restored.enabled)
    }
}

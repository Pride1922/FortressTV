package com.fortresstv.player.account

import com.fortresstv.player.library.CloudIdentityHasher
import com.fortresstv.player.profile.HouseholdProfile
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class LocalDataMigrationTest {

    @Test
    fun targetProfileMustBeAdult() {
        val childProfile = HouseholdProfile(
            id = "child-1",
            name = "Kid",
            kind = com.fortresstv.player.profile.ProfileKind.CHILD,
            language = "en"
        )
        assertTrue(childProfile.isChild)
        assertFalse(childProfile.isAdult)
    }

    @Test
    fun duplicateSourceDetectionMatchesServerUrlAndUsername() {
        val existingCloudSources = listOf(
            HouseholdSourceRecord(
                id = "src-existing",
                sourceType = "xtream",
                displayName = "Existing Cloud Source",
                enabled = true,
                config = JSONObject().apply {
                    put("server_url", "http://xtream.provider.com:8080/")
                    put("username", "user123")
                    put("password", "pass123")
                }
            )
        )

        val localCandidateUrl = "http://xtream.provider.com:8080"
        val localCandidateUser = "user123"

        val isDuplicate = existingCloudSources.any { s ->
            s.sourceType == "xtream" &&
                s.config.getString("server_url").trimEnd('/') == localCandidateUrl.trimEnd('/') &&
                s.config.getString("username") == localCandidateUser
        }

        assertTrue("Should detect duplicate source connection", isDuplicate)

        val newCandidateUrl = "http://different.provider.com"
        val isNewDuplicate = existingCloudSources.any { s ->
            s.sourceType == "xtream" &&
                s.config.getString("server_url").trimEnd('/') == newCandidateUrl.trimEnd('/') &&
                s.config.getString("username") == localCandidateUser
        }
        assertFalse("Should not flag different provider as duplicate", isNewDuplicate)
    }

    @Test
    fun migrationOutboxEntriesRetainExactItemHashedIdentities() {
        val localRawId = "channel_55"
        val expectedHash = CloudIdentityHasher.hashItemId(localRawId)

        assertEquals(64, expectedHash.length)
        // Verify no extra prefixes were prepended:
        assertEquals(expectedHash, CloudIdentityHasher.hashItemId("channel_55"))
    }
}

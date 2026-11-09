package com.fortresstv.player.trakt

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraktClientTest {

    private class MockTraktClient : TraktClient(
        authHost = "https://auth.trakt.tv",
        apiHost = "https://api.trakt.tv"
    ) {
        var postHandler: ((url: String, body: String, clientId: String?) -> TraktHttpResponse)? = null
        var getHandler: ((url: String, accessToken: String?, clientId: String?) -> TraktHttpResponse)? = null

        var lastPostUrl: String? = null
        var lastPostBody: String? = null
        var lastPostClientId: String? = null

        override fun executePost(
            urlString: String,
            jsonBody: String,
            accessToken: String?,
            clientId: String?
        ): TraktHttpResponse {
            lastPostUrl = urlString
            lastPostBody = jsonBody
            lastPostClientId = clientId
            return postHandler?.invoke(urlString, jsonBody, clientId)
                ?: TraktHttpResponse(500, "No mock handler", emptyMap())
        }

        override fun executeGet(
            urlString: String,
            accessToken: String?,
            clientId: String?
        ): TraktHttpResponse {
            return getHandler?.invoke(urlString, accessToken, clientId)
                ?: TraktHttpResponse(500, "No mock handler", emptyMap())
        }
    }

    @Test
    fun parseDeviceCodeFromJson() {
        val json = JSONObject("""
            {
                "device_code": "dev_code_12345",
                "user_code": "ABCD-1234",
                "verification_url": "https://trakt.tv/activate",
                "expires_in": 600,
                "interval": 5
            }
        """.trimIndent())

        val device = TraktDeviceCode.fromJson(json)
        assertEquals("dev_code_12345", device.deviceCode)
        assertEquals("ABCD-1234", device.userCode)
        assertEquals("https://trakt.tv/activate", device.verificationUrl)
        assertEquals(600, device.expiresInSeconds)
        assertEquals(5, device.intervalSeconds)
        assertFalse(device.isExpired)
    }

    @Test
    fun parseTokenFromJson() {
        val json = JSONObject("""
            {
                "access_token": "acc_tok_999",
                "token_type": "bearer",
                "expires_in": 7776000,
                "refresh_token": "ref_tok_888",
                "scope": "public",
                "created_at": 1700000000
            }
        """.trimIndent())

        val token = TraktToken.fromJson(json)
        assertEquals("acc_tok_999", token.accessToken)
        assertEquals("ref_tok_888", token.refreshToken)
        assertEquals("bearer", token.tokenType)
        assertEquals("public", token.scope)
        assertEquals(1700000000000L, token.createdAtEpochMs)
        assertEquals(7776000L, token.expiresInSeconds)
    }

    @Test
    fun parseUserProfileFromJson() {
        val json = JSONObject("""
            {
                "username": "cinephile",
                "name": "Jane Doe",
                "vip": true,
                "images": {
                    "avatar": {
                        "full": "https://walter.trakt.tv/user_avatar.jpg"
                    }
                }
            }
        """.trimIndent())

        val profile = TraktUserProfile.fromJson(json)
        assertEquals("cinephile", profile.username)
        assertEquals("Jane Doe", profile.name)
        assertTrue(profile.isVip)
        assertEquals("https://walter.trakt.tv/user_avatar.jpg", profile.avatarUrl)
    }

    @Test
    fun generateDeviceCodeUsesAuthHostAndPublicClientId() {
        val client = MockTraktClient()
        client.postHandler = { url, body, clientId ->
            assertEquals("https://auth.trakt.tv/oauth/device/code", url)
            assertEquals("fortress_test_client_id", clientId)
            val json = JSONObject(body)
            assertEquals("fortress_test_client_id", json.getString("client_id"))
            assertFalse(json.has("client_secret"))
            TraktHttpResponse(200, """
                {
                    "device_code": "dc_1",
                    "user_code": "UC-1",
                    "verification_url": "https://trakt.tv/activate",
                    "expires_in": 300,
                    "interval": 5
                }
            """.trimIndent(), emptyMap())
        }

        val result = client.generateDeviceCode("fortress_test_client_id")
        assertTrue(result is TraktAuthResult.Success)
        val code = (result as TraktAuthResult.Success).data
        assertEquals("dc_1", code.deviceCode)
        assertEquals("UC-1", code.userCode)
    }

    @Test
    fun pollDeviceTokenHandlesAllTraktStatusCodes() {
        val client = MockTraktClient()

        // 200 -> Success
        client.postHandler = { _, _, _ ->
            TraktHttpResponse(200, """
                {
                    "access_token": "acc_1",
                    "refresh_token": "ref_1",
                    "expires_in": 7776000
                }
            """.trimIndent(), emptyMap())
        }
        val res200 = client.pollDeviceToken("dc_test", "cid")
        assertTrue(res200 is TraktPollResult.Success)
        assertEquals("acc_1", (res200 as TraktPollResult.Success).token.accessToken)

        // 400 -> Pending
        client.postHandler = { _, _, _ -> TraktHttpResponse(400, "", emptyMap()) }
        assertTrue(client.pollDeviceToken("dc_test", "cid") is TraktPollResult.Pending)

        // 410 -> Expired
        client.postHandler = { _, _, _ -> TraktHttpResponse(410, "", emptyMap()) }
        assertTrue(client.pollDeviceToken("dc_test", "cid") is TraktPollResult.Expired)

        // 418 -> Denied
        client.postHandler = { _, _, _ -> TraktHttpResponse(418, "", emptyMap()) }
        assertTrue(client.pollDeviceToken("dc_test", "cid") is TraktPollResult.Denied)

        // 429 -> SlowDown
        client.postHandler = { _, _, _ -> TraktHttpResponse(429, "", mapOf("Retry-After" to "10")) }
        val res429 = client.pollDeviceToken("dc_test", "cid")
        assertTrue(res429 is TraktPollResult.SlowDown)
        assertEquals(10, (res429 as TraktPollResult.SlowDown).newIntervalSeconds)
    }

    @Test
    fun tokenRefreshOmitsClientSecret() {
        val client = MockTraktClient()
        client.postHandler = { url, body, clientId ->
            assertEquals("https://auth.trakt.tv/oauth/token", url)
            val json = JSONObject(body)
            assertEquals("ref_single_use_123", json.getString("refresh_token"))
            assertEquals("cid_456", json.getString("client_id"))
            assertEquals("refresh_token", json.getString("grant_type"))
            assertFalse(json.has("client_secret"))
            TraktHttpResponse(200, """
                {
                    "access_token": "acc_refreshed",
                    "refresh_token": "ref_replacement",
                    "expires_in": 7776000
                }
            """.trimIndent(), emptyMap())
        }

        val result = client.refreshToken("ref_single_use_123", "cid_456")
        assertTrue(result is TraktAuthResult.Success)
        val token = (result as TraktAuthResult.Success).data
        assertEquals("acc_refreshed", token.accessToken)
        assertEquals("ref_replacement", token.refreshToken)
    }

    @Test
    fun pollUntilAuthorizedCancelsWhenExpired() = runBlocking {
        val client = MockTraktClient()
        // Device code that expired 1 second ago
        val expiredDevice = TraktDeviceCode(
            deviceCode = "dc_exp",
            userCode = "UC_EXP",
            verificationUrl = "https://trakt.tv/activate",
            expiresInSeconds = -1,
            intervalSeconds = 1,
            createdAtEpochMs = System.currentTimeMillis() - 2000L
        )

        val result = client.pollUntilAuthorized(expiredDevice, "cid")
        assertTrue(result is TraktPollResult.Expired)
    }

    @Test
    fun pollUntilAuthorizedCancelsWhenJobCancelled() = runBlocking {
        val client = MockTraktClient()
        client.postHandler = { _, _, _ -> TraktHttpResponse(400, "", emptyMap()) }

        val activeDevice = TraktDeviceCode(
            deviceCode = "dc_active",
            userCode = "UC_ACT",
            verificationUrl = "https://trakt.tv/activate",
            expiresInSeconds = 300,
            intervalSeconds = 1
        )

        var pollAttempts = 0
        val job = launch {
            client.pollUntilAuthorized(activeDevice, "cid", onPollAttempt = { pollAttempts++ })
        }

        // Give it a brief moment to run first attempt, then cancel
        kotlinx.coroutines.delay(50)
        job.cancel()
        job.join()

        assertTrue(pollAttempts >= 1)
        assertTrue(job.isCancelled)
    }
}

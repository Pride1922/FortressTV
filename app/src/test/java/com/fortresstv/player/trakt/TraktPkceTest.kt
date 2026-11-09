package com.fortresstv.player.trakt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TraktPkceTest {

    @Test
    fun codeVerifierGeneratesValidCharactersAndLength() {
        val verifierDefault = TraktPkce.generateCodeVerifier()
        assertEquals(64, verifierDefault.length)
        assertTrue(verifierDefault.all { it in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~" })

        val verifierCustom = TraktPkce.generateCodeVerifier(128)
        assertEquals(128, verifierCustom.length)

        val verifierMin = TraktPkce.generateCodeVerifier(43)
        assertEquals(43, verifierMin.length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun codeVerifierRejectsTooShort() {
        TraktPkce.generateCodeVerifier(42)
    }

    @Test(expected = IllegalArgumentException::class)
    fun codeVerifierRejectsTooLong() {
        TraktPkce.generateCodeVerifier(129)
    }

    @Test
    fun codeChallengeMatchesRfc7636Vector() {
        // RFC 7636 Appendix B test vector:
        // verifier: "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        // challenge: "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val expectedChallenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"

        val challenge = TraktPkce.generateCodeChallenge(verifier)
        assertEquals(expectedChallenge, challenge)
    }

    @Test
    fun authorizeUrlContainsRequiredPkceParameters() {
        val url = TraktPkce.buildAuthorizeUrl(
            clientId = "test_client_id_123",
            redirectUri = "fortresstv://trakt/oauth",
            codeChallenge = "test_challenge_abc",
            state = "random_state_xyz"
        )

        assertTrue(url.startsWith("https://auth.trakt.tv/oauth/authorize"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("client_id=test_client_id_123"))
        assertTrue(url.contains("redirect_uri=fortresstv%3A%2F%2Ftrakt%2Foauth"))
        assertTrue(url.contains("code_challenge=test_challenge_abc"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("state=random_state_xyz"))
        assertFalse(url.contains("client_secret"))
    }
}

package com.fortresstv.player.trakt

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * RFC 7636 Proof Key for Code Exchange (PKCE) implementation for Trakt OAuth.
 * Allows client applications without a client secret to securely authenticate.
 */
object TraktPkce {

    private const val CODE_VERIFIER_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~"
    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically random code verifier string between 43 and 128 characters.
     * Default length is 64 characters.
     */
    fun generateCodeVerifier(length: Int = 64): String {
        require(length in 43..128) { "code_verifier length must be between 43 and 128 characters" }
        val chars = CharArray(length)
        for (i in 0 until length) {
            chars[i] = CODE_VERIFIER_CHARS[secureRandom.nextInt(CODE_VERIFIER_CHARS.length)]
        }
        return String(chars)
    }

    /**
     * Computes the S256 code challenge from a code verifier:
     * BASE64URL-ENCODE(SHA256(ASCII(code_verifier))) without padding.
     */
    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(StandardCharsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return base64UrlEncode(digest)
    }

    /**
     * Builds the Trakt OAuth authorize URL with PKCE parameters.
     */
    fun buildAuthorizeUrl(
        clientId: String,
        redirectUri: String,
        codeChallenge: String,
        state: String? = null,
        authHost: String = TraktEndpoints.AUTH_HOST
    ): String {
        val query = StringBuilder()
            .append("response_type=code")
            .append("&client_id=").append(urlEncode(clientId))
            .append("&redirect_uri=").append(urlEncode(redirectUri))
            .append("&code_challenge=").append(urlEncode(codeChallenge))
            .append("&code_challenge_method=S256")

        if (!state.isNullOrBlank()) {
            query.append("&state=").append(urlEncode(state))
        }

        return "${authHost.trimEnd('/')}/oauth/authorize?$query"
    }

    private fun base64UrlEncode(data: ByteArray): String {
        // Base64 URL-safe without padding as per RFC 7636
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data)
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    }
}

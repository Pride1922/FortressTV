package com.fortresstv.player.account

import org.json.JSONObject
import com.fortresstv.player.BuildConfig
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID

// Public client configuration, not a server secret. Never add a service-role key.
internal object AccountConfiguration {
    val URL = BuildConfig.SUPABASE_URL.trimEnd('/')
    val PUBLISHABLE_KEY = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    val configured: Boolean get() = runCatching {
        val uri = URI(URL)
        uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null &&
            uri.query == null && uri.fragment == null && uri.path.isNullOrEmpty() &&
            PUBLISHABLE_KEY.startsWith("sb_publishable_")
    }.getOrDefault(false)
}

internal enum class AccountError { NETWORK, INVALID_LOGIN, EXPIRED_CODE, RATE_LIMIT, WEAK_PASSWORD, SESSION_EXPIRED, SERVICE }
internal class AccountException(val reason: AccountError) : Exception(reason.name)

// Deliberately no generated toString(): sessions must never be printed in logs.
internal class AccountSession(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val authSessionId: String = extractAuthSessionId(accessToken, userId)
) {
    fun encode(): String = JSONObject().put("user_id", userId).put("email", email)
        .put("access_token", accessToken).put("refresh_token", refreshToken)
        .put("expires_at", expiresAt)
        .put("auth_session_id", authSessionId).toString()

    companion object {
        fun extractAuthSessionId(token: String, fallbackUserId: String): String {
            return runCatching {
                val parts = token.split('.')
                if (parts.size >= 2) {
                    val payloadBytes = java.util.Base64.getUrlDecoder().decode(parts[1])
                    val json = JSONObject(String(payloadBytes, Charsets.UTF_8))
                    json.optString("session_id").takeIf { it.isNotBlank() }
                        ?: json.optString("sub").takeIf { it.isNotBlank() }
                } else null
            }.getOrNull() ?: fallbackUserId
        }

        fun decode(json: JSONObject, nowSeconds: Long = System.currentTimeMillis() / 1000): AccountSession {
            val user = json.optJSONObject("user")
            val id = user?.getString("id") ?: json.getString("user_id")
            UUID.fromString(id)
            val access = json.getString("access_token").also { require(it.isNotBlank()) }
            val refresh = json.getString("refresh_token").also { require(it.isNotBlank()) }
            val authSessionId = json.optString("auth_session_id").takeIf { it.isNotBlank() }
                ?: json.optString("session_id").takeIf { it.isNotBlank() }
                ?: extractAuthSessionId(access, id)
            return AccountSession(
                userId = id,
                email = user?.optString("email").orEmpty().ifBlank { json.getString("email") },
                accessToken = access,
                refreshToken = refresh,
                expiresAt = if (json.has("expires_at")) json.getLong("expires_at")
                    else nowSeconds + json.getLong("expires_in"),
                authSessionId = authSessionId
            )
        }
    }
}

internal fun interface AuthTransport {
    fun request(method: String, path: String, body: JSONObject?, accessToken: String?): JSONObject
}

internal class SupabaseHttpTransport : AuthTransport {
    override fun request(method: String, path: String, body: JSONObject?, accessToken: String?): JSONObject {
        val connection = URI(AccountConfiguration.URL + path).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("apikey", AccountConfiguration.PUBLISHABLE_KEY)
            connection.setRequestProperty("Accept", "application/json")
            accessToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                // Only interpret the error code; never surface provider messages or bodies.
                val code = runCatching {
                    connection.errorStream?.bufferedReader()?.use { JSONObject(it.readText()).optString("error_code") }
                }.getOrNull()
                throw AccountException(when {
                    status == 429 -> AccountError.RATE_LIMIT
                    code == "weak_password" -> AccountError.WEAK_PASSWORD
                    code == "otp_expired" || code == "otp_disabled" -> AccountError.EXPIRED_CODE
                    code == "invalid_credentials" || code == "email_not_confirmed" -> AccountError.INVALID_LOGIN
                    status == 401 || code == "refresh_token_not_found" || code == "refresh_token_already_used" -> AccountError.SESSION_EXPIRED
                    else -> AccountError.SERVICE
                })
            }
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        } catch (failure: AccountException) {
            throw failure
        } catch (_: java.io.IOException) {
            throw AccountException(AccountError.NETWORK)
        } finally {
            connection.disconnect()
        }
    }
}

internal class SupabaseAuthApi(private val transport: AuthTransport = SupabaseHttpTransport()) {
    fun signIn(email: String, password: String): AccountSession = AccountSession.decode(
        transport.request("POST", "/auth/v1/token?grant_type=password", credentials(email, password), null))

    fun register(email: String, password: String): AccountSession? {
        val result = transport.request("POST", "/auth/v1/signup", credentials(email, password), null)
        return if (result.optString("access_token").isNotBlank()) AccountSession.decode(result) else null
    }

    fun recover(email: String) { transport.request("POST", "/auth/v1/recover", JSONObject().put("email", email.trim()), null) }

    fun resendSignup(email: String) {
        transport.request("POST", "/auth/v1/resend", JSONObject().put("email", email.trim()).put("type", "signup"), null)
    }

    fun verify(email: String, code: String, recovery: Boolean): AccountSession = AccountSession.decode(
        transport.request("POST", "/auth/v1/verify", JSONObject().put("email", email.trim())
            .put("token", code.trim()).put("type", if (recovery) "recovery" else "signup"), null))

    fun changePassword(session: AccountSession, password: String) {
        transport.request("PUT", "/auth/v1/user", JSONObject().put("password", password), session.accessToken)
    }

    fun refresh(session: AccountSession): AccountSession = AccountSession.decode(transport.request(
        "POST", "/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", session.refreshToken), null))

    fun signOut(session: AccountSession) {
        transport.request("POST", "/auth/v1/logout?scope=local", null, session.accessToken)
    }

    private fun credentials(email: String, password: String) = JSONObject().put("email", email.trim()).put("password", password)
}

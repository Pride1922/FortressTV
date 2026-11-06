package com.fortresstv.player.account

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AccountRepositoryTest {
    private class MemoryPersistence : SessionPersistence {
        var value: AccountSession? = null
        override fun load() = value
        override fun save(session: AccountSession) { value = session }
        override fun clear() { value = null }
    }

    private fun response(expiresAt: Long = Long.MAX_VALUE) = JSONObject()
        .put("access_token", "synthetic-access").put("refresh_token", "synthetic-refresh")
        .put("expires_at", expiresAt).put("user", JSONObject()
            .put("id", "10000000-0000-0000-0000-000000000001").put("email", "adult@example.test"))

    @Test fun loginTrimsEmailButPreservesPasswordAndStoresSession() = runBlocking {
        val persistence = MemoryPersistence()
        val repository = AccountRepository(persistence, SupabaseAuthApi(AuthTransport { method, path, body, token ->
            assertEquals("POST", method)
            assertEquals("/auth/v1/token?grant_type=password", path)
            assertEquals("adult@example.test", body!!.getString("email"))
            assertEquals("  password  ", body.getString("password"))
            assertNull(token)
            response()
        }))
        assertEquals("adult@example.test", repository.signIn(" adult@example.test ", "  password  "))
        assertNotNull(persistence.value)
    }

    @Test fun signupWithConfirmationDoesNotPersistUnverifiedUser() = runBlocking {
        val persistence = MemoryPersistence()
        val repository = AccountRepository(persistence, SupabaseAuthApi(AuthTransport { _, path, _, _ ->
            assertEquals("/auth/v1/signup", path)
            JSONObject().put("user", JSONObject().put("id", "unverified"))
        }))
        assertFalse(repository.register("adult@example.test", "password"))
        assertNull(persistence.value)
    }

    @Test fun recoverySessionIsNotPersistedAndPasswordResetRequiresLogin() = runBlocking {
        val persistence = MemoryPersistence()
        val paths = mutableListOf<String>()
        val repository = AccountRepository(persistence, SupabaseAuthApi(AuthTransport { method, path, body, token ->
            paths += path
            when (path) {
                "/auth/v1/verify" -> {
                    assertEquals("recovery", body!!.getString("type"))
                    assertEquals("123456", body.getString("token"))
                    response()
                }
                "/auth/v1/user" -> {
                    assertEquals("PUT", method)
                    assertEquals("new-password", body!!.getString("password"))
                    assertEquals("synthetic-access", token)
                    JSONObject()
                }
                else -> JSONObject()
            }
        }))
        repository.verifyRecovery("adult@example.test", "123456")
        assertNull(persistence.value)
        repository.resetPassword("new-password")
        assertNull(persistence.value)
        assertEquals(listOf("/auth/v1/verify", "/auth/v1/user", "/auth/v1/logout?scope=local"), paths)
        try { repository.resetPassword("another-password"); fail("Consumed recovery reused") }
        catch (failure: AccountException) { assertEquals(AccountError.EXPIRED_CODE, failure.reason) }
    }

    @Test fun logoutClearsLocalSessionEvenWhenServerIsOffline() = runBlocking {
        val persistence = MemoryPersistence().apply { value = AccountSession.decode(response()) }
        val repository = AccountRepository(persistence, SupabaseAuthApi(AuthTransport { _, _, _, _ ->
            assertNull(persistence.value)
            throw AccountException(AccountError.NETWORK)
        }))
        repository.restore()
        assertFalse(repository.signOut())
        assertNull(persistence.value)
        assertNull(repository.restore())
    }

    @Test fun expiredSessionRefreshIsPersistedAndRevocationClearsIt() = runBlocking {
        val persistence = MemoryPersistence().apply { value = AccountSession.decode(response(0)) }
        var revoked = false
        val repository = AccountRepository(persistence, SupabaseAuthApi(AuthTransport { _, path, body, _ ->
            assertEquals("/auth/v1/token?grant_type=refresh_token", path)
            assertEquals("synthetic-refresh", body!!.getString("refresh_token"))
            if (revoked) throw AccountException(AccountError.SESSION_EXPIRED)
            response()
        }))
        assertEquals("adult@example.test", repository.restore())
        assertEquals(Long.MAX_VALUE, persistence.value!!.expiresAt)
        revoked = true
        persistence.value = AccountSession.decode(response(0))
        assertNull(repository.restore())
        assertNull(persistence.value)
    }

    @Test fun sessionSerializationRoundTripsAndDoesNotPrintTokens() {
        val original = AccountSession.decode(response())
        val restored = AccountSession.decode(JSONObject(original.encode()))
        assertEquals(original.userId, restored.userId)
        assertEquals(original.refreshToken, restored.refreshToken)
        assertFalse(restored.toString().contains("synthetic-access"))
        assertFalse(restored.toString().contains("synthetic-refresh"))
    }

    @Test fun sessionExtractsAuthSessionIdFromJwtOrExplicitField() {
        val payload = JSONObject().put("session_id", "auth-session-uuid-123").put("sub", "10000000-0000-0000-0000-000000000001")
        val base64Payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toString().toByteArray())
        val jwt = "header.$base64Payload.signature"

        val json = JSONObject()
            .put("access_token", jwt)
            .put("refresh_token", "refresh")
            .put("expires_at", Long.MAX_VALUE)
            .put("user", JSONObject().put("id", "10000000-0000-0000-0000-000000000001").put("email", "test@test.com"))

        val session = AccountSession.decode(json)
        assertEquals("auth-session-uuid-123", session.authSessionId)

        val restored = AccountSession.decode(JSONObject(session.encode()))
        assertEquals("auth-session-uuid-123", restored.authSessionId)
    }

    @Test fun departureHandlerInvokedWithDepartingSessionOnSignOut() = runBlocking {
        val session = AccountSession.decode(response())
        val persistence = MemoryPersistence().apply { value = session }
        var capturedDeparting: AccountSession? = null
        var persistenceIntactDuringDeparture = false

        val repository = AccountRepository(persistence, SupabaseAuthApi(AuthTransport { _, _, _, _ -> JSONObject() }))
        repository.setDepartureHandler { departing ->
            capturedDeparting = departing
            persistenceIntactDuringDeparture = (persistence.value != null)
        }

        repository.restore()
        repository.signOut()

        assertNotNull("Departure handler must be invoked on signOut", capturedDeparting)
        assertEquals(session.userId, capturedDeparting?.userId)
        assertTrue("Persistence must still be intact when departure handler runs", persistenceIntactDuringDeparture)
        assertNull("Persistence must be cleared after signOut finishes", persistence.value)
    }

    @Test fun departureHandlerInvokedWhenAcceptingDifferentUserSession() = runBlocking {
        val user1Session = AccountSession(
            userId = "10000000-0000-0000-0000-000000000001",
            email = "user1@test.com",
            accessToken = "token-1",
            refreshToken = "refresh-1",
            expiresAt = Long.MAX_VALUE
        )
        val persistence = MemoryPersistence().apply { value = user1Session }
        var departingUser: String? = null

        val repository = AccountRepository(persistence, SupabaseAuthApi(AuthTransport { _, _, _, _ ->
            JSONObject().put("access_token", "token-2").put("refresh_token", "refresh-2")
                .put("expires_at", Long.MAX_VALUE)
                .put("user", JSONObject().put("id", "20000000-0000-0000-0000-000000000002").put("email", "user2@test.com"))
        }))
        repository.setDepartureHandler { departing ->
            departingUser = departing.userId
        }

        repository.restore()
        repository.signIn("user2@test.com", "password")

        assertEquals("10000000-0000-0000-0000-000000000001", departingUser)
        assertEquals("20000000-0000-0000-0000-000000000002", repository.currentUserId())
    }
}

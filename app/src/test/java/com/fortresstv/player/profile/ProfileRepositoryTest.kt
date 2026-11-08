package com.fortresstv.player.profile

import com.fortresstv.player.account.AccountRepository
import com.fortresstv.player.account.AccountSession
import com.fortresstv.player.account.SessionPersistence
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ProfileRepositoryTest {

    private class MemoryProfilePersistence : ProfilePersistence {
        private data class Storage(
            var json: String? = null,
            var activeId: String? = null,
            var sessionToken: String? = null,
            var pendingRevocation: Boolean = false,
            val pendingSessions: MutableSet<String> = mutableSetOf()
        )
        private val namespaces = mutableMapOf<String, Storage>()
        private var currentNs = "local"

        private val cur: Storage get() = namespaces.getOrPut(currentNs) { Storage() }

        override fun loadProfilesJson(): String? = cur.json
        override fun saveProfilesJson(json: String) { cur.json = json }
        override fun loadActiveProfileId(): String? = cur.activeId
        override fun saveActiveProfileId(id: String) { cur.activeId = id }
        override fun loadActiveSessionToken(): String? = cur.sessionToken
        override fun saveActiveSessionToken(token: String?) { cur.sessionToken = token }
        override fun loadPendingAdultRevocationSessions(): Set<String> = cur.pendingSessions.toSet()
        override fun addPendingAdultRevocationSession(authSessionId: String) {
            cur.pendingSessions.add(authSessionId)
            cur.pendingRevocation = true
        }
        override fun removePendingAdultRevocationSession(authSessionId: String) {
            cur.pendingSessions.remove(authSessionId)
            if (cur.pendingSessions.isEmpty()) {
                cur.pendingRevocation = false
            }
        }
        override fun clearPendingAdultRevocationSessions() {
            cur.pendingSessions.clear()
            cur.pendingRevocation = false
        }
        override fun loadPendingAdultRevocation(): Boolean = cur.pendingSessions.isNotEmpty() || cur.pendingRevocation
        override fun savePendingAdultRevocation(pending: Boolean) {
            cur.pendingRevocation = pending
            if (!pending) cur.pendingSessions.clear()
        }
        override fun switchNamespace(namespace: String) { currentNs = namespace }
    }

    private class MemoryPinPersistence : PinPersistence {
        private val namespaces = mutableMapOf<String, JSONObject>()
        private var currentNs = "local"

        override fun load(): PinPayloadResult {
            val data = namespaces[currentNs] ?: return if (currentNs == "local") PinPayloadResult.UnconfiguredLocal else PinPayloadResult.UnconfirmedAuthenticated
            return PinPayloadResult.Valid(JSONObject(data.toString()))
        }
        override fun save(json: JSONObject) { namespaces[currentNs] = JSONObject(json.toString()) }
        override fun switchNamespace(namespace: String) { currentNs = namespace }
    }

    private class MemorySessionPersistence : SessionPersistence {
        var session: AccountSession? = null
        override fun load(): AccountSession? = session
        override fun save(session: AccountSession) { this.session = session }
        override fun clear() { session = null }
    }

    private class FakeProfileTransport(
        var sessionResult: JSONObject? = null,
        var shouldThrowOnSession: Boolean = false,
        var shouldThrowOnLeaveAdultMode: Boolean = false
    ) : ProfileTransport {
        var leaveAdultModeCalled = false
        var leaveAdultModeCount = 0
        val leaveAdultModeTokens = mutableListOf<String?>()
        val leaveAdultModeTargets = mutableListOf<String?>()

        override fun request(
            method: String,
            path: String,
            body: JSONObject?,
            accessToken: String?,
            headers: Map<String, String>?
        ): String {
            if (path.contains("fortress_leave_adult_mode")) {
                leaveAdultModeCalled = true
                leaveAdultModeCount++
                leaveAdultModeTokens.add(accessToken)
                leaveAdultModeTargets.add(body?.optString("p_target_auth_session_id")?.takeIf { it.isNotBlank() })
                if (shouldThrowOnLeaveAdultMode) throw java.io.IOException("HTTP 500: Revocation failure")
                return "true"
            }
            if (path.contains("fortress_open_profile_session")) {
                if (shouldThrowOnSession) throw java.io.IOException("HTTP 403: Forbidden")
                return sessionResult?.toString() ?: "{\"session_token\":\"fake-session-uuid\"}"
            }
            if (path.contains("fortress_verify_parental_pin")) {
                return "{\"verified\":true,\"unlock_token\":\"fake-unlock-token\"}"
            }
            if (path.contains("fortress_set_parental_pin") || path.contains("fortress_remove_parental_pin") || path.contains("fortress_reset_parental_pin_after_recovery")) {
                return "true"
            }
            if (path.contains("fortress_create_profile")) {
                return "\"fake-profile-uuid\""
            }
            if (path.contains("fortress_update_profile") || path.contains("fortress_delete_profile") || path.contains("fortress_set_profile_hide_adult_content")) {
                return "true"
            }
            if (path.contains("household_profiles")) {
                return "[]"
            }
            return "{}"
        }
    }

    @Test
    fun initialLaunchCreatesDefaultAdultProfile() {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val accountRepo = AccountRepository(MemorySessionPersistence())
        val repo = ProfileRepository(persistence, accountRepo, pinStore)

        val profiles = repo.profiles.value
        assertEquals(1, profiles.size)
        val defaultProfile = profiles[0]
        assertEquals(ProfileKind.ADULT, defaultProfile.kind)
        assertEquals(defaultProfile.id, repo.activeProfile.value.id)
    }

    @Test
    fun selectingChildProfileRevokesAdultUnlock() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val accountRepo = AccountRepository(MemorySessionPersistence())
        val repo = ProfileRepository(persistence, accountRepo, pinStore)

        val child = HouseholdProfile(
            name = "Kid",
            kind = ProfileKind.CHILD,
            language = "en",
            hideAdultContent = true,
            maximumAge = 10,
            blockUnrated = true
        )
        repo.saveProfile(child)

        // Grant adult unlock
        pinStore.grantAdultUnlock("fake-token")
        assertTrue(pinStore.isAdultUnlocked())

        // Selecting child profile MUST revoke adult unlock immediately
        assertTrue(repo.selectProfile(child))
        assertEquals(child.id, repo.activeProfile.value.id)
        assertTrue(repo.activeProfile.value.isChild)
        assertFalse(pinStore.isAdultUnlocked())
    }

    @Test
    fun switchingToAdultProfileRequiresUnlockWhenPinIsConfigured() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val accountRepo = AccountRepository(MemorySessionPersistence())
        val repo = ProfileRepository(persistence, accountRepo, pinStore)

        val adult = repo.activeProfile.value
        val child = HouseholdProfile(
            name = "Junior",
            kind = ProfileKind.CHILD,
            maximumAge = 8
        )
        repo.saveProfile(child)

        // Set PIN
        repo.setPin("1234")
        assertTrue(repo.getPinStatus().hasPin)

        // Select child profile
        assertTrue(repo.selectProfile(child))
        assertEquals(child.id, repo.activeProfile.value.id)

        // Lock adult mode
        repo.revokeAdultUnlock()
        assertFalse(repo.isAdultUnlocked())

        // Attempting to select adult profile without unlock must be DENIED
        assertFalse(repo.selectProfile(adult))
        assertEquals(child.id, repo.activeProfile.value.id)

        // Verify PIN -> unlock granted
        val verifyRes = repo.verifyPin("1234")
        assertTrue(verifyRes is PinVerifyResult.Success)
        assertTrue(repo.isAdultUnlocked())

        // Now selecting adult profile SUCCEEDS
        assertTrue(repo.selectProfile(adult))
        assertEquals(adult.id, repo.activeProfile.value.id)
    }

    @Test
    fun deletingActiveProfileSwitchesToChildOrRestricted() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val accountRepo = AccountRepository(MemorySessionPersistence())
        val repo = ProfileRepository(persistence, accountRepo, pinStore)

        val adult = repo.activeProfile.value
        assertFalse(repo.deleteProfile(adult.id))

        val child = HouseholdProfile(
            name = "Second Profile",
            kind = ProfileKind.CHILD,
            maximumAge = 12
        )
        repo.saveProfile(child)
        assertEquals(2, repo.profiles.value.size)

        // Deleting active adult switches active to child
        assertTrue(repo.deleteProfile(adult.id))
        assertEquals(1, repo.profiles.value.size)
        assertEquals(child.id, repo.activeProfile.value.id)

        assertFalse(repo.deleteProfile(child.id))
        assertEquals(1, repo.profiles.value.size)
    }

    @Test
    fun missingOrCorruptSavedIdFailsClosedToChildOrRestricted() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val accountRepo = AccountRepository(MemorySessionPersistence())
        val repo = ProfileRepository(persistence, accountRepo, pinStore)

        val adult = repo.activeProfile.value
        val child = HouseholdProfile(
            name = "Child Profile",
            kind = ProfileKind.CHILD,
            maximumAge = 10
        )
        repo.saveProfile(child)

        // Set PIN and lock
        repo.setPin("1234")
        repo.revokeAdultUnlock()

        // Corrupt active profile ID in storage (e.g. invalid / deleted ID)
        persistence.saveActiveProfileId("non-existent-id")

        // Reload local storage: must fail closed to the child profile, NEVER adult!
        repo.onAccountChanged("local")
        assertEquals(child.id, repo.activeProfile.value.id)
        assertTrue(repo.activeProfile.value.isChild)
    }

    @Test
    fun accountSwitchIsolatesProfilesAndActiveSelection() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence()
        val accountRepo = AccountRepository(sessionPersistence)
        val repo = ProfileRepository(persistence, accountRepo, pinStore)

        // Account A has 1 adult and sets a child profile
        repo.onAccountChanged("user_A")
        val childA = HouseholdProfile(name = "Child A", kind = ProfileKind.CHILD, maximumAge = 6)
        repo.saveProfile(childA)
        repo.selectProfile(childA)
        assertEquals(childA.id, repo.activeProfile.value.id)
        assertEquals(2, repo.profiles.value.size)

        // Switch to Account B: must NOT inherit Account A's profiles or selection!
        repo.onAccountChanged("user_B")
        assertEquals(1, repo.profiles.value.size) // Fresh default profile for user B
        assertNotEquals(childA.id, repo.activeProfile.value.id)
        assertFalse(repo.profiles.value.any { it.name == "Child A" })

        // Switch back to Account A: Account A's state is preserved
        repo.onAccountChanged("user_A")
        assertEquals(2, repo.profiles.value.size)
        assertTrue(repo.profiles.value.any { it.name == "Child A" })
    }

    @Test
    fun rejectedServerAuthorizationDoesNotGrantAdultAccessLocally() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence().apply {
            session = AccountSession(
                userId = "test-user-id",
                email = "user@test.com",
                accessToken = "valid-token",
                refreshToken = "valid-refresh",
                expiresAt = System.currentTimeMillis() / 1000 + 3600
            )
        }
        val accountRepo = AccountRepository(sessionPersistence)
        val transport = FakeProfileTransport(shouldThrowOnSession = true)
        val api = SupabaseProfileApi(transport)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        val child = HouseholdProfile(name = "Kid", kind = ProfileKind.CHILD, maximumAge = 8)
        repo.saveProfile(child)
        repo.selectProfile(child)
        assertEquals(child.id, repo.activeProfile.value.id)

        // Pin is set
        repo.setPin("1234")
        pinStore.grantAdultUnlock("fake-token") // Client claims unlocked

        // Server rejects opening adult session (transport throws)
        val adult = repo.profiles.value.first { it.isAdult }
        val success = repo.selectProfile(adult)

        // Must be rejected!
        assertFalse(success)
        assertEquals(child.id, repo.activeProfile.value.id) // Still child!
    }

    @Test
    fun pinChangeOrRemovalInvalidatesAdultUnlockAndSessionToken() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val accountRepo = AccountRepository(MemorySessionPersistence())
        val repo = ProfileRepository(persistence, accountRepo, pinStore)

        repo.setPin("1234")
        pinStore.grantAdultUnlock("token-1")
        persistence.saveActiveSessionToken("session-token-1")
        assertTrue(repo.isAdultUnlocked())
        assertNotNull(repo.getActiveSessionToken())

        // Change PIN
        repo.setPin("5678")
        assertFalse(repo.isAdultUnlocked())
        assertNull(repo.getActiveSessionToken())

        // Unlock again
        pinStore.grantAdultUnlock("token-2")
        persistence.saveActiveSessionToken("session-token-2")
        assertTrue(repo.isAdultUnlocked())

        // Remove PIN
        repo.removePin()
        assertFalse(repo.isAdultUnlocked())
        assertNull(repo.getActiveSessionToken())
    }

    @Test
    fun serverRejectionWithCachedHasPinFalseBlocksAdultSelection() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence().apply {
            session = AccountSession(
                userId = "test-user-id",
                email = "user@test.com",
                accessToken = "valid-token",
                refreshToken = "valid-refresh",
                expiresAt = System.currentTimeMillis() / 1000 + 3600
            )
        }
        val accountRepo = AccountRepository(sessionPersistence)
        val transport = FakeProfileTransport(shouldThrowOnSession = true)
        val api = SupabaseProfileApi(transport)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        // Cache explicitly says NO PIN
        pinStore.syncServerStatus(hasPin = false, isLocked = false, remainingAttempts = 5, lockoutSeconds = 0)
        assertFalse(pinStore.getStatus().hasPin)

        val child = HouseholdProfile(name = "Child", kind = ProfileKind.CHILD, maximumAge = 5)
        repo.saveProfile(child)
        repo.selectProfile(child)
        assertEquals(child.id, repo.activeProfile.value.id)

        // Try selecting adult while online: server throws
        val adult = repo.profiles.value.first { it.isAdult }
        val success = repo.selectProfile(adult)

        assertFalse("Adult selection must fail when server rejects, even if hasPin=false", success)
        assertEquals(child.id, repo.activeProfile.value.id)
    }

    @Test
    fun missingSessionTokenInServerResponseBlocksAdultSelection() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence().apply {
            session = AccountSession(
                userId = "test-user-id",
                email = "user@test.com",
                accessToken = "valid-token",
                refreshToken = "valid-refresh",
                expiresAt = System.currentTimeMillis() / 1000 + 3600
            )
        }
        val accountRepo = AccountRepository(sessionPersistence)
        // Transport returns empty json instead of session_token
        val transport = FakeProfileTransport(sessionResult = JSONObject("{}"))
        val api = SupabaseProfileApi(transport)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        val child = HouseholdProfile(name = "Child", kind = ProfileKind.CHILD, maximumAge = 5)
        repo.saveProfile(child)
        repo.selectProfile(child)

        val adult = repo.profiles.value.first { it.isAdult }
        val success = repo.selectProfile(adult)

        assertFalse("Adult selection must fail when session token is missing", success)
        assertEquals(child.id, repo.activeProfile.value.id)
    }

    @Test
    fun selectingChildProfileCallsLeaveAdultModeOnServer() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence().apply {
            session = AccountSession(
                userId = "test-user-id",
                email = "user@test.com",
                accessToken = "valid-token",
                refreshToken = "valid-refresh",
                expiresAt = System.currentTimeMillis() / 1000 + 3600
            )
        }
        val accountRepo = AccountRepository(sessionPersistence)
        val transport = FakeProfileTransport()
        val api = SupabaseProfileApi(transport)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        pinStore.grantAdultUnlock("fake-unlock")
        assertTrue(pinStore.isAdultUnlocked())

        val child = HouseholdProfile(name = "Child", kind = ProfileKind.CHILD, maximumAge = 5)
        repo.saveProfile(child)

        val success = repo.selectProfile(child)
        assertTrue(success)
        assertEquals(child.id, repo.activeProfile.value.id)
        assertFalse("Selecting child must immediately revoke local adult unlock", pinStore.isAdultUnlocked())
        assertTrue("Selecting child must call leaveAdultMode on server", transport.leaveAdultModeCalled)
    }

    @Test
    fun authenticatedAccountWithMissingActiveProfileFailsClosedToRestrictedDefault() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence().apply {
            session = AccountSession(
                userId = "auth-user-123",
                email = "user@test.com",
                accessToken = "valid-token",
                refreshToken = "valid-refresh",
                expiresAt = System.currentTimeMillis() / 1000 + 3600
            )
        }
        val accountRepo = AccountRepository(sessionPersistence)
        val transport = FakeProfileTransport()
        val api = SupabaseProfileApi(transport)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        // Switch to authenticated account namespace
        repo.onAccountChanged("auth-user-123")
        // Active profiles in storage: 1 adult profile, NO child profile exists yet
        val adult = repo.profiles.value.first { it.isAdult }
        // Corrupt or remove active profile ID
        persistence.saveActiveProfileId("non-existent-or-corrupted-id")

        // Trigger selection reload
        repo.onAccountChanged("auth-user-123")

        // Must FAIL CLOSED: active profile must NOT be the adult profile!
        assertNotEquals(adult.id, repo.activeProfile.value.id)
        assertTrue("Must fall back to restricted profile", repo.activeProfile.value.isChild)
        assertTrue(repo.activeProfile.value.hideAdultContent)
        assertEquals(HouseholdProfile.restrictedDefault().maximumAge, repo.activeProfile.value.maximumAge)
    }

    @Test
    fun authenticatedOfflineModeRequiresValidUnlockWhenPinConfigured() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence().apply {
            session = AccountSession(
                userId = "auth-user-123",
                email = "user@test.com",
                accessToken = "expired-token",
                refreshToken = "expired-refresh",
                expiresAt = 0 // Expired: offline!
            )
        }
        val accountRepo = AccountRepository(sessionPersistence)
        val transport = FakeProfileTransport()
        val api = SupabaseProfileApi(transport)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        repo.onAccountChanged("auth-user-123")
        val child = HouseholdProfile(name = "Child", kind = ProfileKind.CHILD, maximumAge = 6)
        repo.saveProfile(child)
        repo.selectProfile(child)
        assertEquals(child.id, repo.activeProfile.value.id)

        // PIN is configured
        repo.setPin("1234")
        repo.revokeAdultUnlock()

        val adult = repo.profiles.value.first { it.isAdult }

        // Attempt adult selection while locked offline: fails
        val fail = repo.selectProfile(adult)
        assertFalse(fail)
        assertEquals(child.id, repo.activeProfile.value.id)

        // Now unlock locally with PIN
        pinStore.grantAdultUnlock()
        val success = repo.selectProfile(adult)
        assertTrue(success)
        assertEquals(adult.id, repo.activeProfile.value.id)
    }

    @Test
    fun offlineChildEntryPersistsPendingRevocationAndFlushesOnReconnection() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence().apply {
            session = AccountSession(
                userId = "user-1",
                email = "user1@test.com",
                accessToken = "valid-token-1",
                refreshToken = "refresh-1",
                expiresAt = 0 // Expired / offline!
            )
        }
        val accountRepo = AccountRepository(sessionPersistence)
        val transport = FakeProfileTransport()
        val api = SupabaseProfileApi(transport)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        repo.onAccountChanged("user-1")
        val child = HouseholdProfile(name = "Child", kind = ProfileKind.CHILD, maximumAge = 5)
        repo.saveProfile(child)

        // Select child profile while OFFLINE:
        val selected = repo.selectProfile(child)
        assertTrue(selected)
        assertEquals(child.id, repo.activeProfile.value.id)

        // Must persist pending adult revocation in storage!
        assertTrue("Pending adult revocation must be persisted on offline child entry", persistence.loadPendingAdultRevocation())
        assertEquals(0, transport.leaveAdultModeCount)

        // Now device comes back online with valid token
        sessionPersistence.session = AccountSession(
            userId = "user-1",
            email = "user1@test.com",
            accessToken = "valid-token-1",
            refreshToken = "refresh-1",
            expiresAt = System.currentTimeMillis() / 1000 + 3600
        )
        accountRepo.restore()

        // Trigger authenticated operation / refresh
        repo.refresh()

        // Pending revocation must now be completed on the server and cleared from persistence!
        assertFalse("Pending revocation must be cleared once server succeeds", persistence.loadPendingAdultRevocation())
        assertTrue("Server leaveAdultMode must have been called upon reconnection", transport.leaveAdultModeCalled)
        assertEquals("valid-token-1", transport.leaveAdultModeTokens.last())
    }

    @Test
    fun adultActivationBlockedWhilePendingRevocationRemainsUnconfirmed() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence().apply {
            session = AccountSession(
                userId = "user-1",
                email = "user1@test.com",
                accessToken = "valid-token-1",
                refreshToken = "refresh-1",
                expiresAt = System.currentTimeMillis() / 1000 + 3600
            )
        }
        val accountRepo = AccountRepository(sessionPersistence)
        val transport = FakeProfileTransport(shouldThrowOnLeaveAdultMode = true) // Revocation fails!
        val api = SupabaseProfileApi(transport)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        repo.onAccountChanged("user-1")
        val child = HouseholdProfile(name = "Child", kind = ProfileKind.CHILD, maximumAge = 5)
        repo.saveProfile(child)

        // Select child: server revocation throws
        repo.selectProfile(child)
        assertTrue("Pending revocation must remain true if server fails", persistence.loadPendingAdultRevocation())

        // Try to activate adult: server revocation remains unconfirmed, so adult activation MUST be rejected!
        val adult = repo.profiles.value.first { it.isAdult }
        val canSelectAdult = repo.selectProfile(adult)
        assertFalse("Adult activation must fail while pending revocation remains unconfirmed", canSelectAdult)
        assertEquals(child.id, repo.activeProfile.value.id)

        // Now server succeeds
        transport.shouldThrowOnLeaveAdultMode = false
        val canSelectAdultNow = repo.selectProfile(adult)
        assertTrue("Adult activation can succeed once pending revocation completes", canSelectAdultNow)
        assertFalse(persistence.loadPendingAdultRevocation())
        assertEquals(adult.id, repo.activeProfile.value.id)
    }

    @Test
    fun logoutRevokesUsingDepartingSessionBeforeClearingCredentials() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence().apply {
            session = AccountSession(
                userId = "departing-user-123",
                email = "user@test.com",
                accessToken = "departing-access-token",
                refreshToken = "departing-refresh-token",
                expiresAt = System.currentTimeMillis() / 1000 + 3600
            )
        }
        val transport = FakeProfileTransport()
        val api = SupabaseProfileApi(transport)
        val accountRepo = AccountRepository(sessionPersistence)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        repo.onAccountChanged("departing-user-123")
        pinStore.grantAdultUnlock("token-123")
        assertTrue(pinStore.isAdultUnlocked())

        // Simulate Logout sequence (AccountTab.doSignOut):
        // 1. prepareAccountDeparture()
        repo.prepareAccountDeparture()

        // Assert that leaveAdultMode was called with the DEPARTING session token
        assertTrue(transport.leaveAdultModeCalled)
        assertEquals("departing-access-token", transport.leaveAdultModeTokens.last())
        assertFalse("Local adult unlock must be revoked immediately on departure", pinStore.isAdultUnlocked())

        // 2. signOut() clears credentials
        accountRepo.signOut()
        assertNull(accountRepo.currentUserId())

        // 3. onAccountChanged(null)
        repo.onAccountChanged(null)
        assertEquals(ProfileKind.ADULT, repo.activeProfile.value.kind)
        assertFalse(repo.isAdultUnlocked())
    }

    @Test
    fun accountSwitchingRevokesDepartingSessionTokenNotNewAccount() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence().apply {
            session = AccountSession(
                userId = "account-A",
                email = "a@test.com",
                accessToken = "token-A",
                refreshToken = "refresh-A",
                expiresAt = System.currentTimeMillis() / 1000 + 3600
            )
        }
        val transport = FakeProfileTransport()
        val api = SupabaseProfileApi(transport)
        val accountRepo = AccountRepository(sessionPersistence)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        repo.onAccountChanged("account-A")
        pinStore.grantAdultUnlock("unlock-A")

        // Switch to Account B (simulate AccountTab.submit):
        // 1. prepareAccountDeparture() for Account A
        repo.prepareAccountDeparture()
        assertEquals("token-A", transport.leaveAdultModeTokens.last())

        // 2. Account B signs in
        val sessionB = AccountSession(
            userId = "account-B",
            email = "b@test.com",
            accessToken = "token-B",
            refreshToken = "refresh-B",
            expiresAt = System.currentTimeMillis() / 1000 + 3600
        )
        sessionPersistence.save(sessionB)
        accountRepo.restore()
        assertEquals("account-B", accountRepo.currentUserId())

        // Reset transport tracker
        transport.leaveAdultModeTokens.clear()

        // 3. Switch profile repository to Account B
        repo.onAccountChanged("account-B")

        // Assert that onAccountChanged did NOT call leaveAdultMode with Account B's token!
        assertTrue("Account B's token must not be mistakenly revoked on account change", transport.leaveAdultModeTokens.isEmpty())
    }

    @Test
    fun offlineLogoutPreservesPendingRevocationForDepartingAccount() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence().apply {
            session = AccountSession(
                userId = "offline-user-1",
                email = "off@test.com",
                accessToken = "off-token",
                refreshToken = "off-refresh",
                expiresAt = 0 // Expired / offline
            )
        }
        val transport = FakeProfileTransport(shouldThrowOnLeaveAdultMode = true)
        val api = SupabaseProfileApi(transport)
        val accountRepo = AccountRepository(sessionPersistence)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        repo.onAccountChanged("offline-user-1")

        // Offline departure
        repo.prepareAccountDeparture()

        // In offline-user-1 namespace, pending adult revocation MUST be persisted
        assertTrue(persistence.loadPendingAdultRevocation())

        // Sign out
        accountRepo.signOut()
        repo.onAccountChanged(null)

        // In local namespace, pending revocation is false
        assertFalse(persistence.loadPendingAdultRevocation())

        // When logging back into offline-user-1 later:
        repo.onAccountChanged("offline-user-1")
        assertTrue("When switching back to offline-user-1, pending revocation must still be present", persistence.loadPendingAdultRevocation())
    }

    @Test
    fun offlineLogoutFromSessionAThenLoginAsSessionBRevokesANotB() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionA = AccountSession(
            userId = "same-user-id",
            email = "user@test.com",
            accessToken = "token-A",
            refreshToken = "refresh-A",
            expiresAt = 0, // offline
            authSessionId = "auth-session-A"
        )
        val sessionPersistence = MemorySessionPersistence().apply { session = sessionA }
        val transport = FakeProfileTransport(shouldThrowOnLeaveAdultMode = true)
        val api = SupabaseProfileApi(transport)
        val accountRepo = AccountRepository(sessionPersistence)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        repo.onAccountChanged("same-user-id")

        // 1. Session A logs out while offline
        repo.prepareAccountDeparture()
        accountRepo.signOut()
        repo.onAccountChanged(null)

        // Verify that pending adult revocation in "same-user-id" specifically recorded "auth-session-A"
        repo.onAccountChanged("same-user-id")
        assertTrue(persistence.loadPendingAdultRevocation())
        assertTrue(persistence.loadPendingAdultRevocationSessions().contains("auth-session-A"))

        // 2. Now connect and log into the SAME account as Session B
        val sessionB = AccountSession(
            userId = "same-user-id",
            email = "user@test.com",
            accessToken = "token-B",
            refreshToken = "refresh-B",
            expiresAt = System.currentTimeMillis() / 1000 + 3600,
            authSessionId = "auth-session-B"
        )
        sessionPersistence.save(sessionB)
        accountRepo.restore()
        assertEquals("same-user-id", accountRepo.currentUserId())
        assertEquals("auth-session-B", accountRepo.currentAuthSessionId())

        // Bring network back online
        transport.shouldThrowOnLeaveAdultMode = false
        transport.leaveAdultModeCalled = false
        transport.leaveAdultModeTokens.clear()
        transport.leaveAdultModeTargets.clear()

        // 3. Complete pending revocation (e.g. during refresh)
        val cleanupSuccess = repo.completePendingAdultRevocation()
        assertTrue("Cleanup should succeed once online", cleanupSuccess)

        // CRITICAL ASSERTION: The server call was authenticated with Session B, but targeted Session A!
        assertTrue("leaveAdultMode must be called", transport.leaveAdultModeCalled)
        assertEquals("token-B", transport.leaveAdultModeTokens.last())
        assertEquals("auth-session-A", transport.leaveAdultModeTargets.last())
        assertFalse("Session B must NOT have targeted its own auth session", transport.leaveAdultModeTargets.contains("auth-session-B"))

        // Pending revocation is now cleared
        assertFalse("Pending revocation must be cleared once Session A is confirmed revoked", persistence.loadPendingAdultRevocation())
        assertTrue(persistence.loadPendingAdultRevocationSessions().isEmpty())
    }

    @Test
    fun operationsHaltWhenPendingRevocationCleanupRemainsUnconfirmed() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val session = AccountSession(
            userId = "user-cleanup-test",
            email = "test@test.com",
            accessToken = "valid-token",
            refreshToken = "refresh-token",
            expiresAt = System.currentTimeMillis() / 1000 + 3600,
            authSessionId = "session-1"
        )
        val sessionPersistence = MemorySessionPersistence().apply { this.session = session }
        val transport = FakeProfileTransport(shouldThrowOnLeaveAdultMode = true)
        val api = SupabaseProfileApi(transport)
        val accountRepo = AccountRepository(sessionPersistence)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        repo.onAccountChanged("user-cleanup-test")

        // Seed initial local profile
        val adultProfile = HouseholdProfile.defaultAdult().copy(id = "adult-1")
        val childProfile = HouseholdProfile(
            id = "child-1",
            name = "Kid",
            kind = ProfileKind.CHILD,
            language = "en",
            hideAdultContent = true,
            maximumAge = 10,
            blockUnrated = true
        )
        persistence.saveProfilesJson(org.json.JSONArray().put(adultProfile.encode()).put(childProfile.encode()).toString())
        repo.onAccountChanged("user-cleanup-test")

        // Simulate an unconfirmed pending revocation
        persistence.savePendingAdultRevocation(true)
        persistence.addPendingAdultRevocationSession("old-session")

        // 1. refresh() must stop protected operations and fall back to local storage
        repo.refresh()
        // Still has pending revocation because server call threw exception
        assertTrue(persistence.loadPendingAdultRevocation())

        // 2. deleteProfile() must halt and return false
        val deleteResult = repo.deleteProfile(childProfile.id)
        assertFalse("deleteProfile must halt when pending cleanup is unconfirmed", deleteResult)

        // 3. saveProfile() must halt and throw IllegalStateException
        try {
            repo.saveProfile(childProfile)
            fail("saveProfile must throw when pending cleanup is unconfirmed")
        } catch (_: IllegalStateException) {
            // Expected
        }

        // 4. selectProfile(adult) must halt and return false
        val selectAdultResult = repo.selectProfile(adultProfile)
        assertFalse("selectProfile(adult) must halt when pending cleanup is unconfirmed", selectAdultResult)

        // 5. setPin() and removePin() must halt and return false
        val setPinResult = repo.setPin("1234")
        assertFalse("setPin must halt when pending cleanup is unconfirmed", setPinResult)

        val removePinResult = repo.removePin()
        assertFalse("removePin must halt when pending cleanup is unconfirmed", removePinResult)

        // Now simulate server recovery:
        transport.shouldThrowOnLeaveAdultMode = false
        val cleanupSuccess = repo.completePendingAdultRevocation()
        assertTrue(cleanupSuccess)
        assertFalse(persistence.loadPendingAdultRevocation())

        // Now protected operations can proceed
        val secondDelete = repo.deleteProfile(childProfile.id)
        assertTrue(secondDelete)
    }

    @Test
    fun coldLaunchPreservesRememberedProfileIdWithoutGrantingAdultAccess() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinPersistence = MemoryPinPersistence()
        val pinStore = ParentalPinStore(pinPersistence)
        val sessionPersistence = MemorySessionPersistence()
        val accountRepo = AccountRepository(sessionPersistence)
        val transport = FakeProfileTransport()
        val api = SupabaseProfileApi(transport)

        // Set up adult profile and PIN
        val adultProfile = HouseholdProfile(
            name = "Adult 1",
            kind = ProfileKind.ADULT
        )
        persistence.saveProfilesJson(org.json.JSONArray().put(adultProfile.encode()).toString())
        persistence.saveActiveProfileId(adultProfile.id)

        // Configure PIN
        pinStore.setPinLocally("1234")

        // Create repository as on cold launch
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        // 1. Must NOT grant adult access automatically
        assertFalse(repo.isAdultUnlocked())
        assertEquals(adultProfile.id, repo.activeProfile.value.id)

        // 2. Must preserve the remembered last active profile ID for highlighting in the picker
        assertEquals(adultProfile.id, repo.getLastActiveProfileId())
    }

    @Test
    fun childProfileSelectsWithoutPinWhileAdultRequiresPin() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinPersistence = MemoryPinPersistence()
        val pinStore = ParentalPinStore(pinPersistence)
        val sessionPersistence = MemorySessionPersistence()
        val accountRepo = AccountRepository(sessionPersistence)
        val transport = FakeProfileTransport()
        val api = SupabaseProfileApi(transport)

        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        val adult = repo.activeProfile.value
        val child = HouseholdProfile(
            name = "Kid",
            kind = ProfileKind.CHILD,
            maximumAge = 8
        )
        repo.saveProfile(child)
        repo.setPin("5555")

        // Selecting child succeeds immediately without PIN or adult unlock
        assertTrue(repo.selectProfile(child))
        assertEquals(child.id, repo.activeProfile.value.id)
        assertFalse(repo.isAdultUnlocked())

        // From child mode, switching to adult profile without PIN verification fails
        assertFalse(repo.selectProfile(adult))

        // After PIN verification, switching to adult profile succeeds
        val verify = repo.verifyPin("5555")
        assertTrue(verify is PinVerifyResult.Success)
        assertTrue(repo.isAdultUnlocked())
        assertTrue(repo.selectProfile(adult))
        assertEquals(adult.id, repo.activeProfile.value.id)
    }

    @Test
    fun revokeAdultUnlockLocksAdultStatePromptly() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val accountRepo = AccountRepository(MemorySessionPersistence())
        val repo = ProfileRepository(persistence, accountRepo, pinStore)

        pinStore.grantAdultUnlock("unlock-token-123")
        assertTrue(repo.isAdultUnlocked())

        // Inactivity timeout / background lock revokes unlock
        repo.revokeAdultUnlock()
        assertFalse(repo.isAdultUnlocked())
    }

    @Test
    fun resetPinAfterRecoveryUpdatesPinAndStatus() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence()
        val accountRepo = AccountRepository(sessionPersistence)
        val transport = FakeProfileTransport()
        val api = SupabaseProfileApi(transport)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        // Set initial PIN
        repo.setPin("1111")
        assertTrue(repo.getPinStatus().hasPin)

        // Simulate recovering via OTP and resetting PIN to 9999
        val resetSuccess = repo.resetPinAfterRecovery("9999", "recovery-access-token")
        assertTrue(resetSuccess)
        assertTrue(repo.getPinStatus().hasPin)

        // Old PIN should fail, new PIN should succeed
        val failOld = repo.verifyPin("1111")
        assertFalse(failOld is PinVerifyResult.Success)

        val successNew = repo.verifyPin("9999")
        assertTrue(successNew is PinVerifyResult.Success)
    }

    @Test
    fun duplicateProfileNameIsHandledIdempotentlyAndLocalStorageDeduplicates() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val sessionPersistence = MemorySessionPersistence()
        val accountRepo = AccountRepository(sessionPersistence)
        val transport = FakeProfileTransport()
        val api = SupabaseProfileApi(transport)
        val repo = ProfileRepository(persistence, accountRepo, pinStore, api)

        val child1 = HouseholdProfile(
            id = "child-1",
            name = "Sofia",
            kind = ProfileKind.CHILD,
            maximumAge = 10
        )
        val saved1 = repo.saveProfile(child1)
        assertEquals("child-1", saved1.id)

        // Attempting to save another profile with the exact same name (even with different ID)
        // returns the existing profile and does not add a duplicate
        val child2 = HouseholdProfile(
            id = "child-2",
            name = "sofia", // case-insensitive duplicate
            kind = ProfileKind.CHILD,
            maximumAge = 10
        )
        val saved2 = repo.saveProfile(child2)
        assertEquals("child-1", saved2.id)
        assertEquals(2, repo.profiles.value.size) // Default Adult + Sofia

        // Verify local storage deduplication on load
        val dupJson = org.json.JSONArray()
            .put(HouseholdProfile.defaultAdult().encode())
            .put(child1.encode())
            .put(child2.encode())
            .toString()
        val persistence2 = MemoryProfilePersistence().apply {
            saveProfilesJson(dupJson)
        }
        val repo2 = ProfileRepository(persistence2, accountRepo, pinStore, api)
        assertEquals(2, repo2.profiles.value.size)
        assertEquals("Sofia", repo2.profiles.value[1].name)
    }

    @Test
    fun hideAdultContentCannotBeTurnedOffForChildProfile() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val accountRepo = AccountRepository(MemorySessionPersistence())
        val repo = ProfileRepository(persistence, accountRepo, pinStore)

        val child = HouseholdProfile(
            name = "Kiddo",
            kind = ProfileKind.CHILD,
            hideAdultContent = true,
            maximumAge = 10
        )
        repo.saveProfile(child)

        val result = repo.setProfileHideAdultContent(child.id, false)
        assertFalse(result)
        val storedChild = repo.profiles.value.find { it.id == child.id }!!
        assertTrue(storedChild.hideAdultContent)
    }

    @Test
    fun hideAdultContentCanBeToggledForAdultProfile() = runBlocking {
        val persistence = MemoryProfilePersistence()
        val pinStore = ParentalPinStore(MemoryPinPersistence())
        val accountRepo = AccountRepository(MemorySessionPersistence())
        val repo = ProfileRepository(persistence, accountRepo, pinStore)

        val adult = repo.activeProfile.value
        assertFalse(adult.hideAdultContent)

        // Toggling on succeeds
        val resultOn = repo.setProfileHideAdultContent(adult.id, true)
        assertTrue(resultOn)
        assertTrue(repo.activeProfile.value.hideAdultContent)

        // Toggling off succeeds
        val resultOff = repo.setProfileHideAdultContent(adult.id, false)
        assertTrue(resultOff)
        assertFalse(repo.activeProfile.value.hideAdultContent)
    }
}

package com.fortresstv.player.profile

import android.content.Context
import com.fortresstv.player.account.AccountRepository
import com.fortresstv.player.account.AccountSession
import com.fortresstv.player.account.AccountState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

internal interface ProfilePersistence {
    fun loadProfilesJson(): String?
    fun saveProfilesJson(json: String)
    fun loadActiveProfileId(): String?
    fun saveActiveProfileId(id: String)
    fun loadActiveSessionToken(): String?
    fun saveActiveSessionToken(token: String?)
    fun loadPendingAdultRevocation(): Boolean
    fun savePendingAdultRevocation(pending: Boolean)
    fun loadPendingAdultRevocationSessions(): Set<String>
    fun addPendingAdultRevocationSession(authSessionId: String)
    fun removePendingAdultRevocationSession(authSessionId: String)
    fun clearPendingAdultRevocationSessions()
    fun switchNamespace(namespace: String)
}

internal class SharedPrefsProfilePersistence(
    private val context: Context,
    initialNamespace: String = "local"
) : ProfilePersistence {
    private var namespace: String = sanitize(initialNamespace)
    private var preferences = getPrefs(namespace)

    private fun sanitize(raw: String): String = raw.replace(Regex("[^a-zA-Z0-9_-]"), "_")

    private fun getPrefs(ns: String) =
        context.getSharedPreferences("fortress_household_profiles_$ns", Context.MODE_PRIVATE)

    override fun switchNamespace(namespace: String) {
        this.namespace = sanitize(namespace)
        preferences = getPrefs(this.namespace)
    }

    override fun loadProfilesJson(): String? = preferences.getString("profiles_json", null)
    override fun saveProfilesJson(json: String) { preferences.edit().putString("profiles_json", json).apply() }
    override fun loadActiveProfileId(): String? = preferences.getString("active_profile_id", null)
    override fun saveActiveProfileId(id: String) { preferences.edit().putString("active_profile_id", id).apply() }
    override fun loadActiveSessionToken(): String? = preferences.getString("active_session_token", null)
    override fun saveActiveSessionToken(token: String?) {
        if (token != null) preferences.edit().putString("active_session_token", token).apply()
        else preferences.edit().remove("active_session_token").apply()
    }
    override fun loadPendingAdultRevocationSessions(): Set<String> {
        return preferences.getStringSet("pending_adult_revocation_sessions", null) ?: emptySet()
    }
    override fun addPendingAdultRevocationSession(authSessionId: String) {
        if (authSessionId.isBlank()) return
        val current = preferences.getStringSet("pending_adult_revocation_sessions", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(authSessionId)
        preferences.edit()
            .putStringSet("pending_adult_revocation_sessions", current)
            .putBoolean("pending_adult_revocation", true)
            .apply()
    }
    override fun removePendingAdultRevocationSession(authSessionId: String) {
        val current = preferences.getStringSet("pending_adult_revocation_sessions", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.remove(authSessionId)
        val editor = preferences.edit()
        if (current.isEmpty()) {
            editor.remove("pending_adult_revocation_sessions")
            editor.remove("pending_adult_revocation")
        } else {
            editor.putStringSet("pending_adult_revocation_sessions", current)
            editor.putBoolean("pending_adult_revocation", true)
        }
        editor.apply()
    }
    override fun clearPendingAdultRevocationSessions() {
        preferences.edit()
            .remove("pending_adult_revocation_sessions")
            .remove("pending_adult_revocation")
            .apply()
    }
    override fun loadPendingAdultRevocation(): Boolean {
        val sessions = preferences.getStringSet("pending_adult_revocation_sessions", null)
        if (sessions != null && sessions.isNotEmpty()) return true
        return preferences.getBoolean("pending_adult_revocation", false)
    }
    override fun savePendingAdultRevocation(pending: Boolean) {
        if (pending) {
            preferences.edit().putBoolean("pending_adult_revocation", true).apply()
        } else {
            clearPendingAdultRevocationSessions()
        }
    }
}

class ProfileRepository internal constructor(
    private val persistence: ProfilePersistence,
    private val accountRepository: AccountRepository,
    private val pinStore: ParentalPinStore,
    private val api: SupabaseProfileApi = SupabaseProfileApi()
) {
    fun currentUnlockToken(): String? = pinStore.currentUnlockToken()
    constructor(context: Context) : this(
        persistence = SharedPrefsProfilePersistence(
            context.applicationContext,
            AccountRepository.get(context).currentUserId() ?: "local"
        ),
        accountRepository = AccountRepository.get(context),
        pinStore = ParentalPinStore(
            context.applicationContext,
            AccountRepository.get(context).currentUserId() ?: "local"
        )
    )

    companion object {
        @Volatile
        private var shared: ProfileRepository? = null

        fun get(context: Context): ProfileRepository = shared ?: synchronized(this) {
            shared ?: ProfileRepository(context.applicationContext).also { shared = it }
        }
    }

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _profiles = MutableStateFlow<List<HouseholdProfile>>(emptyList())
    val profiles: StateFlow<List<HouseholdProfile>> = _profiles.asStateFlow()

    // Initialize with a safe restricted profile so unrestricted content is never exposed on startup
    private val _activeProfile = MutableStateFlow(HouseholdProfile.restrictedDefault())
    val activeProfile: StateFlow<HouseholdProfile> = _activeProfile.asStateFlow()

    private var currentNamespace: String = accountRepository.currentUserId() ?: "local"

    init {
        accountRepository.setDepartureHandler { departingSession ->
            onDepartingSession(departingSession)
        }
        loadFromLocalStorage()
    }

    internal fun onDepartingSession(departingSession: AccountSession) {
        val departingNs = departingSession.userId
        val isCurrent = (currentNamespace == departingNs)

        pinStore.revokeAdultUnlock()

        val departingAuthSessionId = departingSession.authSessionId
        if (isCurrent) {
            persistence.saveActiveSessionToken(null)
            if (departingAuthSessionId.isNotBlank()) {
                persistence.addPendingAdultRevocationSession(departingAuthSessionId)
            } else {
                persistence.savePendingAdultRevocation(true)
            }
        }

        val success = runCatching {
            api.leaveAdultMode(
                accessToken = departingSession.accessToken,
                targetAuthSessionId = departingAuthSessionId.takeIf { it.isNotBlank() }
            )
        }.getOrDefault(false)

        if (success && isCurrent) {
            if (departingAuthSessionId.isNotBlank()) {
                persistence.removePendingAdultRevocationSession(departingAuthSessionId)
            } else {
                persistence.savePendingAdultRevocation(false)
            }
        }
    }

    suspend fun prepareAccountDeparture() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val departingUserId = currentNamespace.takeIf { it != "local" } ?: accountRepository.currentUserId() ?: return@withLock
            val token = accountRepository.getValidAccessToken()
            val authSessionId = accountRepository.currentAuthSessionId()
            pinStore.revokeAdultUnlock()
            persistence.saveActiveSessionToken(null)
            if (authSessionId != null && authSessionId.isNotBlank()) {
                persistence.addPendingAdultRevocationSession(authSessionId)
            } else {
                persistence.savePendingAdultRevocation(true)
            }
            if (token != null) {
                val success = runCatching {
                    api.leaveAdultMode(
                        accessToken = token,
                        targetAuthSessionId = authSessionId?.takeIf { it.isNotBlank() }
                    )
                }.getOrDefault(false)
                if (success) {
                    if (authSessionId != null && authSessionId.isNotBlank()) {
                        persistence.removePendingAdultRevocationSession(authSessionId)
                    } else {
                        persistence.savePendingAdultRevocation(false)
                    }
                }
            }
        }
    }

    suspend fun completePendingAdultRevocation(explicitToken: String? = null): Boolean {
        if (!persistence.loadPendingAdultRevocation()) return true
        val token = explicitToken ?: accountRepository.getValidAccessToken() ?: return false
        val pendingSessions = persistence.loadPendingAdultRevocationSessions()

        if (pendingSessions.isNotEmpty()) {
            for (targetSessionId in pendingSessions) {
                val left = runCatching {
                    api.leaveAdultMode(accessToken = token, targetAuthSessionId = targetSessionId)
                }.getOrDefault(false)
                if (left) {
                    persistence.removePendingAdultRevocationSession(targetSessionId)
                } else {
                    return false
                }
            }
        } else {
            val currentSessionId = accountRepository.currentAuthSessionId()
            val left = runCatching {
                api.leaveAdultMode(accessToken = token, targetAuthSessionId = currentSessionId)
            }.getOrDefault(false)
            if (left) {
                persistence.savePendingAdultRevocation(false)
            } else {
                return false
            }
        }

        if (persistence.loadPendingAdultRevocation()) {
            return false
        }

        val currentActive = _activeProfile.value
        if (currentActive.id != "restricted_default") {
            runCatching {
                val sessionObj = api.openProfileSession(
                    profileId = currentActive.id,
                    unlockToken = pinStore.currentUnlockToken(),
                    accessToken = token
                )
                val activeToken = sessionObj.optString("session_token").takeIf(String::isNotBlank)
                if (activeToken != null) {
                    persistence.saveActiveSessionToken(activeToken)
                }
            }
        }
        return true
    }

    fun isAdultUnlocked(): Boolean = pinStore.isAdultUnlocked()

    fun revokeAdultUnlock() {
        pinStore.revokeAdultUnlock()
    }

    fun getPinStatus(): PinStatus = pinStore.getStatus()

    fun getActiveSessionToken(): String? = persistence.loadActiveSessionToken()

    suspend fun getOrOpenActiveSessionToken(): String? = withContext(Dispatchers.IO) {
        val existing = persistence.loadActiveSessionToken()
        if (!existing.isNullOrBlank()) {
            return@withContext existing
        }
        val token = accountRepository.getValidAccessToken() ?: return@withContext null
        val active = _activeProfile.value
        val targetProfileId = if (active.id != "restricted_default") {
            active.id
        } else {
            persistence.loadActiveProfileId() ?: _profiles.value.firstOrNull { it.isAdult }?.id
        } ?: return@withContext null

        val sessionObj = runCatching {
            api.openProfileSession(
                profileId = targetProfileId,
                unlockToken = pinStore.currentUnlockToken(),
                accessToken = token
            )
        }.getOrNull()
        val newToken = sessionObj?.optString("session_token")?.takeIf(String::isNotBlank)
        if (newToken != null) {
            persistence.saveActiveSessionToken(newToken)
        }
        newToken
    }

    fun getLastActiveProfileId(): String? = persistence.loadActiveProfileId()

    suspend fun onAccountChanged(userId: String?) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val ns = userId ?: "local"
            currentNamespace = ns
            pinStore.switchNamespace(ns)
            persistence.switchNamespace(ns)
            persistence.saveActiveSessionToken(null)
            _profiles.value = emptyList()
            _activeProfile.value = HouseholdProfile.restrictedDefault()
            loadFromLocalStorage()
        }
    }

    suspend fun refresh(): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val token = accountRepository.getValidAccessToken()
            if (token != null) {
                if (persistence.loadPendingAdultRevocation()) {
                    val completed = completePendingAdultRevocation(token)
                    if (!completed) {
                        loadFromLocalStorage()
                        return@withLock
                    }
                }
                try {
                    val remoteProfiles = api.getProfiles(token)
                    val status = api.getPinStatus(token)
                    pinStore.syncServerStatus(
                        hasPin = status.hasPin,
                        isLocked = status.isLocked,
                        remainingAttempts = status.remainingAttempts,
                        lockoutSeconds = status.lockoutSeconds
                    )

                    if (remoteProfiles.isNotEmpty()) {
                        val distinctRemote = remoteProfiles.distinctBy { it.name.trim().lowercase() }
                        val duplicateIds = remoteProfiles.filterNot { distinctRemote.contains(it) }.map { it.id }
                        if (duplicateIds.isNotEmpty()) {
                            val unlockToken = pinStore.currentUnlockToken()
                            for (dupId in duplicateIds) {
                                runCatching { api.deleteProfile(dupId, unlockToken, token) }
                            }
                        }
                        _profiles.value = distinctRemote
                        saveProfilesLocally(distinctRemote)
                        updateActiveProfileSelection(distinctRemote)
                    } else {
                        // User has no profiles in Supabase yet.
                        // Create their initial adult profile automatically via RPC
                        val initial = HouseholdProfile.defaultAdult()
                        val newId = runCatching {
                            api.createProfile(
                                name = initial.name,
                                kind = initial.kind,
                                language = initial.language,
                                hideAdultContent = initial.hideAdultContent,
                                maximumAge = initial.maximumAge,
                                blockUnrated = initial.blockUnrated,
                                unlockToken = null,
                                accessToken = token
                            )
                        }.getOrNull()

                        val list = if (newId != null) {
                            listOf(initial.copy(id = newId))
                        } else {
                            listOf(initial)
                        }
                        _profiles.value = list
                        saveProfilesLocally(list)
                        updateActiveProfileSelection(list)
                    }

                    val active = _activeProfile.value
                    if (active.id != "restricted_default") {
                        val sessionObj = runCatching {
                            api.openProfileSession(
                                profileId = active.id,
                                unlockToken = pinStore.currentUnlockToken(),
                                accessToken = token
                            )
                        }.getOrNull()
                        val sessionToken = sessionObj?.optString("session_token")?.takeIf(String::isNotBlank)
                        if (sessionToken != null) {
                            persistence.saveActiveSessionToken(sessionToken)
                        }
                    }
                } catch (e: Exception) {
                    // Fall back to local profiles seamlessly when offline or on network failure
                    android.util.Log.w("ProfileRepository", "Failed to refresh remote profiles or PIN status", e)
                    loadFromLocalStorage()
                }
            } else {
                loadFromLocalStorage()
            }
        }
    }

    suspend fun verifyPin(pin: String): PinVerifyResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val token = accountRepository.getValidAccessToken()
            if (token != null) {
                try {
                    val result = api.verifyPin(pin, token)
                    when (result) {
                        is PinVerifyResult.Success -> {
                            pinStore.grantAdultUnlock(result.unlockToken)
                            pinStore.syncServerStatus(hasPin = true, isLocked = false, remainingAttempts = 5, lockoutSeconds = 0)
                        }
                        is PinVerifyResult.Locked -> {
                            pinStore.syncServerStatus(hasPin = true, isLocked = true, remainingAttempts = 0, lockoutSeconds = result.lockoutSeconds)
                        }
                        is PinVerifyResult.Failed -> {
                            pinStore.syncServerStatus(hasPin = true, isLocked = false, remainingAttempts = result.remainingAttempts, lockoutSeconds = 0)
                        }
                    }
                    result
                } catch (_: Exception) {
                    PinVerifyResult.Failed(0)
                }
            } else {
                pinStore.verifyPinLocally(pin)
            }
        }
    }

    suspend fun setPin(newPin: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val token = accountRepository.getValidAccessToken()
            if (token != null && persistence.loadPendingAdultRevocation()) {
                if (!completePendingAdultRevocation(token)) return@withLock false
            }
            val success = if (token != null) {
                val unlockToken = pinStore.currentUnlockToken()
                val apiSuccess = api.setPin(newPin, unlockToken, token)
                if (apiSuccess) {
                    pinStore.setPinLocally(newPin)
                    pinStore.syncServerStatus(hasPin = true, isLocked = false, remainingAttempts = 5, lockoutSeconds = 0)
                }
                apiSuccess
            } else {
                pinStore.setPinLocally(newPin)
                pinStore.syncServerStatus(hasPin = true, isLocked = false, remainingAttempts = 5, lockoutSeconds = 0)
                true
            }
            if (success) {
                pinStore.revokeAdultUnlock()
                persistence.saveActiveSessionToken(null)
            }
            success
        }
    }

    suspend fun removePin(explicitUnlockToken: String? = null): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val token = accountRepository.getValidAccessToken()
            if (token != null && persistence.loadPendingAdultRevocation()) {
                if (!completePendingAdultRevocation(token)) return@withLock false
            }
            val unlockToken = explicitUnlockToken ?: pinStore.currentUnlockToken()
            val success = if (token != null) {
                val apiSuccess = api.removePin(unlockToken, token)
                if (apiSuccess) {
                    pinStore.removePinLocally()
                    pinStore.syncServerStatus(hasPin = false, isLocked = false, remainingAttempts = 5, lockoutSeconds = 0)
                }
                apiSuccess
            } else {
                pinStore.removePinLocally()
                pinStore.syncServerStatus(hasPin = false, isLocked = false, remainingAttempts = 5, lockoutSeconds = 0)
                true
            }
            if (success) {
                pinStore.revokeAdultUnlock()
                persistence.saveActiveSessionToken(null)
            }
            success
        }
    }

    suspend fun resetPinAfterRecovery(newPin: String, recoveryToken: String? = null): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val token = recoveryToken ?: accountRepository.getValidAccessToken()
            val success = if (token != null) {
                val apiSuccess = api.resetPinAfterRecovery(newPin, token)
                if (apiSuccess) {
                    pinStore.setPinLocally(newPin)
                    pinStore.syncServerStatus(hasPin = true, isLocked = false, remainingAttempts = 5, lockoutSeconds = 0)
                }
                apiSuccess
            } else {
                pinStore.setPinLocally(newPin)
                pinStore.syncServerStatus(hasPin = true, isLocked = false, remainingAttempts = 5, lockoutSeconds = 0)
                true
            }
            if (success) {
                pinStore.revokeAdultUnlock()
                persistence.saveActiveSessionToken(null)
            }
            success
        }
    }

    suspend fun selectProfile(profile: HouseholdProfile): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            val pinStatus = pinStore.getStatus()

            var sessionTokenToSave: String? = null

            if (profile.isAdult) {
                if (accountState is AccountState.AuthenticatedOnline && persistence.loadPendingAdultRevocation()) {
                    if (!completePendingAdultRevocation(accountState.accessToken)) {
                        return@withLock false
                    }
                }

                when (accountState) {
                    is AccountState.SignedOutLocal -> {
                        val currentActive = _activeProfile.value
                        if (pinStatus.isProtected && (currentActive.isChild || currentActive.id == "restricted_default") && !pinStore.isAdultUnlocked()) {
                            return@withLock false
                        }
                    }
                    is AccountState.AuthenticatedOnline -> {
                        val currentActive = _activeProfile.value
                        val isTransitioningFromChild = currentActive.isChild || currentActive.id == "restricted_default"

                        val token = try {
                            val sessionObj = api.openProfileSession(
                                profileId = profile.id,
                                unlockToken = pinStore.currentUnlockToken(),
                                accessToken = accountState.accessToken
                            )
                            sessionObj.optString("session_token").takeIf(String::isNotBlank)
                        } catch (_: Exception) {
                            null
                        }

                        if (isTransitioningFromChild && token == null) {
                            return@withLock false
                        }
                        sessionTokenToSave = token
                    }
                    is AccountState.AuthenticatedOffline -> {
                        // In authenticated offline mode, only allow adult activation if confirmed absence
                        // of server PIN, or if a deliberately granted local adult unlock is still valid.
                        val canActivate = (pinStatus.configurationState == PinConfigurationState.CONFIRMED_NO_PIN) ||
                                (pinStore.isAdultUnlocked() && !pinStatus.isLocked)
                        if (!canActivate) {
                            return@withLock false
                        }
                    }
                }
            } else if (profile.isChild) {
                // Entering child mode:
                // 1. Immediately revoke local adult unlock and clear local session token
                pinStore.revokeAdultUnlock()
                persistence.saveActiveSessionToken(null)

                // 2. Invalidate server-side adult authorizations for this Auth session
                if (accountState !is AccountState.SignedOutLocal) {
                    val currentAuthSessionId = accountRepository.currentAuthSessionId()
                    if (!currentAuthSessionId.isNullOrBlank()) {
                        persistence.addPendingAdultRevocationSession(currentAuthSessionId)
                    } else {
                        persistence.savePendingAdultRevocation(true)
                    }

                    if (accountState is AccountState.AuthenticatedOnline) {
                        val leftSuccess = runCatching {
                            api.leaveAdultMode(
                                accessToken = accountState.accessToken,
                                targetAuthSessionId = currentAuthSessionId?.takeIf { it.isNotBlank() }
                            )
                        }.getOrDefault(false)

                        val sessionObj = runCatching {
                            api.openProfileSession(
                                profileId = profile.id,
                                unlockToken = null,
                                accessToken = accountState.accessToken
                            )
                        }.getOrNull()
                        val childToken = sessionObj?.optString("session_token")?.takeIf(String::isNotBlank)
                        if (childToken != null) {
                            sessionTokenToSave = childToken
                        }

                        if (leftSuccess) {
                            if (!currentAuthSessionId.isNullOrBlank()) {
                                persistence.removePendingAdultRevocationSession(currentAuthSessionId)
                            } else {
                                persistence.savePendingAdultRevocation(false)
                            }
                        }
                    }
                }
            }

            // Commit local active selection and session state
            persistence.saveActiveProfileId(profile.id)
            persistence.saveActiveSessionToken(sessionTokenToSave)
            _activeProfile.value = profile
            true
        }
    }

    suspend fun saveProfile(profile: HouseholdProfile, explicitUnlockToken: String? = null): HouseholdProfile = withContext(Dispatchers.IO) {
        mutex.withLock {
            val token = accountRepository.getValidAccessToken()
            if (token != null && persistence.loadPendingAdultRevocation()) {
                if (!completePendingAdultRevocation(token)) {
                    throw IllegalStateException("Required adult revocation cleanup unconfirmed")
                }
            }
            val unlockToken = explicitUnlockToken ?: pinStore.currentUnlockToken()
            val existing = _profiles.value.find { it.id == profile.id }
            val sameNameExisting = _profiles.value.find {
                it.id != profile.id && it.name.trim().equals(profile.name.trim(), ignoreCase = true)
            }
            if (existing == null && sameNameExisting != null) {
                return@withLock sameNameExisting
            }

            val savedProfile = if (token != null) {
                if (existing != null && isUuid(profile.id)) {
                    val updateResult = runCatching {
                        api.updateProfile(profile, unlockToken, token)
                        profile
                    }
                    if (updateResult.isSuccess) {
                        profile
                    } else {
                        val errMsg = updateResult.exceptionOrNull()?.message.orEmpty()
                        if (errMsg.contains("Profile not found")) {
                            val serverId = api.createProfile(
                                name = profile.name,
                                kind = profile.kind,
                                language = profile.language,
                                hideAdultContent = profile.hideAdultContent,
                                maximumAge = profile.maximumAge,
                                blockUnrated = profile.blockUnrated,
                                unlockToken = unlockToken,
                                accessToken = token,
                                avatarUrl = profile.avatarUrl
                            )
                            profile.copy(id = serverId)
                        } else {
                            android.util.Log.w("ProfileRepository", "Remote update failed: " + updateResult.exceptionOrNull()?.message)
                            profile
                        }
                    }
                } else {
                    val serverId = api.createProfile(
                        name = profile.name,
                        kind = profile.kind,
                        language = profile.language,
                        hideAdultContent = profile.hideAdultContent,
                        maximumAge = profile.maximumAge,
                        blockUnrated = profile.blockUnrated,
                        unlockToken = unlockToken,
                        accessToken = token,
                        avatarUrl = profile.avatarUrl
                    )
                    profile.copy(id = serverId)
                }
            } else {
                profile
            }

            val updatedList = _profiles.value.toMutableList().apply {
                val index = indexOfFirst { it.id == profile.id || it.id == savedProfile.id }
                if (index >= 0) set(index, savedProfile) else add(savedProfile)
            }
            _profiles.value = updatedList
            saveProfilesLocally(updatedList)

            if (_activeProfile.value.id == profile.id || _activeProfile.value.id == savedProfile.id) {
                _activeProfile.value = savedProfile
                persistence.saveActiveProfileId(savedProfile.id)
            }
            savedProfile
        }
    }

    suspend fun setProfileHideAdultContent(
        profileId: String,
        hideAdultContent: Boolean,
        explicitUnlockToken: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val target = _profiles.value.find { it.id == profileId } ?: return@withLock false
            if (target.isChild && !hideAdultContent) {
                return@withLock false
            }
            val unlockToken = explicitUnlockToken ?: pinStore.currentUnlockToken()
            val token = accountRepository.getValidAccessToken()
            val updated = target.copy(hideAdultContent = hideAdultContent)

            if (token != null && isUuid(target.id)) {
                val remoteSuccess = runCatching {
                    api.setProfileHideAdultContent(profileId, hideAdultContent, unlockToken, token)
                }.getOrDefault(false)

                if (!remoteSuccess) {
                    runCatching {
                        api.updateProfile(updated, unlockToken, token)
                    }
                }
            }

            val updatedList = _profiles.value.toMutableList().apply {
                val index = indexOfFirst { it.id == profileId }
                if (index >= 0) set(index, updated)
            }
            _profiles.value = updatedList
            saveProfilesLocally(updatedList)

            if (_activeProfile.value.id == profileId) {
                _activeProfile.value = updated
                persistence.saveActiveProfileId(updated.id)
            }
            true
        }
    }

    suspend fun deleteProfile(profileId: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (_profiles.value.size <= 1) return@withLock false
            val token = accountRepository.getValidAccessToken()
            if (token != null && persistence.loadPendingAdultRevocation()) {
                if (!completePendingAdultRevocation(token)) return@withLock false
            }
            val unlockToken = pinStore.currentUnlockToken()

            if (token != null && isUuid(profileId)) {
                runCatching { api.deleteProfile(profileId, unlockToken, token) }
            }

            val updatedList = _profiles.value.filterNot { it.id == profileId }
            _profiles.value = updatedList
            saveProfilesLocally(updatedList)

            if (_activeProfile.value.id == profileId) {
                val next = updatedList.firstOrNull { it.isChild }
                    ?: updatedList.firstOrNull { it.isAdult && (!pinStore.getStatus().isProtected || pinStore.isAdultUnlocked()) }
                    ?: HouseholdProfile.restrictedDefault()
                persistence.saveActiveProfileId(next.id)
                _activeProfile.value = next
            }
            true
        }
    }

    private fun loadFromLocalStorage() {
        val raw = persistence.loadProfilesJson()
        val loaded = if (!raw.isNullOrBlank()) {
            runCatching {
                val array = JSONArray(raw)
                (0 until array.length()).mapNotNull { i ->
                    array.optJSONObject(i)?.let { HouseholdProfile.decode(it) }
                }
            }.getOrDefault(emptyList())
        } else emptyList()

        val distinctLoaded = loaded.distinctBy { it.name.trim().lowercase() }
        val list = if (distinctLoaded.isNotEmpty()) distinctLoaded else {
            val default = HouseholdProfile.defaultAdult()
            listOf(default).also { saveProfilesLocally(it) }
        }
        if (distinctLoaded.size != loaded.size) {
            saveProfilesLocally(list)
        }
        _profiles.value = list
        updateActiveProfileSelection(list)
    }

    private fun updateActiveProfileSelection(availableProfiles: List<HouseholdProfile>) {
        val savedId = persistence.loadActiveProfileId()
        val candidate = availableProfiles.find { it.id == savedId }
        val pinStatus = pinStore.getStatus()
        val accountState = accountRepository.getAccountState()
        val isAuthenticated = accountState !is AccountState.SignedOutLocal
        val isAdultUnlocked = pinStore.isAdultUnlocked()

        val isAdultAuthorized = when {
            isAdultUnlocked -> true
            !isAuthenticated && pinStatus.configurationState == PinConfigurationState.GENUINE_NO_PIN -> true
            isAuthenticated && pinStatus.configurationState == PinConfigurationState.CONFIRMED_NO_PIN -> true
            else -> false
        }

        val previousProfile = _activeProfile.value

        val selected = when {
            candidate != null -> candidate
            else -> {
                // Missing, deleted, corrupt, or unauthenticated:
                if (!isAuthenticated && pinStatus.configurationState == PinConfigurationState.GENUINE_NO_PIN &&
                    availableProfiles.size == 1 && availableProfiles[0].isAdult
                ) {
                    availableProfiles[0]
                } else {
                    if (previousProfile.isChild && previousProfile.id != "restricted_default") {
                        previousProfile
                    } else {
                        availableProfiles.firstOrNull { it.isChild }
                            ?: HouseholdProfile.restrictedDefault()
                    }
                }
            }
        }

        _activeProfile.value = selected
        if (selected.id != "restricted_default" && (candidate == null || candidate.id == selected.id)) {
            persistence.saveActiveProfileId(selected.id)
        }
    }

    private fun saveProfilesLocally(list: List<HouseholdProfile>) {
        val array = JSONArray()
        list.forEach { array.put(it.encode()) }
        persistence.saveProfilesJson(array.toString())
    }

    private fun isUuid(value: String): Boolean = runCatching {
        java.util.UUID.fromString(value)
        true
    }.getOrDefault(false)
}

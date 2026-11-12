package com.fortresstv.player.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface SessionPersistence {
    fun load(): AccountSession?
    fun save(session: AccountSession)
    fun clear()
}

sealed class AccountState {
    object SignedOutLocal : AccountState()
    data class AuthenticatedOnline(val userId: String, val accessToken: String, val authSessionId: String = "") : AccountState()
    data class AuthenticatedOffline(val userId: String) : AccountState()
}

internal class AccountRepository(
    private val persistence: SessionPersistence,
    private val api: SupabaseAuthApi = SupabaseAuthApi()
) {
    companion object {
        @Volatile private var shared: AccountRepository? = null
        fun get(context: android.content.Context): AccountRepository = shared ?: synchronized(this) {
            shared ?: AccountRepository(AccountSessionStore(context.applicationContext)).also { shared = it }
        }
    }
    private val mutex = Mutex()
    private var session: AccountSession? = null
    private var recovery: AccountSession? = null

    @Volatile
    private var departureHandler: ((AccountSession) -> Unit)? = null

    fun setDepartureHandler(handler: ((AccountSession) -> Unit)?) {
        departureHandler = handler
    }

    fun currentUserId(): String? = session?.userId ?: persistence.load()?.also { session = it }?.userId

    fun currentEmail(): String? = session?.email ?: persistence.load()?.also { session = it }?.email

    fun currentAuthSessionId(): String? = session?.authSessionId ?: persistence.load()?.also { session = it }?.authSessionId

    fun getRecoveryAccessToken(): String? = recovery?.accessToken

    fun getAccountState(): AccountState {
        val uid = currentUserId() ?: return AccountState.SignedOutLocal
        val current = session?.takeIf { it.expiresAt > System.currentTimeMillis() / 1000 + 30 }
        val token = current?.accessToken
        return if (token != null) AccountState.AuthenticatedOnline(uid, token, current.authSessionId)
        else AccountState.AuthenticatedOffline(uid)
    }

    suspend fun resolveAccountState(): AccountState {
        val uid = currentUserId() ?: return AccountState.SignedOutLocal
        val token = try { getValidAccessToken() } catch (_: Throwable) { null }
        val authSessionId = currentAuthSessionId().orEmpty()
        return if (token != null) AccountState.AuthenticatedOnline(uid, token, authSessionId)
        else AccountState.AuthenticatedOffline(uid)
    }

    suspend fun getValidAccessToken(): String? = io {
        val current = session ?: persistence.load()?.also { session = it } ?: return@io null
        val now = System.currentTimeMillis() / 1000
        if (current.expiresAt <= now + 30) {
            try {
                accept(api.refresh(current)).accessToken
            } catch (failure: AccountException) {
                if (failure.reason == AccountError.SESSION_EXPIRED) {
                    persistence.clear()
                    session = null
                    null
                } else if (current.expiresAt > now) {
                    current.accessToken
                } else null
            } catch (_: Exception) {
                if (current.expiresAt > now) current.accessToken else null
            }
        } else {
            current.accessToken
        }
    }

    suspend fun restore(): String? = io {
        session = persistence.load()
        session?.takeIf { it.expiresAt <= System.currentTimeMillis() / 1000 + 30 }?.let { previous ->
            try { accept(api.refresh(previous)) }
            catch (failure: AccountException) {
                if (failure.reason == AccountError.SESSION_EXPIRED) {
                    persistence.clear()
                    session = null
                } else if (failure.reason != AccountError.NETWORK) throw failure
            }
        }
        session?.email
    }

    suspend fun signIn(email: String, password: String): String = io {
        accept(api.signIn(email, password)).email
    }

    suspend fun register(email: String, password: String): Boolean = io {
        val result = api.register(email, password)
        if (result != null) accept(result)
        result != null
    }

    suspend fun verifySignup(email: String, code: String): String = io {
        accept(api.verify(email, code, recovery = false)).email
    }

    suspend fun requestRecovery(email: String) = io { recovery = null; api.recover(email) }

    suspend fun resendSignup(email: String) = io { api.resendSignup(email) }

    suspend fun verifyRecovery(email: String, code: String) = io {
        // A recovery session stays in memory and is not shown as a regular login.
        recovery = api.verify(email, code, recovery = true)
    }

    suspend fun resetPassword(password: String) = io {
        val verified = recovery ?: throw AccountException(AccountError.EXPIRED_CODE)
        api.changePassword(verified, password)
        recovery = null
        // A reset usually means the old password may be known to someone else: revoke every
        // session of the account on every device, then require an ordinary login with the new password.
        // The password is already changed, so a failed revoke is retried once rather than surfaced.
        if (runCatching { api.signOut(verified, everywhere = true) }.isFailure) {
            runCatching { api.signOut(verified, everywhere = true) }
        }
        // This device's own stored session for the same account was just revoked too.
        val stored = session ?: persistence.load()
        if (stored != null && stored.userId == verified.userId) {
            runCatching { departureHandler?.invoke(stored) }
            persistence.clear()
            session = null
        }
        Unit
    }

    suspend fun signOut(): Boolean = io {
        val previous = session ?: persistence.load()
        if (previous != null) {
            runCatching { departureHandler?.invoke(previous) }
        }
        persistence.clear()
        session = null
        recovery = null
        if (previous == null) true else runCatching {
            val active = if (previous.expiresAt <= System.currentTimeMillis() / 1000 + 30) api.refresh(previous) else previous
            api.signOut(active)
        }.isSuccess
    }

    fun discardRecovery() { recovery = null }

    private fun accept(value: AccountSession): AccountSession {
        val previous = session ?: persistence.load()
        if (previous != null && previous.userId != value.userId) {
            runCatching { departureHandler?.invoke(previous) }
        }
        persistence.save(value)
        recovery = null
        session = value
        return value
    }

    private suspend fun <T> io(operation: () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock { operation() }
    }
}

package com.fortresstv.player.trakt

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

object TraktAuthBridge {
    private var pendingVerifier: String? = null
    private var pendingState: String? = null
    private var pendingClientId: String? = null
    private var pendingNamespace: String? = null

    private val _authEvents = MutableSharedFlow<TraktAuthResult<TraktUserProfile>>(replay = 0, extraBufferCapacity = 1)
    val authEvents = _authEvents.asSharedFlow()

    fun startAuthorization(
        context: Context,
        clientId: String,
        profileNamespace: String,
        redirectUri: String = TraktEndpoints.REDIRECT_URI
    ) {
        val cleanClientId = clientId.trim().trim('"', '\'')
        val verifier = TraktPkce.generateCodeVerifier(64)
        val challenge = TraktPkce.generateCodeChallenge(verifier)
        val state = UUID.randomUUID().toString()

        pendingVerifier = verifier
        pendingState = state
        pendingClientId = cleanClientId
        pendingNamespace = profileNamespace

        val url = TraktPkce.buildAuthorizeUrl(
            clientId = cleanClientId,
            redirectUri = redirectUri,
            codeChallenge = challenge,
            state = state
        )

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun handleDeepLink(context: Context, uri: Uri): Boolean = handleDeepLink(context, uri.toString())

    fun handleDeepLink(context: Context, urlString: String): Boolean {
        if (!urlString.startsWith("fortresstv://trakt")) return false

        val query = urlString.substringAfter('?', "")
        val params = if (query.isNotBlank()) {
            query.split('&').mapNotNull { part ->
                val eq = part.indexOf('=')
                if (eq > 0) {
                    val key = part.substring(0, eq)
                    val value = java.net.URLDecoder.decode(part.substring(eq + 1), "UTF-8")
                    key to value
                } else null
            }.toMap()
        } else emptyMap()

        val error = params["error"]
        if (!error.isNullOrBlank()) {
            val errorDesc = params["error_description"] ?: error
            _authEvents.tryEmit(TraktAuthResult.Failure("Trakt authorization error: $errorDesc"))
            clear()
            return true
        }

        val code = params["code"]
        if (code.isNullOrBlank()) return false

        val state = params["state"]
        val expectedState = pendingState
        val verifier = pendingVerifier
        val clientId = pendingClientId
        val namespace = pendingNamespace

        if (verifier == null || clientId == null || namespace == null) {
            _authEvents.tryEmit(TraktAuthResult.Failure("No pending Trakt authorization session. Please tap Connect again."))
            clear()
            return true
        }

        if (expectedState != null && state != expectedState) {
            _authEvents.tryEmit(TraktAuthResult.Failure("Security state mismatch. Authorization rejected."))
            clear()
            return true
        }

        CoroutineScope(Dispatchers.IO).launch {
            val client = TraktClient()
            when (val tokenResult = client.exchangeAuthorizationCode(code, verifier, TraktEndpoints.REDIRECT_URI, clientId)) {
                is TraktAuthResult.Success -> {
                    val token = tokenResult.data
                    val profile = when (val p = client.fetchUserProfile(token.accessToken, clientId)) {
                        is TraktAuthResult.Success -> p.data
                        is TraktAuthResult.Failure -> TraktUserProfile(
                            username = "trakt_user",
                            name = "Trakt User",
                            isVip = false,
                            avatarUrl = null
                        )
                    }
                    val store = TraktSettingsStore(context.applicationContext, namespace)
                    store.saveConnection(token, profile)
                    _authEvents.emit(TraktAuthResult.Success(profile))
                }
                is TraktAuthResult.Failure -> {
                    _authEvents.emit(TraktAuthResult.Failure(tokenResult.message, tokenResult.statusCode))
                }
            }
            clear()
        }
        return true
    }

    private fun clear() {
        pendingVerifier = null
        pendingState = null
        pendingClientId = null
        pendingNamespace = null
    }
}

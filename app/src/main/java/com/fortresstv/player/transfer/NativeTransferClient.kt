package com.fortresstv.player.transfer

import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.source.ContentSource
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed interface NativeSendResult {
    data object Success : NativeSendResult
    data class Error(val message: String) : NativeSendResult
}

object NativeTransferClient {
    fun sendSource(
        tv: DiscoveredFortressTv,
        profile: PlaylistProfile,
        pin: String,
        epgUrl: String? = null
    ): NativeSendResult {
        if (tv.protocolVersion != TRANSFER_PROTOCOL_VERSION) return NativeSendResult.Error("Unsupported protocol version")
        val token = tv.sessionToken
            ?: return NativeSendResult.Error("Scan the TV QR code to send source credentials securely")
        val endpoint = tv.endpoint() ?: return NativeSendResult.Error("TV address unavailable")
        val plainPayload = createNativeTransferPayload(profile, epgUrl)
            ?: return NativeSendResult.Error("Local files cannot be sent yet")
        val payload = TransferCrypto.encrypt(plainPayload, token)
        return runCatching {
            val connection = URL("$endpoint/api/source").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 5_000
                connection.readTimeout = 10_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("X-Fortress-Encrypted", "v1")
                applyAuthentication(connection, tv, pin)
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                if (connection.responseCode in 200..299) NativeSendResult.Success
                else NativeSendResult.Error("TV rejected the transfer (${connection.responseCode})")
            } finally {
                connection.disconnect()
            }
        }.getOrElse { NativeSendResult.Error("Could not connect to the TV") }
    }

    private fun applyAuthentication(connection: HttpURLConnection, tv: DiscoveredFortressTv, pin: String) {
        val token = tv.sessionToken
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        else connection.setRequestProperty("X-Fortress-Pin", pin)
    }
}

internal fun createNativeTransferPayload(profile: PlaylistProfile, epgUrl: String? = null): String? =
    when (val source = profile.source) {
            is ContentSource.RemoteUrl -> JSONObject().apply {
                put("protocolVersion", TRANSFER_PROTOCOL_VERSION)
                put("kind", "m3u")
                put("name", profile.name)
                put("url", source.value)
                put("epgUrl", epgUrl?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            }.toString()
            is ContentSource.Xtream -> JSONObject().apply {
                put("protocolVersion", TRANSFER_PROTOCOL_VERSION)
                put("kind", "xtream")
                put("name", profile.name)
                put("url", source.value)
                put("username", source.username)
                put("password", source.password)
                put("epgUrl", epgUrl?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            }.toString()
            is ContentSource.LocalFile -> null
    }

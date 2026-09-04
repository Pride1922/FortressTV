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
    fun sendSource(tv: DiscoveredFortressTv, profile: PlaylistProfile, pin: String): NativeSendResult {
        if (tv.protocolVersion != TRANSFER_PROTOCOL_VERSION) return NativeSendResult.Error("Unsupported protocol version")
        if (!pin.matches(Regex("\\d{6}"))) return NativeSendResult.Error("Invalid pairing code")
        val endpoint = tv.endpoint() ?: return NativeSendResult.Error("TV address unavailable")
        val payload = createNativeTransferPayload(profile)
            ?: return NativeSendResult.Error("Local files cannot be sent yet")
        return runCatching {
            val connection = URL("$endpoint/api/source").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 5_000
                connection.readTimeout = 10_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("X-Fortress-Pin", pin)
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                if (connection.responseCode in 200..299) NativeSendResult.Success
                else NativeSendResult.Error("TV rejected the transfer (${connection.responseCode})")
            } finally {
                connection.disconnect()
            }
        }.getOrElse { NativeSendResult.Error("Could not connect to the TV") }
    }
}

internal fun createNativeTransferPayload(profile: PlaylistProfile): String? =
    when (val source = profile.source) {
            is ContentSource.RemoteUrl -> JSONObject().apply {
                put("protocolVersion", TRANSFER_PROTOCOL_VERSION)
                put("kind", "m3u")
                put("name", profile.name)
                put("url", source.value)
                put("epgUrl", JSONObject.NULL)
            }.toString()
            is ContentSource.Xtream -> JSONObject().apply {
                put("protocolVersion", TRANSFER_PROTOCOL_VERSION)
                put("kind", "xtream")
                put("name", profile.name)
                put("url", source.value)
                put("username", source.username)
                put("password", source.password)
                put("epgUrl", JSONObject.NULL)
            }.toString()
            is ContentSource.LocalFile -> null
    }

package com.fortresstv.player.transfer

import java.net.InetAddress
import java.net.URI
import java.net.URLDecoder

object DirectTransferLink {
    fun parse(raw: String): DiscoveredFortressTv? = runCatching {
        val outer = URI(raw)
        if (outer.scheme != "fortresstv" || outer.host != "send") return null
        val params = outer.rawQuery.orEmpty().split('&').mapNotNull { part ->
            val pieces = part.split('=', limit = 2)
            if (pieces.size != 2) null else pieces[0] to URLDecoder.decode(pieces[1], "UTF-8")
        }.toMap()
        val endpoint = URI(params["endpoint"] ?: return null)
        val token = params["token"] ?: return null
        val backupKey = params["backupKey"]
        if (backupKey != null && !backupKey.matches(Regex("[0-9a-fA-F]{64}"))) return null
        val host = endpoint.host ?: return null
        if (endpoint.scheme != "http" || endpoint.port !in 1..65535) return null
        if (!token.matches(Regex("[0-9a-fA-F]{32}"))) return null
        if (!isPrivateIpv4(host)) return null
        DiscoveredFortressTv(
            serviceName = "FortressTV TV",
            addresses = listOf(InetAddress.getByName(host)),
            port = endpoint.port,
            protocolVersion = TRANSFER_PROTOCOL_VERSION,
            sessionToken = token.lowercase(),
            backupKey = backupKey?.lowercase()
        )
    }.getOrNull()

    private fun isPrivateIpv4(host: String): Boolean {
        val octets = host.split('.').mapNotNull(String::toIntOrNull)
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        return octets[0] == 10 ||
            (octets[0] == 172 && octets[1] in 16..31) ||
            (octets[0] == 192 && octets[1] == 168) ||
            (octets[0] == 169 && octets[1] == 254)
    }
}

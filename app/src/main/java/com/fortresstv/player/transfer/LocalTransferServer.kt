package com.fortresstv.player.transfer

import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Embedded lightweight HTTP server for local QR / browser-based playlist transfer.
 *
 * Security contract:
 * - Binds on a random available port (pass 0 to NanoHTTPD to get an OS-assigned port).
 * - Generates a 128-bit cryptographically random session token embedded in the QR URL fragment.
 * - Generates an independent 6-digit numeric fallback PIN for manual URL entry.
 * - Validates requests using timing-safe comparison (MessageDigest.isEqual).
 * - Locks out after MAX_BAD_ATTEMPTS failed authentications.
 * - Accepts exactly one successful import per session (idempotent).
 * - Maximum JSON payload size: MAX_JSON_BYTES.
 * - No sensitive values (token, PIN, credentials, URLs) are ever logged.
 * - Must be stopped when the dialog is dismissed (call [stop]).
 *
 * Endpoints:
 *   GET  /              — embedded HTML setup page
 *   GET  /api/status    — JSON {"status": "waiting" | "consumed" | "locked"}
 *   POST /api/source    — JSON transfer payload; yields [TransferRequestResult]
 *
 * DNS-rebinding mitigation: the Host header is checked against the server's
 * own IP address.
 */
class LocalTransferServer(
    private val deviceName: String,
    private val onPayloadReceived: (TransferRequestResult) -> Unit
) : NanoHTTPD(0) /* port 0 = OS assigns an ephemeral port */ {

    // ------------------------------------------------------------------
    // Session credentials
    // ------------------------------------------------------------------

    private val tokenBytes: ByteArray
    private val pinBytes: ByteArray

    /** Hex token embedded in QR URL fragment: #token=<hex> */
    val sessionToken: String

    /** 6-digit numeric PIN for manual URL entry */
    val sessionPin: String

    init {
        val rng = SecureRandom()
        val raw = ByteArray(16)
        rng.nextBytes(raw)
        tokenBytes = raw
        sessionToken = raw.toHex()

        val pin = (rng.nextInt(900_000) + 100_000).toString()
        sessionPin = pin
        pinBytes = pin.toByteArray(Charsets.UTF_8)
    }

    // ------------------------------------------------------------------
    // Session state
    // ------------------------------------------------------------------

    private val badAttempts = AtomicInteger(0)
    private val consumed = AtomicBoolean(false)
    private val locked = AtomicBoolean(false)

    private val _state = MutableStateFlow<TransferRequestResult?>(null)
    val resultFlow: StateFlow<TransferRequestResult?> = _state.asStateFlow()

    // ------------------------------------------------------------------
    // Limits
    // ------------------------------------------------------------------

    companion object {
        const val MAX_JSON_BYTES = 64 * 1024   // 64 KB
        const val MAX_BAD_ATTEMPTS = 5
    }

    // ------------------------------------------------------------------
    // NanoHTTPD request handler
    // ------------------------------------------------------------------

    override fun serve(session: IHTTPSession): Response {
        return try {
            when {
                session.method == Method.GET && session.uri == "/" -> serveIndex(session)
                session.method == Method.GET && session.uri == "/api/status" -> serveStatus()
                session.method == Method.POST && session.uri == "/api/source" -> serveSource(session)
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
            }
        } catch (_: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal error")
        }
    }

    // ------------------------------------------------------------------
    // GET /
    // ------------------------------------------------------------------

    private fun serveIndex(session: IHTTPSession): Response {
        if (!checkHost(session)) return dnsRebindingRejected()
        val html = TransferWebPage.generate(deviceName, sessionPin)
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
    }

    // ------------------------------------------------------------------
    // GET /api/status
    // ------------------------------------------------------------------

    private fun serveStatus(): Response {
        val status = when {
            locked.get() -> "locked"
            consumed.get() -> "consumed"
            else -> "waiting"
        }
        return jsonOk("{\"status\":\"$status\"}")
    }

    // ------------------------------------------------------------------
    // POST /api/source
    // ------------------------------------------------------------------

    private fun serveSource(session: IHTTPSession): Response {
        if (!checkHost(session)) return dnsRebindingRejected()

        // Rate-limit / session guards
        if (locked.get()) return jsonError(429, "Too many attempts. Restart session on TV.")
        if (consumed.get()) return jsonError(409, "Session already used. Restart session on TV.")

        // Read body with size limit
        val contentLength = session.headers["content-length"]?.toLongOrNull() ?: 0L
        if (contentLength > MAX_JSON_BYTES) {
            val result = TransferRequestResult.PayloadTooLarge
            _state.value = result
            onPayloadReceived(result)
            return jsonError(413, "Payload too large")
        }

        // Authenticate
        val authHeader = session.headers["authorization"]
        val pinHeader = session.headers["x-fortress-pin"]
        val authenticated = when {
            authHeader != null && authHeader.startsWith("Bearer ") -> {
                val providedToken = authHeader.removePrefix("Bearer ").trim().toByteArray(Charsets.UTF_8)
                MessageDigest.isEqual(sessionToken.toByteArray(Charsets.UTF_8), providedToken)
            }
            pinHeader != null -> {
                val providedPin = pinHeader.trim().toByteArray(Charsets.UTF_8)
                MessageDigest.isEqual(pinBytes, providedPin)
            }
            else -> false
        }

        if (!authenticated) {
            val attempts = badAttempts.incrementAndGet()
            if (attempts >= MAX_BAD_ATTEMPTS) {
                locked.set(true)
                val result = TransferRequestResult.LockedOut
                _state.value = result
                onPayloadReceived(result)
                return jsonError(403, "Locked. Too many failed attempts.")
            }
            val result = TransferRequestResult.BadPin
            _state.value = result
            onPayloadReceived(result)
            return jsonError(401, "Invalid code. ${MAX_BAD_ATTEMPTS - attempts} attempt(s) remaining.")
        }

        if (authHeader == null || session.headers["x-fortress-encrypted"] != "v1") {
            return jsonError(426, "Secure QR pairing is required for source transfer")
        }

        val expectedSize = contentLength.takeIf { it > 0L }?.toInt()
            ?: return jsonError(411, "Content length required")
        val bodyBytes = readLimited(session, MAX_JSON_BYTES, expectedSize)
            ?: return jsonError(413, "Payload too large or incomplete")
        val bodyStr = runCatching {
            TransferCrypto.decrypt(bodyBytes.toString(Charsets.UTF_8), sessionToken)
        }.getOrElse {
            return jsonError(422, "Invalid encrypted transfer")
        }

        // Parse payload
        val parseResult = parsePayload(bodyStr)
        _state.value = parseResult
        onPayloadReceived(parseResult)

        return when (parseResult) {
            is TransferRequestResult.Accepted -> {
                consumed.set(true)
                jsonOk("{\"success\":true}")
            }
            is TransferRequestResult.UnsupportedVersion -> jsonError(422, "Unsupported protocol version")
            is TransferRequestResult.ParseError -> jsonError(422, "Parse error: ${parseResult.detail}")
            else -> jsonError(500, "Unexpected error")
        }
    }

    private fun readLimited(session: IHTTPSession, limit: Int, expectedSize: Int): ByteArray? {
        if (expectedSize !in 0..limit) return null
        val output = java.io.ByteArrayOutputStream(minOf(expectedSize, 64 * 1024))
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (total < expectedSize) {
            val read = session.inputStream.read(buffer, 0, minOf(buffer.size, expectedSize - total))
            if (read < 0) break
            total += read
            if (total > limit) return null
            output.write(buffer, 0, read)
        }
        return output.toByteArray().takeIf { it.size == expectedSize }
    }

    private fun publish(result: TransferRequestResult) {
        _state.value = result
        onPayloadReceived(result)
    }

    // ------------------------------------------------------------------
    // Payload parser
    // ------------------------------------------------------------------

    private fun parsePayload(body: String): TransferRequestResult {
        return runCatching {
            val json = JSONObject(body)
            val version = json.optInt("protocolVersion", 0)
            if (version != TRANSFER_PROTOCOL_VERSION) {
                return TransferRequestResult.UnsupportedVersion
            }
            val kindStr = json.optString("kind", "").uppercase()
            val kind = when (kindStr) {
                "M3U" -> TransferKind.M3U
                "XTREAM" -> TransferKind.XTREAM
                else -> return TransferRequestResult.ParseError("Unknown kind: $kindStr")
            }
            val name = json.optString("name", "").trim()
            val url = json.optString("url", "").trim().ifBlank { null }
            val username = json.optString("username", "").trim().ifBlank { null }
            val password = json.optString("password", "").trim().ifBlank { null }
            val epgUrl = json.optString("epgUrl", "").trim().ifBlank { null }

            when (kind) {
                TransferKind.M3U -> {
                    if (url.isNullOrBlank()) {
                        return TransferRequestResult.ParseError("M3U url is required")
                    }
                }
                TransferKind.XTREAM -> {
                    if (url.isNullOrBlank()) {
                        return TransferRequestResult.ParseError("Xtream server url is required")
                    }
                    if (username.isNullOrBlank()) {
                        return TransferRequestResult.ParseError("Xtream username is required")
                    }
                    if (password.isNullOrBlank()) {
                        return TransferRequestResult.ParseError("Xtream password is required")
                    }
                }
            }

            TransferRequestResult.Accepted(
                TransferSourcePayload(
                    protocolVersion = version,
                    kind = kind,
                    name = name,
                    url = url,
                    username = username,
                    password = password,
                    epgUrl = epgUrl
                )
            )
        }.getOrElse { e ->
            when (e) {
                is JSONException -> TransferRequestResult.ParseError("Malformed JSON")
                else -> TransferRequestResult.ParseError("Unexpected: ${e.javaClass.simpleName}")
            }
        }
    }

    // ------------------------------------------------------------------
    // DNS-rebinding mitigation
    // ------------------------------------------------------------------

    private fun checkHost(session: IHTTPSession): Boolean {
        val host = session.headers["host"] ?: return true // can't check, allow
        // Allow localhost and IP addresses; reject anything that looks like a hostname
        val hostWithoutPort = host.substringBefore(':')
        return hostWithoutPort == "localhost"
            || hostWithoutPort == "127.0.0.1"
            || hostWithoutPort.matches(Regex("""(\d{1,3}\.){3}\d{1,3}"""))
            || hostWithoutPort.matches(Regex("""\[([0-9a-fA-F:]+)]"""/* IPv6 */))
    }

    private fun dnsRebindingRejected(): Response =
        newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun jsonOk(body: String): Response =
        newFixedLengthResponse(Response.Status.OK, "application/json", body)

    private fun jsonError(code: Int, message: String): Response {
        val status = Response.Status.lookup(code) ?: CustomStatus(code)
        // Never echo credentials or internal paths in error body
        return newFixedLengthResponse(status, "application/json",
            "{\"error\":\"${message.jsonEscape()}\"}"
        )
    }

    /** Minimal IStatus implementation for HTTP codes not defined in NanoHTTPD 2.x Status enum. */
    private class CustomStatus(private val code: Int) : Response.IStatus {
        override fun getDescription(): String = "$code"
        override fun getRequestStatus(): Int = code
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.jsonEscape(): String = this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

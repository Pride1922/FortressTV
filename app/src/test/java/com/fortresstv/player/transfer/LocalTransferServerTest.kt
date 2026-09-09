package com.fortresstv.player.transfer

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.Socket

/**
 * Unit tests for [LocalTransferServer].
 *
 * These tests start a real embedded server on a random port and make HTTP
 * requests against it. No Android runtime is required.
 */
class LocalTransferServerTest {

    private lateinit var server: LocalTransferServer
    private var receivedResult: TransferRequestResult? = null

    @Before
    fun setUp() {
        receivedResult = null
        server = LocalTransferServer("Test TV") { result ->
            receivedResult = result
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private val baseUrl get() = "http://127.0.0.1:${server.listeningPort}"

    private fun get(path: String): Pair<Int, String> {
        val conn = URL("$baseUrl$path").openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5_000
        conn.readTimeout = 5_000
        val code = conn.responseCode
        val body = runCatching {
            (if (code in 200..299) conn.inputStream else conn.errorStream)
                .bufferedReader().readText()
        }.getOrElse { "" }
        conn.disconnect()
        return code to body
    }

    private fun post(
        path: String,
        body: String,
        token: String? = null,
        pin: String? = null,
        host: String? = null
    ): Pair<Int, String> {
        val conn = URL("$baseUrl$path").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
        if (pin != null) conn.setRequestProperty("X-Fortress-Pin", pin)
        if (host != null) conn.setRequestProperty("Host", host)
        conn.connectTimeout = 5_000
        conn.readTimeout = 5_000
        val requestBody = if (token != null) {
            conn.setRequestProperty("X-Fortress-Encrypted", "v1")
            TransferCrypto.encrypt(body, token)
        } else body
        val bytes = requestBody.toByteArray(Charsets.UTF_8)
        conn.setRequestProperty("Content-Length", bytes.size.toString())
        val out: OutputStream = conn.outputStream
        out.write(bytes)
        out.flush()
        val code = conn.responseCode
        val responseBody = runCatching {
            (if (code in 200..299) conn.inputStream else conn.errorStream)
                .bufferedReader().readText()
        }.getOrElse { "" }
        conn.disconnect()
        return code to responseBody
    }

    private fun validM3uPayload(name: String = "Test") = """
        {"protocolVersion":$TRANSFER_PROTOCOL_VERSION,"kind":"m3u","name":"$name","url":"http://example.com/pl.m3u","epgUrl":null}
    """.trimIndent()

    private fun validXtreamPayload() = """
        {"protocolVersion":$TRANSFER_PROTOCOL_VERSION,"kind":"xtream","name":"Sports","url":"http://srv.example.com","username":"user","password":"pass","epgUrl":null}
    """.trimIndent()

    private fun postBytes(path: String, bytes: ByteArray, pin: String): Pair<Int, String> {
        val conn = URL("$baseUrl$path").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setFixedLengthStreamingMode(bytes.size)
        conn.setRequestProperty("Content-Type", "application/octet-stream")
        conn.setRequestProperty("X-Fortress-Pin", pin)
        conn.setRequestProperty("X-Fortress-Filename", "backup.ftv")
        conn.outputStream.use { it.write(bytes) }
        val code = conn.responseCode
        val body = runCatching {
            (if (code in 200..299) conn.inputStream else conn.errorStream).bufferedReader().readText()
        }.getOrElse { "" }
        conn.disconnect()
        return code to body
    }

    // ----------------------------------------------------------------
    // Tests — index page
    // ----------------------------------------------------------------

    @Test
    fun indexPageReturnsHtml() {
        val (code, body) = get("/")
        assertEquals(200, code)
        assertTrue("Expected HTML", body.contains("<html", ignoreCase = true))
    }

    // ----------------------------------------------------------------
    // Tests — status endpoint
    // ----------------------------------------------------------------

    @Test
    fun statusInitiallyWaiting() {
        val (code, body) = get("/api/status")
        assertEquals(200, code)
        assertTrue(body.contains("waiting"))
    }

    // ----------------------------------------------------------------
    // Tests — valid session token accepted
    // ----------------------------------------------------------------

    @Test
    fun validTokenAcceptsM3uPayload() {
        val (code, body) = post("/api/source", validM3uPayload(), token = server.sessionToken)
        assertEquals(200, code)
        assertTrue(body.contains("true"))
        assertTrue(receivedResult is TransferRequestResult.Accepted)
        val accepted = receivedResult as TransferRequestResult.Accepted
        assertEquals(TransferKind.M3U, accepted.payload.kind)
        assertEquals("http://example.com/pl.m3u", accepted.payload.url)
    }

    @Test
    fun validTokenAcceptsXtreamPayload() {
        val (code, _) = post("/api/source", validXtreamPayload(), token = server.sessionToken)
        assertEquals(200, code)
        val accepted = receivedResult as TransferRequestResult.Accepted
        assertEquals(TransferKind.XTREAM, accepted.payload.kind)
        assertEquals("user", accepted.payload.username)
    }

    // ----------------------------------------------------------------
    // Tests — valid 6-digit PIN accepted
    // ----------------------------------------------------------------

    @Test
    fun pinOnlySourceTransferIsRejectedBecauseItCannotProvideConfidentiality() {
        val (code, body) = post("/api/source", validM3uPayload(), pin = server.sessionPin)
        assertEquals(426, code)
        assertTrue(body.contains("Secure QR pairing"))
    }

    @Test
    fun validPinAcceptsEncryptedBackup() {
        val bytes = byteArrayOf(0x46, 0x54, 0x56, 0x42, 0x01, 1, 2, 3)
        val (code, _) = postBytes("/api/backup", bytes, server.sessionPin)
        assertEquals(200, code)
        val accepted = receivedResult as TransferRequestResult.AcceptedBackup
        assertEquals("backup.ftv", accepted.payload.fileName)
        assertTrue(bytes.contentEquals(accepted.payload.bytes))
    }

    @Test
    fun invalidBackupHeaderIsRejected() {
        val (code, _) = postBytes("/api/backup", "not a backup".toByteArray(), server.sessionPin)
        assertEquals(422, code)
        assertTrue(receivedResult is TransferRequestResult.ParseError)
    }

    @Test
    fun oversizedBackupIsRejectedBeforeReadingBody() {
        Socket("127.0.0.1", server.listeningPort).use { socket ->
            socket.soTimeout = 5_000
            socket.getOutputStream().bufferedWriter().apply {
                write("POST /api/backup HTTP/1.1\r\n")
                write("Host: 127.0.0.1:${server.listeningPort}\r\n")
                write("X-Fortress-Pin: ${server.sessionPin}\r\n")
                write("Content-Length: ${LocalTransferServer.MAX_BACKUP_BYTES + 1}\r\n\r\n")
                flush()
            }
            val statusLine = socket.getInputStream().bufferedReader().readLine()
            assertTrue(statusLine.contains("413"))
        }
    }

    // ----------------------------------------------------------------
    // Tests — invalid credentials
    // ----------------------------------------------------------------

    @Test
    fun wrongTokenReturns401() {
        val (code, _) = post("/api/source", validM3uPayload(), token = "0".repeat(32))
        assertEquals(401, code)
        assertTrue(receivedResult is TransferRequestResult.BadPin)
    }

    @Test
    fun wrongPinReturns401() {
        val (code, _) = post("/api/source", validM3uPayload(), pin = "000000")
        assertEquals(401, code)
    }

    @Test
    fun noCredentialsReturns401() {
        val (code, _) = post("/api/source", validM3uPayload())
        assertEquals(401, code)
    }

    // ----------------------------------------------------------------
    // Tests — lockout after max bad attempts
    // ----------------------------------------------------------------

    @Test
    fun locksOutAfterMaxBadAttempts() {
        // Make MAX_BAD_ATTEMPTS wrong PIN submissions
        repeat(LocalTransferServer.MAX_BAD_ATTEMPTS - 1) {
            post("/api/source", validM3uPayload(), pin = "000000")
        }
        // Final wrong attempt should lock
        val (code, body) = post("/api/source", validM3uPayload(), pin = "000000")
        assertEquals(403, code)
        assertTrue(body.contains("ocked", ignoreCase = true))
        assertTrue(receivedResult is TransferRequestResult.LockedOut)
    }

    @Test
    fun afterLockoutValidTokenAlsoRejected() {
        repeat(LocalTransferServer.MAX_BAD_ATTEMPTS) {
            post("/api/source", validM3uPayload(), pin = "000000")
        }
        // Even valid token should now be rejected (session is locked)
        val (code, _) = post("/api/source", validM3uPayload(), token = server.sessionToken)
        assertEquals(429, code)
    }

    // ----------------------------------------------------------------
    // Tests — session consumed after first success (replay prevention)
    // ----------------------------------------------------------------

    @Test
    fun secondSubmissionAfterSuccessRejected() {
        post("/api/source", validM3uPayload(), token = server.sessionToken)
        // Second attempt — same token
        val (code, _) = post("/api/source", validM3uPayload(), token = server.sessionToken)
        assertEquals(409, code)
    }

    // ----------------------------------------------------------------
    // Tests — oversized payload
    // ----------------------------------------------------------------

    @Test
    fun oversizedPayloadRejected() {
        val giant = "x".repeat(LocalTransferServer.MAX_JSON_BYTES + 1)
        val (code, _) = post("/api/source", """{"data":"$giant"}""", token = server.sessionToken)
        assertEquals(413, code)
    }

    // ----------------------------------------------------------------
    // Tests — malformed JSON
    // ----------------------------------------------------------------

    @Test
    fun malformedJsonReturns422() {
        val (code, _) = post("/api/source", "not json at all!!!", token = server.sessionToken)
        assertEquals(422, code)
        assertTrue(receivedResult is TransferRequestResult.ParseError)
    }

    // ----------------------------------------------------------------
    // Tests — unsupported protocol version
    // ----------------------------------------------------------------

    @Test
    fun unsupportedVersionReturns422() {
        val payload = """{"protocolVersion":99,"kind":"m3u","name":"X","url":"http://x.com/p.m3u","epgUrl":null}"""
        val (code, _) = post("/api/source", payload, token = server.sessionToken)
        assertEquals(422, code)
        assertTrue(receivedResult is TransferRequestResult.UnsupportedVersion)
    }

    // ----------------------------------------------------------------
    // Tests — missing required fields
    // ----------------------------------------------------------------

    @Test
    fun m3uMissingUrlReturns422() {
        val payload = """{"protocolVersion":$TRANSFER_PROTOCOL_VERSION,"kind":"m3u","name":"X","url":"","epgUrl":null}"""
        val (code, _) = post("/api/source", payload, token = server.sessionToken)
        assertEquals(422, code)
    }

    @Test
    fun xtreamMissingUsernameReturns422() {
        val payload = """{"protocolVersion":$TRANSFER_PROTOCOL_VERSION,"kind":"xtream","name":"X","url":"http://srv.com","username":"","password":"pass","epgUrl":null}"""
        val (code, _) = post("/api/source", payload, token = server.sessionToken)
        assertEquals(422, code)
    }

    // ----------------------------------------------------------------
    // Tests — unknown source kind
    // ----------------------------------------------------------------

    @Test
    fun unknownKindReturns422() {
        val payload = """{"protocolVersion":$TRANSFER_PROTOCOL_VERSION,"kind":"torrent","name":"X","url":"magnet:?xt=...","epgUrl":null}"""
        val (code, _) = post("/api/source", payload, token = server.sessionToken)
        assertEquals(422, code)
    }

    // ----------------------------------------------------------------
    // Tests — status reflects consumed
    // ----------------------------------------------------------------

    @Test
    fun statusConsumedAfterSuccessfulTransfer() {
        post("/api/source", validM3uPayload(), token = server.sessionToken)
        val (code, body) = get("/api/status")
        assertEquals(200, code)
        assertTrue(body.contains("consumed"))
    }

    // ----------------------------------------------------------------
    // Tests — sanitizeHostForDisplay helper
    // ----------------------------------------------------------------

    @Test
    fun sanitizeStripsBearerCredentials() {
        assertEquals("stream.example.com:8080", sanitizeHostForDisplay("http://user:pass@stream.example.com:8080/live/channel"))
    }

    @Test
    fun sanitizeStripsPath() {
        assertEquals("example.com", sanitizeHostForDisplay("https://example.com/api/player?token=abc"))
    }

    @Test
    fun sanitizeHandlesNullInput() {
        assertEquals("", sanitizeHostForDisplay(null))
    }

    @Test
    fun sanitizeHandlesBlankInput() {
        assertEquals("", sanitizeHostForDisplay("   "))
    }
}

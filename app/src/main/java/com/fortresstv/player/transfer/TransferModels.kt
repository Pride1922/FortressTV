package com.fortresstv.player.transfer

import com.fortresstv.player.playlist.PlaylistProfile

// ---------------------------------------------------------------------------
// Protocol version
// ---------------------------------------------------------------------------

const val TRANSFER_PROTOCOL_VERSION = 2

// ---------------------------------------------------------------------------
// Incoming payload from phone/browser
// ---------------------------------------------------------------------------

enum class TransferKind { M3U, XTREAM }

data class TransferSourcePayload(
    val protocolVersion: Int,
    val kind: TransferKind,
    val name: String,
    val url: String?,
    val username: String?,
    val password: String?,
    val epgUrl: String?
)

// ---------------------------------------------------------------------------
// Preview shown to TV user before confirming import
// ---------------------------------------------------------------------------

data class TransferPreview(
    val sourceName: String,
    val kind: TransferKind,
    /** Sanitized: host only, no credentials, no tokens */
    val sanitizedHost: String,
    val payload: TransferSourcePayload
)

data class TransferBackupPayload(
    val fileName: String,
    val bytes: ByteArray,
    val pairingPassword: String? = null
)

// ---------------------------------------------------------------------------
// Session lifecycle
// ---------------------------------------------------------------------------

sealed interface TransferSessionState {
    /** Awaiting incoming connection. Shows QR and PIN. */
    data object Waiting : TransferSessionState

    /** Received payload — awaiting TV user confirmation. */
    data class PendingConfirmation(val preview: TransferPreview) : TransferSessionState

    /** Received encrypted backup — awaiting explicit TV confirmation. */
    data class PendingBackup(val payload: TransferBackupPayload) : TransferSessionState

    /** TV user confirmed — source was saved. */
    data class Success(val profile: PlaylistProfile) : TransferSessionState

    /** Terminal error (locked out, timed out, parse failure, etc.) */
    data class Error(val message: String) : TransferSessionState
}

// ---------------------------------------------------------------------------
// Server result from individual HTTP requests (internal)
// ---------------------------------------------------------------------------

sealed interface TransferRequestResult {
    data class Accepted(val payload: TransferSourcePayload) : TransferRequestResult
    data class AcceptedBackup(val payload: TransferBackupPayload) : TransferRequestResult
    data object BadPin : TransferRequestResult
    data object LockedOut : TransferRequestResult
    data object Expired : TransferRequestResult
    data object AlreadyConsumed : TransferRequestResult
    data class ParseError(val detail: String) : TransferRequestResult
    data object PayloadTooLarge : TransferRequestResult
    data object UnsupportedVersion : TransferRequestResult
}

// ---------------------------------------------------------------------------
// Helper — extract sanitized display host from a URL (no credentials exposed)
// ---------------------------------------------------------------------------

fun sanitizeHostForDisplay(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return runCatching {
        val cleaned = raw.trim()
            .removePrefix("http://")
            .removePrefix("https://")
        // Strip any embedded user:password@
        val afterAt = cleaned.substringAfter('@', cleaned)
        // Take only host:port, no path
        afterAt.substringBefore('/').substringBefore('?').substringBefore('#')
    }.getOrElse { "" }
}

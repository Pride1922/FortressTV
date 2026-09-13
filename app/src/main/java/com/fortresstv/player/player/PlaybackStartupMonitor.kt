package com.fortresstv.player.player

import kotlinx.coroutines.delay

internal class PlaybackNetworkTracker<T>(
    private var network: T? = null,
    private var internet: Boolean = false,
    private var validated: Boolean = false
) {
    fun update(next: T, hasInternet: Boolean, isValidated: Boolean): Boolean {
        val recovered = hasInternet && (next != network || !internet || (!validated && isValidated))
        network = next
        internet = hasInternet
        validated = isValidated
        return recovered
    }

    fun lost(lost: T) {
        if (network == lost) {
            network = null
            internet = false
            validated = false
        }
    }
}

internal data class BufferingSample(
    val positionMs: Long,
    val bufferedMs: Long,
    val bytes: Long,
    val networkAvailable: Boolean,
    val recoveryAttempts: Int
)

/** Keep observing partial startup progress until playback exits buffering or needs recovery. */
internal suspend fun awaitBufferingStall(
    intervalMs: Long,
    sample: () -> BufferingSample?,
    wait: suspend (Long) -> Unit = { delay(it) }
): StallDecision? {
    var previous = sample() ?: return null
    while (true) {
        wait(intervalMs)
        val current = sample() ?: return null
        val decision = StallRecoveryPolicy.decide(
            positionProgressMs = current.positionMs - previous.positionMs,
            bufferProgressMs = current.bufferedMs - previous.bufferedMs,
            recoveryAttempts = current.recoveryAttempts,
            networkAvailable = current.networkAvailable,
            bytesProgress = current.bytes - previous.bytes
        )
        if (decision != StallDecision.WAIT) return decision
        previous = current
    }
}

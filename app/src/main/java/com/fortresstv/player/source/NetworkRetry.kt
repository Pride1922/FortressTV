package com.fortresstv.player.source

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException

internal class HttpStatusException(val statusCode: Int) : IOException("Server returned HTTP $statusCode")

internal suspend fun <T> retryTransientNetwork(
    maxAttempts: Int = 3,
    initialDelayMillis: Long = 500L,
    block: suspend () -> T
): T {
    require(maxAttempts > 0)
    var lastFailure: Throwable? = null
    repeat(maxAttempts) { attempt ->
        try {
            return block()
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            if (!failure.isTransientNetworkFailure() || attempt == maxAttempts - 1) throw failure
            lastFailure = failure
            delay(initialDelayMillis * (attempt + 1))
        }
    }
    throw lastFailure ?: IllegalStateException("Network request failed")
}

private fun Throwable.isTransientNetworkFailure(): Boolean = when (this) {
    is HttpStatusException -> statusCode == 408 || statusCode == 425 || statusCode == 429 || statusCode in 500..599
    is IOException -> true
    else -> false
}

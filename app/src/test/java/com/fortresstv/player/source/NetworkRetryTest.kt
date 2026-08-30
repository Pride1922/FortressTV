package com.fortresstv.player.source

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class NetworkRetryTest {
    @Test
    fun retriesTransientFailuresUpToSuccess() = runBlocking {
        var attempts = 0

        val result = retryTransientNetwork(initialDelayMillis = 0) {
            attempts++
            if (attempts < 3) throw IOException("offline")
            "loaded"
        }

        assertEquals("loaded", result)
        assertEquals(3, attempts)
    }

    @Test
    fun doesNotRetryConfigurationFailures() {
        var attempts = 0

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                retryTransientNetwork(initialDelayMillis = 0) {
                    attempts++
                    throw IllegalArgumentException("invalid credentials")
                }
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun retriesServerErrorsButNotMissingResources() {
        var serverAttempts = 0
        assertThrows(HttpStatusException::class.java) {
            runBlocking {
                retryTransientNetwork(initialDelayMillis = 0) {
                    serverAttempts++
                    throw HttpStatusException(503)
                }
            }
        }
        assertEquals(3, serverAttempts)

        var missingAttempts = 0
        assertThrows(HttpStatusException::class.java) {
            runBlocking {
                retryTransientNetwork(initialDelayMillis = 0) {
                    missingAttempts++
                    throw HttpStatusException(404)
                }
            }
        }
        assertEquals(1, missingAttempts)
    }
}

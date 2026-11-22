package com.fortresstv.player.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateManagerTest {

    private val sampleVersionJson = """
        {
          "version": "2.0.0-RC5",
          "versionCode": 78,
          "notes": "Test release",
          "downloadUrl": "https://example.com/fortresstv.apk",
          "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        }
    """.trimIndent()

    @Test
    fun parseUpdateInfoAllowsSameVersionOnlyWhenRequested() {
        assertNull(UpdateManager.parseUpdateInfo(sampleVersionJson, currentVersionCode = 78, allowSameVersion = false))

        val sameVersion = UpdateManager.parseUpdateInfo(sampleVersionJson, currentVersionCode = 78, allowSameVersion = true)
        assertNotNull(sameVersion)
        assertEquals(78, sameVersion?.versionCode)
        assertEquals("2.0.0-RC5", sameVersion?.versionName)
    }

    @Test
    fun parseUpdateInfoRejectsOlderVersionEvenWhenSameVersionAllowed() {
        assertNull(UpdateManager.parseUpdateInfo(sampleVersionJson, currentVersionCode = 79, allowSameVersion = true))
    }

    @Test
    fun parseUpdateInfoReturnsNewerVersionNormally() {
        val newer = UpdateManager.parseUpdateInfo(sampleVersionJson, currentVersionCode = 77, allowSameVersion = false)
        assertNotNull(newer)
        assertEquals(78, newer?.versionCode)
    }
}

package com.fortresstv.player.ui

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFormFactorTest {

    @Test
    fun detectsTelevisionFromConfiguration() {
        val config = Configuration().apply {
            uiMode = Configuration.UI_MODE_TYPE_TELEVISION or Configuration.UI_MODE_NIGHT_YES
        }
        val formFactor = FormFactorDetector.detectFromConfiguration(config)
        assertEquals(AppFormFactor.TELEVISION, formFactor)
        assertTrue(formFactor.isTelevision)
        assertFalse(formFactor.isHandheld)
    }

    @Test
    fun detectsHandheldFromConfiguration() {
        val config = Configuration().apply {
            uiMode = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_YES
        }
        val formFactor = FormFactorDetector.detectFromConfiguration(config)
        assertEquals(AppFormFactor.HANDHELD, formFactor)
        assertFalse(formFactor.isTelevision)
        assertTrue(formFactor.isHandheld)
    }

    @Test
    fun detectsHandheldFromDeskConfiguration() {
        val config = Configuration().apply {
            uiMode = Configuration.UI_MODE_TYPE_DESK or Configuration.UI_MODE_NIGHT_NO
        }
        val formFactor = FormFactorDetector.detectFromConfiguration(config)
        assertEquals(AppFormFactor.HANDHELD, formFactor)
    }
}

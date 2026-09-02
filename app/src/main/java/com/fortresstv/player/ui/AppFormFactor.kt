package com.fortresstv.player.ui

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppFormFactor {
    HANDHELD,
    TELEVISION;

    val isTelevision: Boolean get() = this == TELEVISION
    val isHandheld: Boolean get() = this == HANDHELD
}

object FormFactorDetector {
    fun detect(context: Context): AppFormFactor {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            return AppFormFactor.TELEVISION
        }
        val config = context.resources.configuration
        return detectFromConfiguration(config)
    }

    fun detectFromConfiguration(configuration: Configuration): AppFormFactor {
        return if ((configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION) {
            AppFormFactor.TELEVISION
        } else {
            AppFormFactor.HANDHELD
        }
    }
}

val LocalFormFactor = staticCompositionLocalOf { AppFormFactor.HANDHELD }

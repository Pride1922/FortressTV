package com.fortresstv.player.introdb

import android.content.Context
import android.content.SharedPreferences

class TheIntroDbSettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): TheIntroDbSettings {
        return TheIntroDbSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            autoSkipIntro = prefs.getBoolean(KEY_AUTO_SKIP_INTRO, false),
            autoSkipRecap = prefs.getBoolean(KEY_AUTO_SKIP_RECAP, false)
        )
    }

    fun saveSettings(settings: TheIntroDbSettings) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putBoolean(KEY_AUTO_SKIP_INTRO, settings.autoSkipIntro)
            .putBoolean(KEY_AUTO_SKIP_RECAP, settings.autoSkipRecap)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "fortresstv_introdb_settings"
        private const val KEY_ENABLED = "introdb_enabled"
        private const val KEY_AUTO_SKIP_INTRO = "introdb_auto_skip_intro"
        private const val KEY_AUTO_SKIP_RECAP = "introdb_auto_skip_recap"
    }
}

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

// Producer module for the Baseline Profile of :app.
//
// It is separate from :macrobenchmark on purpose: the Baseline Profile plugin only keeps the
// `nonMinifiedRelease` and `benchmarkRelease` variants of the test module it is applied to, which
// would switch off the `benchmark` variant that the existing benchmark commands build.
android {
    namespace = "com.fortresstv.player.baselineprofile"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        // Collecting a Baseline Profile needs Android 9 (API 28) or newer.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

// No Gradle managed device is configured: the generator runs on the device or emulator adb sees
// (the plugin's default, useConnectedDevices = true).

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
}

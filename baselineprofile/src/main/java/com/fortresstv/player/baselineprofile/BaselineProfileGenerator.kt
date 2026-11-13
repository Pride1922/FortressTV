package com.fortresstv.player.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records which classes and methods FortressTV uses from a cold start to the start screen and,
 * when a library is configured, while opening Home and scrolling its shelves.
 *
 * On a device without a source or sign-in (a fresh emulator) the app stops at the welcome screen.
 * That is not an error here: the start-up part of the profile is still recorded and the Home part
 * is left out. A plain early return is used instead of `assumeTrue`, because a failed assumption
 * inside [BaselineProfileRule.collect] would throw the whole profile away, start-up included.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true
    ) {
        // Cold start. collect() kills the app process before every iteration.
        pressHome()
        startActivityAndWait()

        val startupReady = device.wait(
            Until.findObject(By.res("startup_destination_ready")),
            STARTUP_TIMEOUT_MS
        ) != null
        if (!startupReady) {
            // No source or sign-in: welcome screen. Keep the start-up profile, skip Home.
            device.waitForIdle()
            return@collect
        }

        // Home and its shelves, when the library offers them. Nothing here may fail the run.
        runCatching {
            device.findObject(By.res("library_destination_home"))?.click()
            val shelves = device.wait(Until.findObject(By.res("home_shelves")), HOME_TIMEOUT_MS)
            if (shelves != null && shelves.isScrollable) {
                shelves.setGestureMargin(device.displayWidth / 5)
                shelves.fling(Direction.DOWN)
                device.findObject(By.res("home_shelves"))?.fling(Direction.UP)
            }
        }
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE_NAME = "com.fortresstv.player"

        // A configured device shows the start screen long before this. Without a source the
        // wait runs out on every iteration, so it is the cost of a run on a fresh emulator.
        const val STARTUP_TIMEOUT_MS = 60_000L
        const val HOME_TIMEOUT_MS = 30_000L
    }
}

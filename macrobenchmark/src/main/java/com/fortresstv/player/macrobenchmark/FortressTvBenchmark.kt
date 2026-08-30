package com.fortresstv.player.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FortressTvBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.None(),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
    }

    @Test
    fun warmStartup() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
    }

    @Test
    fun channelListFling() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            assertNotNull(
                "Configure a playlist before running channelListFling",
                device.wait(Until.findObject(By.res("channel_list")), UI_TIMEOUT)
            )
        }
    ) {
        val list = device.findObject(By.res("channel_list"))
        list.setGestureMargin(device.displayWidth / 5)
        list.fling(Direction.DOWN)
        device.waitForIdle()
    }

    @Test
    fun programmeGuideFling() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            val guideButton = device.wait(
                Until.findObject(By.res("programme_guide_button")),
                UI_TIMEOUT
            )
            assertNotNull("Configure a playlist and EPG before running programmeGuideFling", guideButton)
            guideButton.click()
            assertNotNull(
                "The programme guide did not become ready",
                device.wait(Until.findObject(By.res("epg_ready")), UI_TIMEOUT)
            )
        }
    ) {
        val grid = device.findObject(By.res("epg_grid"))
        grid.setGestureMargin(device.displayWidth / 5)
        grid.fling(Direction.DOWN)
        device.waitForIdle()
    }

    @Test
    fun programmeGuideOpen() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            ensureChannelBrowser()
            assertNotNull(
                "Configure a playlist and EPG before running programmeGuideOpen",
                device.wait(Until.findObject(By.res("programme_guide_button")), UI_TIMEOUT)
            )
        }
    ) {
        device.findObject(By.res("programme_guide_button")).click()
        assertNotNull(
            "The programme guide did not become ready",
            device.wait(Until.findObject(By.res("epg_ready")), UI_TIMEOUT)
        )
        device.waitForIdle()
    }

    @Test
    fun programmeGuideVerticalStress() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = { openProgrammeGuide() }
    ) {
        val grid = device.findObject(By.res("epg_grid"))
        grid.setGestureMargin(device.displayWidth / 5)
        repeat(2) {
            grid.fling(Direction.DOWN)
            grid.fling(Direction.UP)
        }
        device.waitForIdle()
    }

    @Test
    fun programmeGuideHorizontalStress() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = { openProgrammeGuide() }
    ) {
        val grid = device.findObject(By.res("epg_grid"))
        grid.setGestureMargin(device.displayWidth / 5)
        repeat(2) {
            grid.fling(Direction.LEFT)
            grid.fling(Direction.RIGHT)
        }
        device.waitForIdle()
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.openProgrammeGuide() {
        pressHome()
        startActivityAndWait()
        ensureChannelBrowser()
        val guideButton = device.wait(
            Until.findObject(By.res("programme_guide_button")),
            UI_TIMEOUT
        )
        assertNotNull("Configure a playlist and EPG before running the EPG benchmarks", guideButton)
        guideButton.click()
        assertNotNull(
            "The programme guide did not become ready",
            device.wait(Until.findObject(By.res("epg_ready")), UI_TIMEOUT)
        )
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.ensureChannelBrowser() {
        if (device.findObject(By.res("programme_guide_button")) == null &&
            device.findObject(By.res("epg_grid")) != null
        ) {
            device.pressBack()
            device.wait(Until.findObject(By.res("programme_guide_button")), UI_TIMEOUT)
        }
    }

    private companion object {
        const val PACKAGE_NAME = "com.fortresstv.player"
        const val UI_TIMEOUT = 30_000L
    }
}

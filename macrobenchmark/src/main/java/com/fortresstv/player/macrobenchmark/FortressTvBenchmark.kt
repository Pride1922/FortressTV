package com.fortresstv.player.macrobenchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
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
        waitForStartupDestination()
    }

    /**
     * Cold start with the Baseline Profile packaged in the APK compiled, as on a user's device
     * once the profile is installed. Compare with [coldStartup], which compiles nothing, to see
     * what the profile is worth. Fails if the APK contains no Baseline Profile.
     */
    @Test
    fun coldStartupBaselineProfile() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
        waitForStartupDestination()
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
        waitForStartupDestination()
    }

    private fun MacrobenchmarkScope.waitForStartupDestination() {
        assertNotNull(
            "The configured startup destination did not become ready",
            device.wait(Until.findObject(By.res("startup_destination_ready")), UI_TIMEOUT)
        )
    }

    @Test
    fun homeShelvesFling() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            waitForStartupDestination()
            device.findObject(By.res("library_destination_home"))?.click()
            val shelves = device.wait(Until.findObject(By.res("home_shelves")), UI_TIMEOUT)
            assertNotNull("Home shelves did not become available", shelves)
            assumeTrue("Populate enough Home shelves to scroll", shelves?.isScrollable == true)
        }
    ) {
        val shelves = device.findObject(By.res("home_shelves"))
        shelves.setGestureMargin(device.displayWidth / 5)
        shelves.fling(Direction.DOWN)
        shelves.fling(Direction.UP)
        device.waitForIdle()
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
    fun moviesOpen() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = { startLibraryForCatalogBenchmark("library_destination_movies") }
    ) {
        openCatalogDestination("library_destination_movies", "movies_grid")
    }

    @Test
    fun moviesGridFling() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            startLibraryForCatalogBenchmark("library_destination_movies")
            openCatalogDestination("library_destination_movies", "movies_grid")
            assumeScrollableCatalogGrid("movies_grid")
        }
    ) {
        flingCatalogGrid("movies_grid")
    }

    @Test
    fun seriesOpen() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = { startLibraryForCatalogBenchmark("library_destination_series") }
    ) {
        openCatalogDestination("library_destination_series", "series_grid")
    }

    @Test
    fun seriesGridFling() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            startLibraryForCatalogBenchmark("library_destination_series")
            openCatalogDestination("library_destination_series", "series_grid")
            assumeScrollableCatalogGrid("series_grid")
        }
    ) {
        flingCatalogGrid("series_grid")
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
        if (device.findObject(By.res("programme_guide_button")) == null) {
            if (device.findObject(By.res("library_destination_live_tv")) == null) {
                device.findObject(By.res("library_destination_home"))?.click()
            }
            val liveTvDestination = device.wait(
                Until.findObject(By.res("library_destination_live_tv")),
                UI_TIMEOUT
            )
            assertNotNull("The Live TV destination was not available", liveTvDestination)
            liveTvDestination.click()
            device.wait(Until.findObject(By.res("programme_guide_button")), UI_TIMEOUT)
        }
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.startLibraryForCatalogBenchmark(
        destinationTag: String
    ) {
        pressHome()
        startActivityAndWait()
        if (device.findObject(By.res(destinationTag)) == null) {
            device.wait(Until.findObject(By.res("library_destination_home")), UI_TIMEOUT)?.click()
        }
        assertNotNull(
            "Configure an enabled Xtream source before running catalogue benchmarks",
            device.wait(Until.findObject(By.res(destinationTag)), UI_TIMEOUT)
        )
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.openCatalogDestination(
        destinationTag: String,
        gridTag: String
    ) {
        device.findObject(By.res(destinationTag)).click()
        assertNotNull(
            "The configured Xtream catalogue did not produce a $gridTag",
            device.wait(Until.findObject(By.res(gridTag)), UI_TIMEOUT)
        )
        device.waitForIdle()
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.flingCatalogGrid(gridTag: String) {
        val grid = device.findObject(By.res(gridTag))
        grid.setGestureMargin(device.displayWidth / 5)
        repeat(2) {
            grid.fling(Direction.DOWN)
            grid.fling(Direction.UP)
        }
        device.waitForIdle()
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.assumeScrollableCatalogGrid(gridTag: String) {
        assumeTrue(
            "$gridTag needs enough catalogue items to scroll",
            device.findObject(By.res(gridTag)).isScrollable
        )
    }

    private companion object {
        const val PACKAGE_NAME = "com.fortresstv.player"
        const val UI_TIMEOUT = 120_000L
    }
}

package com.fortresstv.player.source

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupDestinationTest {
    @Test
    fun defaultFallbackIsHome() {
        assertEquals(StartupDestination.HOME, StartupDestination.fromPersisted(null))
        assertEquals(StartupDestination.HOME, StartupDestination.fromPersisted(""))
        assertEquals(StartupDestination.HOME, StartupDestination.fromPersisted("invalid"))
    }

    @Test
    fun parsesKnownValues() {
        assertEquals(StartupDestination.HOME, StartupDestination.fromPersisted("home"))
        assertEquals(StartupDestination.LIVE_TV, StartupDestination.fromPersisted("live_tv"))
    }

    @Test
    fun persistedValuesAreStable() {
        assertEquals("home", StartupDestination.HOME.persistedValue)
        assertEquals("live_tv", StartupDestination.LIVE_TV.persistedValue)
    }
}

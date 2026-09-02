package com.fortresstv.player.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationFocusCoordinatorTest {

    @Test
    fun initialZoneIsSidebarByDefault() {
        val coordinator = NavigationFocusCoordinator()
        assertEquals(NavigationZone.SIDEBAR, coordinator.currentZone)
        assertNull(coordinator.previousZone)
    }

    @Test
    fun transitionTracksPreviousZone() {
        val coordinator = NavigationFocusCoordinator(NavigationZone.SIDEBAR)
        coordinator.transitionTo(NavigationZone.CONTENT)

        assertEquals(NavigationZone.CONTENT, coordinator.currentZone)
        assertEquals(NavigationZone.SIDEBAR, coordinator.previousZone)
    }

    @Test
    fun backFromContentReturnsToSidebar() {
        val coordinator = NavigationFocusCoordinator(NavigationZone.SIDEBAR)
        coordinator.transitionTo(NavigationZone.CONTENT)

        val handled = coordinator.handleBack()
        assertTrue(handled)
        assertEquals(NavigationZone.SIDEBAR, coordinator.currentZone)
    }

    @Test
    fun backFromSidebarIsNotHandledInternally() {
        val coordinator = NavigationFocusCoordinator(NavigationZone.SIDEBAR)

        val handled = coordinator.handleBack()
        assertFalse(handled)
        assertEquals(NavigationZone.SIDEBAR, coordinator.currentZone)
    }

    @Test
    fun backFromModalReturnsToContent() {
        val coordinator = NavigationFocusCoordinator(NavigationZone.SIDEBAR)
        coordinator.transitionTo(NavigationZone.CONTENT)
        coordinator.transitionTo(NavigationZone.MODAL)

        assertEquals(NavigationZone.MODAL, coordinator.currentZone)
        assertEquals(NavigationZone.CONTENT, coordinator.previousZone)

        val handled = coordinator.handleBack()
        assertTrue(handled)
        assertEquals(NavigationZone.CONTENT, coordinator.currentZone)
    }
}

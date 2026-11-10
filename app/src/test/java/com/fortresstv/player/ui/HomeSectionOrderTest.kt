package com.fortresstv.player.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSectionOrderTest {
    @Test
    fun existingProfilesStartWithTheDefaultOrder() {
        assertEquals(HomeSectionId.defaultOrder, HomeSectionId.ordered(emptyList()))
    }

    @Test
    fun savedOrderWinsAndMissingSectionsAreAppended() {
        val saved = listOf(HomeSectionId.SPORTS, HomeSectionId.CONTINUE_WATCHING, "old_section", HomeSectionId.SPORTS)
        val expected = listOf(HomeSectionId.SPORTS, HomeSectionId.CONTINUE_WATCHING) +
            HomeSectionId.defaultOrder.filterNot { it in saved }

        assertEquals(expected, HomeSectionId.ordered(saved))
    }
}

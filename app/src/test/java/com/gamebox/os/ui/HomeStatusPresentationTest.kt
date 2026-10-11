package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeStatusPresentationTest {
    @Test
    fun networkStatusUsesDistinctHealthyAttentionAndUnknownStates() {
        assertEquals(HomeStatusTone.HEALTHY, homeStatusTone("Network", "Internet connected"))
        assertEquals(HomeStatusTone.HEALTHY, homeStatusTone("Network", "Local network"))
        assertEquals(HomeStatusTone.ATTENTION, homeStatusTone("Network", "Offline"))
        assertEquals(HomeStatusTone.ATTENTION, homeStatusTone("Network", "Not connected"))
        assertEquals(HomeStatusTone.ATTENTION, homeStatusTone("Network", "Disconnected"))
        assertNull(homeStatusTone("Network", "Checking…"))
    }

    @Test
    fun controllerStatusHighlightsPresenceWithoutTreatingOtherLabelsAsStates() {
        assertEquals(HomeStatusTone.HEALTHY, homeStatusTone("Controller", "Wireless Controller"))
        assertEquals(HomeStatusTone.ATTENTION, homeStatusTone("Controller", "Not connected"))
        assertNull(homeStatusTone("Storage", "4% used"))
    }
}

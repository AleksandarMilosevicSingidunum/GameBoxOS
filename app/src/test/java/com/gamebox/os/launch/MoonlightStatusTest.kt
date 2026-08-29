package com.gamebox.os.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class MoonlightStatusTest {
    @Test fun connectivityDistinguishesOfflineLanAndOtherNetwork() {
        assertEquals(MoonlightConnectivity.OFFLINE, classifyMoonlightConnectivity(false, false))
        assertEquals(MoonlightConnectivity.LOCAL_NETWORK, classifyMoonlightConnectivity(true, true))
        assertEquals(MoonlightConnectivity.INTERNET, classifyMoonlightConnectivity(true, false))
    }

    @Test fun recentSessionsAreNewestFirstDeduplicatedAndBounded() {
        assertEquals(
            listOf("Halo", "Celeste", "Hades"),
            addRecentMoonlightSession(listOf("Celeste", "Halo", "Hades"), " Halo ", 3)
        )
    }

    @Test fun blankSessionDoesNotChangeHistory() {
        assertEquals(
            listOf("Existing"),
            addRecentMoonlightSession(listOf("Existing"), "  ", 5)
        )
    }
}

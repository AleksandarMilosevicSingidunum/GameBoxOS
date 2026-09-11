package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerActionRegistryTest {
    @Test fun configuredActionsExposeTruthfulLabelsAndDispatch() {
        val registry = ControllerActionRegistry()
        var searches = 0
        var filters = 0

        registry.configure("Search", { searches++ }, "Filters", { filters++ })

        assertEquals("Search", registry.xLabel)
        assertEquals("Filters", registry.yLabel)
        assertTrue(registry.invokeX())
        assertTrue(registry.invokeY())
        assertEquals(1, searches)
        assertEquals(1, filters)
    }

    @Test fun clearRestoresGlobalFallbackWithoutDispatchingStaleActions() {
        val registry = ControllerActionRegistry()
        registry.configure("Search", {}, "Filters", {})

        registry.clear()

        assertEquals("Store", registry.xLabel)
        assertEquals("Settings", registry.yLabel)
        assertFalse(registry.invokeX())
        assertFalse(registry.invokeY())
    }
}

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
        assertFalse(registry.invokeBack())
    }

    @Test fun disposingOutgoingRouteCannotClearIncomingRouteActions() {
        val registry = ControllerActionRegistry()
        var incomingActionCalls = 0
        val outgoingLease = registry.configure("Search", {}, "Filters", {})
        val incomingLease = registry.configure("Next section", {}, "Previous section", {
            incomingActionCalls++
        })

        registry.clear(outgoingLease)

        assertEquals("Next section", registry.xLabel)
        assertEquals("Previous section", registry.yLabel)
        assertTrue(registry.invokeY())
        assertEquals(1, incomingActionCalls)

        registry.clear(incomingLease)
        assertEquals("Store", registry.xLabel)
        assertEquals("Settings", registry.yLabel)
    }

    @Test fun activeTextInputDismissesBeforeTheGlobalBackAction() {
        val registry = ControllerActionRegistry()
        var editorOpen = true
        var globalBackCalls = 0
        registry.configure(
            xLabel = "Search",
            onX = {},
            yLabel = "Filters",
            onY = {},
            onBack = {
                if (editorOpen) {
                    editorOpen = false
                    true
                } else false
            },
        )

        assertTrue(registry.dispatchBack { globalBackCalls++; true })
        assertFalse(editorOpen)
        assertEquals(0, globalBackCalls)

        assertTrue(registry.dispatchBack { globalBackCalls++; true })
        assertEquals(1, globalBackCalls)
    }
}

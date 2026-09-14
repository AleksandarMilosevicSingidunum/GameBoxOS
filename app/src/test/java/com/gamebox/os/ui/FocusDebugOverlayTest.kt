package com.gamebox.os.ui

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusDebugRegistryTest {
    @Test
    fun neighborSelectsNearestDirectionalControl() {
        val registry = FocusDebugRegistry()
        val center = registry.allocateId()
        val up = registry.allocateId()
        val farUp = registry.allocateId()
        val right = registry.allocateId()

        registry.update(center, Rect(100f, 100f, 200f, 200f))
        registry.update(up, Rect(110f, 10f, 190f, 80f))
        registry.update(farUp, Rect(500f, 0f, 600f, 80f))
        registry.update(right, Rect(220f, 110f, 300f, 190f))
        registry.focus(center, true)

        assertEquals(up, registry.neighbor(FocusDirection.UP))
        assertEquals(right, registry.neighbor(FocusDirection.RIGHT))
        assertNull(registry.neighbor(FocusDirection.LEFT))
        assertNull(registry.neighbor(FocusDirection.DOWN))
    }

    @Test
    fun removingFocusedControlClearsOverlaySelection() {
        val registry = FocusDebugRegistry()
        val id = registry.allocateId()
        registry.update(id, Rect(0f, 0f, 10f, 10f))
        registry.focus(id, true)

        registry.remove(id)

        assertNull(registry.focusedId)
        assertNull(registry.neighbor(FocusDirection.RIGHT))
    }
}

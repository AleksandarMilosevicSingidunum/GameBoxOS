package com.gamebox.os

import com.gamebox.os.launch.ShortcutAvailability
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutStatusTest {
    @Test
    fun availabilityStatesRemainExplicit() {
        assertEquals(ShortcutAvailability.NOT_INSTALLED, ShortcutAvailability.valueOf("NOT_INSTALLED"))
        assertEquals(ShortcutAvailability.LAUNCH_REJECTED, ShortcutAvailability.valueOf("LAUNCH_REJECTED"))
    }
}
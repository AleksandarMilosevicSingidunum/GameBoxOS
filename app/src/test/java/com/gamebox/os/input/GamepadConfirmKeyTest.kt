package com.gamebox.os.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GamepadConfirmKeyTest {
    @Test
    fun mapsGamepadConfirmToViewCenterActivation() {
        assertEquals(
            KeyEvent.KEYCODE_DPAD_CENTER,
            gamepadConfirmKeyCode(KeyEvent.KEYCODE_BUTTON_A),
        )
    }

    @Test
    fun leavesBackAndNavigationKeysUntouched() {
        assertNull(gamepadConfirmKeyCode(KeyEvent.KEYCODE_BUTTON_B))
        assertNull(gamepadConfirmKeyCode(KeyEvent.KEYCODE_DPAD_CENTER))
        assertNull(gamepadConfirmKeyCode(KeyEvent.KEYCODE_DPAD_LEFT))
    }
}

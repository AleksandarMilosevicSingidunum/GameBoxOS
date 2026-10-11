package com.gamebox.os.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GamepadBrowserNavigationTest {
    @Test
    fun controllerConfirmUsesWebKeyboardActivationForFocusedBrowserControls() {
        assertEquals(KeyEvent.KEYCODE_ENTER, gamepadBrowserActivationKeyCode(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(KeyEvent.KEYCODE_DPAD_CENTER, gamepadBrowserActivationKeyCode(KeyEvent.KEYCODE_DPAD_CENTER))
        assertNull(gamepadBrowserActivationKeyCode(KeyEvent.KEYCODE_BUTTON_B))
    }

    @Test
    fun directionsReachWebViewAsSpatialNavigation() {
        assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, gamepadBrowserNavigationKeyCode(KeyEvent.KEYCODE_DPAD_DOWN))
        assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, gamepadBrowserNavigationKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, gamepadBrowserNavigationKeyCode(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, gamepadBrowserNavigationKeyCode(KeyEvent.KEYCODE_DPAD_LEFT))
        assertNull(gamepadBrowserNavigationKeyCode(KeyEvent.KEYCODE_BUTTON_A))

    }
}

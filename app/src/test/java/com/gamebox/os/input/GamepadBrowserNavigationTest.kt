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
    fun directionsMapToForwardAndReverseWebFocus() {
        assertEquals(KeyEvent.KEYCODE_TAB, gamepadBrowserNavigationKeyCode(KeyEvent.KEYCODE_DPAD_DOWN))
        assertEquals(KeyEvent.KEYCODE_TAB, gamepadBrowserNavigationKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(KeyEvent.KEYCODE_TAB, gamepadBrowserNavigationKeyCode(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(KeyEvent.KEYCODE_TAB, gamepadBrowserNavigationKeyCode(KeyEvent.KEYCODE_DPAD_LEFT))
        assertNull(gamepadBrowserNavigationKeyCode(KeyEvent.KEYCODE_BUTTON_A))

        assertEquals(
            KeyEvent.META_SHIFT_ON,
            gamepadBrowserNavigationMetaState(KeyEvent.KEYCODE_DPAD_UP, 0),
        )
        assertEquals(
            KeyEvent.META_SHIFT_ON,
            gamepadBrowserNavigationMetaState(KeyEvent.KEYCODE_DPAD_LEFT, 0),
        )
        assertEquals(0, gamepadBrowserNavigationMetaState(KeyEvent.KEYCODE_DPAD_DOWN, 0))
        assertEquals(0, gamepadBrowserNavigationMetaState(KeyEvent.KEYCODE_DPAD_RIGHT, 0))
    }
}

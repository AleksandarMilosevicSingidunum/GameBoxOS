package com.gamebox.os.ui

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueprintActivationPolicyTest {
    @Test
    fun acceptsControllerConfirmAndKeyboardActivationKeys() {
        assertTrue(isBlueprintActivationKey(KeyEvent.KEYCODE_BUTTON_A))
        assertTrue(isBlueprintActivationKey(KeyEvent.KEYCODE_DPAD_CENTER))
        assertTrue(isBlueprintActivationKey(KeyEvent.KEYCODE_ENTER))
    }

    @Test
    fun rejectsNavigationAndBackKeys() {
        assertFalse(isBlueprintActivationKey(KeyEvent.KEYCODE_DPAD_LEFT))
        assertFalse(isBlueprintActivationKey(KeyEvent.KEYCODE_BACK))
    }

    @Test
    fun heldActivationDoesNotRepeatActions() {
        assertTrue(shouldInvokeBlueprintActivation(KeyEvent.KEYCODE_BUTTON_A, repeatCount = 0))
        assertTrue(shouldInvokeBlueprintActivation(KeyEvent.KEYCODE_DPAD_CENTER, repeatCount = 0))
        assertFalse(shouldInvokeBlueprintActivation(KeyEvent.KEYCODE_BUTTON_A, repeatCount = 1))
        assertFalse(shouldInvokeBlueprintActivation(KeyEvent.KEYCODE_DPAD_CENTER, repeatCount = 2))
        assertFalse(shouldInvokeBlueprintActivation(KeyEvent.KEYCODE_DPAD_LEFT, repeatCount = 0))
    }
}

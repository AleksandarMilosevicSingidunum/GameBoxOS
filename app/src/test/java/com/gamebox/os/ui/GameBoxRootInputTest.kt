package com.gamebox.os.ui

import android.view.KeyEvent
import android.view.SoundEffectConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameBoxRootInputTest {
    @Test
    fun backDismissalUsesOneStablePriorityAcrossOverlaysAndDetails() {
        assertEquals(
            GameBoxBackTarget.QUICK_ACTIONS,
            gameBoxBackTarget(true, true, true, true),
        )
        assertEquals(
            GameBoxBackTarget.SYSTEM_STATUS,
            gameBoxBackTarget(false, true, true, true),
        )
        assertEquals(
            GameBoxBackTarget.PROFILE_SWITCHER,
            gameBoxBackTarget(false, false, true, true),
        )
        assertEquals(
            GameBoxBackTarget.GAME_DETAILS,
            gameBoxBackTarget(false, false, false, true),
        )
        assertEquals(
            GameBoxBackTarget.NONE,
            gameBoxBackTarget(false, false, false, false),
        )
    }

    @Test
    fun mapsControllerButtonsToRootCommands() {
        assertEquals(GameBoxRootInput.MovePrimaryTab(-1), gameBoxRootInputForKey(KeyEvent.KEYCODE_BUTTON_L1))
        assertEquals(GameBoxRootInput.MovePrimaryTab(1), gameBoxRootInputForKey(KeyEvent.KEYCODE_BUTTON_R1))
        assertEquals(GameBoxRootInput.OpenQuickActions, gameBoxRootInputForKey(KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(GameBoxRootInput.OpenQuickActions, gameBoxRootInputForKey(KeyEvent.KEYCODE_MENU))
        assertEquals(GameBoxRootInput.OpenSystemStatus, gameBoxRootInputForKey(KeyEvent.KEYCODE_BUTTON_SELECT))
        assertEquals(GameBoxRootInput.InvokeX, gameBoxRootInputForKey(KeyEvent.KEYCODE_BUTTON_X))
        assertEquals(GameBoxRootInput.InvokeY, gameBoxRootInputForKey(KeyEvent.KEYCODE_BUTTON_Y))
        assertEquals(GameBoxRootInput.Back, gameBoxRootInputForKey(KeyEvent.KEYCODE_BACK))
        assertEquals(GameBoxRootInput.Back, gameBoxRootInputForKey(KeyEvent.KEYCODE_BUTTON_B))
        assertNull(gameBoxRootInputForKey(KeyEvent.KEYCODE_DPAD_CENTER))
    }

    @Test
    fun mapsDpadDirectionsToNavigationSoundsOnly() {
        assertEquals(SoundEffectConstants.NAVIGATION_LEFT, gameBoxNavigationSoundForKey(KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(SoundEffectConstants.NAVIGATION_RIGHT, gameBoxNavigationSoundForKey(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(SoundEffectConstants.NAVIGATION_UP, gameBoxNavigationSoundForKey(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(SoundEffectConstants.NAVIGATION_DOWN, gameBoxNavigationSoundForKey(KeyEvent.KEYCODE_DPAD_DOWN))
        assertNull(gameBoxNavigationSoundForKey(KeyEvent.KEYCODE_BUTTON_X))
    }
}

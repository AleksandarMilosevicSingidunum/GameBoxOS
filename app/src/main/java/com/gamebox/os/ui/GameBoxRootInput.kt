package com.gamebox.os.ui

import android.view.KeyEvent
import android.view.SoundEffectConstants
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

internal sealed interface GameBoxRootInput {
    data class MovePrimaryTab(val offset: Int) : GameBoxRootInput
    data object OpenQuickActions : GameBoxRootInput
    data object OpenSystemStatus : GameBoxRootInput
    data object InvokeX : GameBoxRootInput
    data object InvokeY : GameBoxRootInput
    data object Back : GameBoxRootInput
}

internal enum class GameBoxBackTarget {
    QUICK_ACTIONS,
    SYSTEM_STATUS,
    PROFILE_SWITCHER,
    GAME_DETAILS,
    NONE,
}

/** One dismissal order shared by Android Back and controller B. */
internal fun gameBoxBackTarget(
    showQuickActions: Boolean,
    showSystemStatus: Boolean,
    showProfileSwitcher: Boolean,
    hasSelectedGame: Boolean,
): GameBoxBackTarget = when {
    showQuickActions -> GameBoxBackTarget.QUICK_ACTIONS
    showSystemStatus -> GameBoxBackTarget.SYSTEM_STATUS
    showProfileSwitcher -> GameBoxBackTarget.PROFILE_SWITCHER
    hasSelectedGame -> GameBoxBackTarget.GAME_DETAILS
    else -> GameBoxBackTarget.NONE
}

internal fun gameBoxRootInputForKey(keyCode: Int): GameBoxRootInput? = when (keyCode) {
    KeyEvent.KEYCODE_BUTTON_L1 -> GameBoxRootInput.MovePrimaryTab(-1)
    KeyEvent.KEYCODE_BUTTON_R1 -> GameBoxRootInput.MovePrimaryTab(1)
    KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_MENU -> GameBoxRootInput.OpenQuickActions
    KeyEvent.KEYCODE_BUTTON_SELECT -> GameBoxRootInput.OpenSystemStatus
    KeyEvent.KEYCODE_BUTTON_X -> GameBoxRootInput.InvokeX
    KeyEvent.KEYCODE_BUTTON_Y -> GameBoxRootInput.InvokeY
    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BUTTON_B -> GameBoxRootInput.Back
    else -> null
}

internal fun gameBoxNavigationSoundForKey(keyCode: Int): Int? = when (keyCode) {
    KeyEvent.KEYCODE_DPAD_LEFT -> SoundEffectConstants.NAVIGATION_LEFT
    KeyEvent.KEYCODE_DPAD_RIGHT -> SoundEffectConstants.NAVIGATION_RIGHT
    KeyEvent.KEYCODE_DPAD_UP -> SoundEffectConstants.NAVIGATION_UP
    KeyEvent.KEYCODE_DPAD_DOWN -> SoundEffectConstants.NAVIGATION_DOWN
    else -> null
}

/** Applies the shell-level controller contract while leaving navigation state in the app host. */
internal fun Modifier.onGameBoxRootInput(
    soundsEnabled: Boolean,
    playSoundEffect: (Int) -> Unit,
    controllerActions: ControllerActionRegistry,
    movePrimaryTab: (Int) -> Unit,
    openQuickActions: () -> Unit,
    openSystemStatus: () -> Unit,
    fallbackX: () -> Unit,
    fallbackY: () -> Unit,
    handleBack: () -> Boolean,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val keyCode = event.nativeKeyEvent.keyCode
    if (soundsEnabled && event.nativeKeyEvent.repeatCount == 0) {
        gameBoxNavigationSoundForKey(keyCode)?.let(playSoundEffect)
    }
    when (val input = gameBoxRootInputForKey(keyCode)) {
        is GameBoxRootInput.MovePrimaryTab -> {
            movePrimaryTab(input.offset)
            true
        }
        GameBoxRootInput.OpenQuickActions -> {
            openQuickActions()
            true
        }
        GameBoxRootInput.OpenSystemStatus -> {
            openSystemStatus()
            true
        }
        GameBoxRootInput.InvokeX -> {
            if (!controllerActions.invokeX()) fallbackX()
            true
        }
        GameBoxRootInput.InvokeY -> {
            if (!controllerActions.invokeY()) fallbackY()
            true
        }
        GameBoxRootInput.Back -> handleBack()
        null -> false
    }
}

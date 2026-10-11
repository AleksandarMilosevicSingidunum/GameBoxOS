package com.gamebox.os.input

import android.view.InputDevice
import android.view.KeyEvent

/** Preserve WebView's native user-gesture activation for links and custom HTML buttons. */
internal fun gamepadBrowserActivationKeyCode(keyCode: Int): Int? = when (keyCode) {
    KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_ENTER
    KeyEvent.KEYCODE_DPAD_CENTER -> KeyEvent.KEYCODE_DPAD_CENTER
    else -> null
}

internal fun gamepadBrowserNavigationKeyCode(keyCode: Int): Int? = when (keyCode) {
    KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_DPAD_UP,
    KeyEvent.KEYCODE_DPAD_LEFT -> keyCode
    else -> null
}

internal fun gamepadBrowserKeyEvent(event: KeyEvent, keyCode: Int, metaState: Int): KeyEvent =
    KeyEvent(
        event.downTime,
        event.eventTime,
        event.action,
        keyCode,
        event.repeatCount,
        metaState,
        event.deviceId,
        event.scanCode,
        event.flags,
        InputDevice.SOURCE_KEYBOARD,
    )

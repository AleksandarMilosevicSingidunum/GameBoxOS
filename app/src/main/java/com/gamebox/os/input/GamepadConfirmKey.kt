package com.gamebox.os.input

import android.view.KeyEvent

/** Normalize Android gamepad confirm into the D-pad activation understood by views. */
internal fun gamepadConfirmKeyCode(keyCode: Int): Int? =
    KeyEvent.KEYCODE_DPAD_CENTER.takeIf { keyCode == KeyEvent.KEYCODE_BUTTON_A }

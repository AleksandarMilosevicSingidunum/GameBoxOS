package com.gamebox.os.input

import android.view.KeyEvent
import kotlin.math.abs

/**
 * Converts left-stick/hat motion into paced D-pad steps.
 *
 * A direction is emitted immediately on engagement or direction change, then after
 * [initialRepeatDelayMs] while held and at [repeatIntervalMs] thereafter. The lower
 * release threshold prevents noisy axes from repeatedly engaging around the dead zone.
 */
internal class AnalogNavigationDebouncer(
    private val engageThreshold: Float = 0.65f,
    private val releaseThreshold: Float = 0.35f,
    private val initialRepeatDelayMs: Long = 320L,
    private val repeatIntervalMs: Long = 120L,
) {
    private var activeKeyCode: Int? = null
    private var nextRepeatAtMs: Long = 0L

    fun consume(
        stickX: Float,
        stickY: Float,
        hatX: Float,
        hatY: Float,
        eventTimeMs: Long,
    ): Int? {
        val x = hatX.takeIf { abs(it) >= engageThreshold } ?: stickX
        val y = hatY.takeIf { abs(it) >= engageThreshold } ?: stickY
        val active = activeKeyCode
        if (abs(x) <= releaseThreshold && abs(y) <= releaseThreshold) {
            activeKeyCode = null
            nextRepeatAtMs = 0L
            return null
        }

        val candidate = if (abs(x) > abs(y)) {
            if (x < -engageThreshold) KeyEvent.KEYCODE_DPAD_LEFT
            else if (x > engageThreshold) KeyEvent.KEYCODE_DPAD_RIGHT
            else null
        } else {
            if (y < -engageThreshold) KeyEvent.KEYCODE_DPAD_UP
            else if (y > engageThreshold) KeyEvent.KEYCODE_DPAD_DOWN
            else null
        } ?: return null

        if (candidate != active) {
            activeKeyCode = candidate
            nextRepeatAtMs = eventTimeMs + initialRepeatDelayMs
            return candidate
        }
        if (eventTimeMs >= nextRepeatAtMs) {
            nextRepeatAtMs = eventTimeMs + repeatIntervalMs
            return candidate
        }
        return null
    }

    fun reset() {
        activeKeyCode = null
        nextRepeatAtMs = 0L
    }
}

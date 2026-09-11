package com.gamebox.os.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalogNavigationDebouncerTest {
    @Test fun deadZoneDoesNotNavigateAndRequiresReleaseBeforeReengaging() {
        val subject = AnalogNavigationDebouncer()
        assertNull(subject.consume(.4f, .2f, 0f, 0f, 0))
        assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, subject.consume(.8f, .1f, 0f, 0f, 10))
        assertNull(subject.consume(.7f, .1f, 0f, 0f, 100))
        assertNull(subject.consume(.2f, .1f, 0f, 0f, 110))
        assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, subject.consume(.8f, 0f, 0f, 0f, 120))
    }

    @Test fun heldDirectionUsesInitialDelayThenBoundedRepeatCadence() {
        val subject = AnalogNavigationDebouncer(initialRepeatDelayMs = 300, repeatIntervalMs = 100)
        assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, subject.consume(0f, .9f, 0f, 0f, 1_000))
        assertNull(subject.consume(0f, .9f, 0f, 0f, 1_299))
        assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, subject.consume(0f, .9f, 0f, 0f, 1_300))
        assertNull(subject.consume(0f, .9f, 0f, 0f, 1_399))
        assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, subject.consume(0f, .9f, 0f, 0f, 1_400))
    }

    @Test fun directionChangeEmitsImmediatelyAndDominantAxisWins() {
        val subject = AnalogNavigationDebouncer()
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, subject.consume(-.9f, .7f, 0f, 0f, 0))
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, subject.consume(-.7f, -.9f, 0f, 0f, 10))
    }

    @Test fun dpadHatTakesPriorityAndResetAllowsImmediateStep() {
        val subject = AnalogNavigationDebouncer()
        assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, subject.consume(-.9f, 0f, 1f, 0f, 0))
        subject.reset()
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, subject.consume(-.9f, 0f, 0f, 0f, 1))
    }
}

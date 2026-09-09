package com.gamebox.os.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchSessionDurationTest {
    @Test fun durationRoundsDownAndRejectsClockRollback() {
        assertEquals(2, sessionMinutesAway(1_000, 126_000))
        assertEquals(0, sessionMinutesAway(2_000, 1_000))
        assertEquals(0, sessionMinutesAway(-1, 1_000))
    }

    @Test fun extremeClockChangeCannotOverflowToNegativeMinutes() {
        assertEquals(Int.MAX_VALUE, sessionMinutesAway(0, Long.MAX_VALUE))
    }
}

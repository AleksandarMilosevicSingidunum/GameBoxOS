package com.gamebox.os.launch

import org.junit.Assert.*
import org.junit.Test

class DeferredHostReturnTest {
    @Test fun initialResumeIsNotAnExternalReturn() {
        val events = DeferredHostReturn()
        events.arm()
        events.onResumed()
        assertFalse(events.take())
    }

    @Test fun quickReturnIsConsumedExactlyOnce() {
        val events = DeferredHostReturn()
        events.arm()
        events.onPaused()
        events.onResumed()
        assertTrue(events.take())
        assertFalse(events.take())
    }

    @Test fun stillAwayDoesNotFinishSession() {
        val events = DeferredHostReturn()
        events.arm()
        events.onPaused()
        assertFalse(events.take())
    }

    @Test fun leavingAgainInvalidatesAnEarlierReturn() {
        val events = DeferredHostReturn()
        events.arm()
        events.onPaused()
        events.onResumed()
        events.onPaused()
        assertFalse(events.take())
    }

    @Test fun lifecycleBeforeDispatchAndPreviousLaunchCannotLeak() {
        val events = DeferredHostReturn()
        events.onPaused()
        events.onResumed()
        events.arm()
        assertFalse(events.take())
        events.arm()
        events.onPaused()
        events.onResumed()
        events.arm()
        assertFalse(events.take())
    }
}


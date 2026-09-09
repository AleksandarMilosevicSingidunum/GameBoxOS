package com.gamebox.os.launch

import java.util.concurrent.CancellationException

/** Cancellation is accepted until the external handoff commits, never after it. */
class LaunchPreparation {
    private var cancelled = false
    private var committed = false
    private var beforeDispatch: () -> Unit = {}

    @Synchronized fun beforeDispatch(action: () -> Unit) {
        check(!committed)
        beforeDispatch = action
    }

    @Synchronized fun cancel(): Boolean {
        if (committed) return false
        cancelled = true
        return true
    }

    @Synchronized fun checkActive() {
        if (cancelled) throw CancellationException("Launch preparation cancelled")
    }

    fun <T> dispatch(block: () -> T): T {
        synchronized(this) {
            checkActive()
            check(!committed) { "Handoff already committed" }
            committed = true
        }
        // The production gateway calls this from its IO worker. Persist before
        // starting the external activity; a failed write must prevent handoff.
        beforeDispatch()
        return block()
    }
}

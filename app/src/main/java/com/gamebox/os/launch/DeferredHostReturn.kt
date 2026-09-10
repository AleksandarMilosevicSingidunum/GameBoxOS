package com.gamebox.os.launch

/** Retains an actual pause/resume across asynchronous handoff confirmation. */
internal class DeferredHostReturn {
    private var armed = false
    private var paused = false
    private var returned = false

    @Synchronized fun arm() {
        armed = true
        paused = false
        returned = false
    }

    @Synchronized fun onPaused() {
        if (armed) {
            paused = true
            returned = false
        }
    }

    @Synchronized fun onResumed() {
        if (armed && paused) returned = true
    }

    @Synchronized fun take(): Boolean {
        val result = returned
        armed = false
        paused = false
        returned = false
        return result
    }
}


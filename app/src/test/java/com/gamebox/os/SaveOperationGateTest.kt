package com.gamebox.os

import com.gamebox.os.storage.SaveOperationGate
import org.junit.Assert.*
import org.junit.Test

class SaveOperationGateTest {
    @Test fun sameGameIsExclusiveAndReleaseAllowsRetry() {
        val key = "gate-test:first"
        try {
            assertTrue(SaveOperationGate.acquire(key))
            assertFalse(SaveOperationGate.acquire(key))
            assertTrue(SaveOperationGate.acquire("gate-test:second"))
            SaveOperationGate.release(key)
            assertTrue(SaveOperationGate.acquire(key))
        } finally {
            SaveOperationGate.release(key)
            SaveOperationGate.release("gate-test:second")
        }
    }
}


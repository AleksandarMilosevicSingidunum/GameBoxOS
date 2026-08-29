package com.gamebox.os

import com.gamebox.os.storage.SaveConflictStrategy
import com.gamebox.os.storage.SaveRevision
import com.gamebox.os.storage.SaveSyncResult
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveSyncModelsTest {
    @Test
    fun conflictPreservesBothRevisions() {
        val local = SaveRevision("game", "local", 10L, 100L)
        val remote = SaveRevision("game", "remote", 20L, 120L)
        val result = SaveSyncResult.Conflict(local, remote)
        assertEquals("local", result.local.checksum)
        assertEquals("remote", result.remote.checksum)
    }

    @Test
    fun keepBothIsAvailableAsNonDestructiveStrategy() {
        assertEquals(SaveConflictStrategy.KEEP_BOTH, SaveConflictStrategy.valueOf("KEEP_BOTH"))
    }
}
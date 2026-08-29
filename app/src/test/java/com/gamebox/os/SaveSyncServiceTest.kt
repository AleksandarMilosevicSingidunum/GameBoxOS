package com.gamebox.os

import com.gamebox.os.storage.SaveConflictStrategy
import com.gamebox.os.storage.SaveRevision
import com.gamebox.os.storage.SaveSyncOperations
import com.gamebox.os.storage.SaveSyncResolver
import com.gamebox.os.storage.SaveSyncResult
import com.gamebox.os.storage.SaveSyncService
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveSyncServiceTest {
    @Test
    fun delegatesConflictsToPreservationOperation() {
        val local = SaveRevision("game", "local", 1L, 1L)
        val remote = SaveRevision("game", "remote", 2L, 1L)
        val operations = object : SaveSyncOperations {
            override fun upload(revision: SaveRevision) = SaveSyncResult.Uploaded(revision)
            override fun download(revision: SaveRevision) = SaveSyncResult.Downloaded(revision)
            override fun preserveConflict(local: SaveRevision, remote: SaveRevision) = SaveSyncResult.Conflict(local, remote)
        }
        val result = SaveSyncService(SaveSyncResolver(SaveConflictStrategy.KEEP_BOTH), operations).synchronize(local, remote)
        assertEquals(SaveSyncResult.Conflict(local, remote), result)
    }
}
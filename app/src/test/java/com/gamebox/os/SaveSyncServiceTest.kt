package com.gamebox.os

import com.gamebox.os.storage.*
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveSyncServiceTest {
    private class RecordingOperations : SaveSyncOperations {
        var uploads = 0; var downloads = 0; var conflicts = 0
        override fun upload(revision: SaveRevision): SaveSyncResult { uploads++; return SaveSyncResult.Uploaded(revision) }
        override fun download(revision: SaveRevision): SaveSyncResult { downloads++; return SaveSyncResult.Downloaded(revision) }
        override fun preserveConflict(local: SaveRevision, remote: SaveRevision): SaveSyncResult { conflicts++; return SaveSyncResult.Conflict(local, remote) }
    }
    @Test fun executesUploadAndDownloadDecisions() {
        val local = SaveRevision("game", "local", 1L, 1L)
        val remote = SaveRevision("game", "remote", 2L, 1L)
        val operations = RecordingOperations()
        SaveSyncService(SaveSyncResolver(SaveConflictStrategy.KEEP_LOCAL), operations).synchronize(local, null)
        SaveSyncService(SaveSyncResolver(SaveConflictStrategy.KEEP_REMOTE), operations).synchronize(null, remote)
        assertEquals(1, operations.uploads); assertEquals(1, operations.downloads)
    }
    @Test fun delegatesConflictsAndSkipsMatchingRevisions() {
        val local = SaveRevision("game", "local", 1L, 1L)
        val remote = SaveRevision("game", "remote", 2L, 1L)
        val operations = RecordingOperations()
        SaveSyncService(SaveSyncResolver(SaveConflictStrategy.KEEP_BOTH), operations).synchronize(local, remote)
        SaveSyncService(operations = operations).synchronize(local, local.copy())
        assertEquals(1, operations.conflicts); assertEquals(0, operations.uploads); assertEquals(0, operations.downloads)
    }
}

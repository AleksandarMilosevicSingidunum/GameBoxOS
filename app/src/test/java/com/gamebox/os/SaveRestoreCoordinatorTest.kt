package com.gamebox.os

import com.gamebox.os.storage.BackupResult
import com.gamebox.os.storage.SaveBackupService
import com.gamebox.os.storage.SaveRestoreCoordinator
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveRestoreCoordinatorTest {
    @Test
    fun rejectsCrossGameAndTraversalPathsPerArtifact() {
        val root = createTempDirectory("gamebox-save-restore-").toFile()
        try {
            val service = SaveBackupService(File(root, "saves"), File(root, "backups"))
            val result = SaveRestoreCoordinator(service).restore(
                "game-a",
                listOf("game-b/slot.sav", "game-a/../game-b/other.sav", "game-a/slot.sav"),
            )

            assertEquals(BackupResult.CROSS_GAME_PATH, result.artifacts[0].result)
            assertEquals(BackupResult.CROSS_GAME_PATH, result.artifacts[1].result)
            assertEquals(BackupResult.BACKUP_MISSING, result.artifacts[2].result)
            assertEquals(0, result.successfulCount)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun restoresMultipleChecksumProtectedArtifactsAfterContentRemoval() {
        val root = createTempDirectory("gamebox-save-restore-").toFile()
        val saves = File(root, "saves")
        val backups = File(root, "backups")
        try {
            File(saves, "game-a").mkdirs()
            File(saves, "game-a/slot1.sav").writeText("one")
            File(saves, "game-a/slot2.sav").writeText("two")
            val service = SaveBackupService(saves, backups)
            assertEquals(BackupResult.SUCCESS, service.createBackup("game-a/slot1.sav"))
            assertEquals(BackupResult.SUCCESS, service.createBackup("game-a/slot2.sav"))
            File(saves, "game-a").deleteRecursively()

            val result = SaveRestoreCoordinator(service).restore(
                "game-a",
                listOf("game-a/slot1.sav", "game-a/slot2.sav"),
            )

            assertEquals(2, result.successfulCount)
            assertEquals(0, result.failedCount)
            assertTrue(File(saves, "game-a/slot1.sav").readText() == "one")
            assertTrue(File(saves, "game-a/slot2.sav").readText() == "two")
        } finally {
            root.deleteRecursively()
        }
    }
}

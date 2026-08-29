package com.gamebox.os

import com.gamebox.os.storage.BackupResult
import com.gamebox.os.storage.SaveBackupService
import com.gamebox.os.storage.SaveRestoreCoordinator
import com.gamebox.os.storage.SaveSnapshotManifest
import com.gamebox.os.storage.SaveSnapshotManifestStore
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveRestoreManifestWiringTest {
    @Test
    fun restoresLatestManifestAfterCoordinatorRecreation() {
        val root = createTempDirectory("gamebox-save-latest-").toFile()
        val saves = File(root, "saves")
        val backups = File(root, "backups")
        val manifests = File(root, "manifests")
        try {
            File(saves, "game-a").mkdirs()
            File(saves, "game-a/slot.sav").writeText("progress")
            val backupService = SaveBackupService(saves, backups)
            assertEquals(BackupResult.SUCCESS, backupService.createBackup("game-a/slot.sav"))
            SaveSnapshotManifestStore(manifests).save(
                SaveSnapshotManifest("game-a", 1L, listOf("game-a/slot.sav")),
            )
            File(saves, "game-a").deleteRecursively()

            val recreated = SaveRestoreCoordinator(
                backupService,
                SaveSnapshotManifestStore(manifests),
            )
            val result = recreated.restoreLatest("game-a")

            assertEquals(1, result.successfulCount)
            assertTrue(File(saves, "game-a/slot.sav").readText() == "progress")
        } finally {
            root.deleteRecursively()
        }
    }
}

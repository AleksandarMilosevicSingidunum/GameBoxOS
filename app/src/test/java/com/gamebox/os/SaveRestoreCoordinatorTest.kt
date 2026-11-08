package com.gamebox.os

import com.gamebox.os.storage.BackupResult
import com.gamebox.os.storage.SaveBackupService
import com.gamebox.os.storage.SaveRestoreCoordinator
import com.gamebox.os.storage.DirectorySaveAdapter
import com.gamebox.os.storage.SaveSnapshotManifest
import com.gamebox.os.storage.SaveSnapshotManifestStore
import com.gamebox.os.storage.BidirectionalSaveAdapter
import com.gamebox.os.storage.SaveArtifact
import java.io.InputStream
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveRestoreCoordinatorTest {
    @Test
    fun automaticRestorePreservesExistingLiveProgress() {
        val root = createTempDirectory("gamebox-save-auto-restore-").toFile()
        val live = File(root, "live")
        val backups = File(root, "backups")
        val manifests = File(root, "manifests")
        try {
            File(live, "game-a").mkdirs()
            File(live, "game-a/slot.sav").writeText("newer live progress")
            val service = SaveBackupService(File(root, "snapshot-source"), backups)
            val backupPath = ".snapshots/generation/game-a/slot.sav"
            File(backups, backupPath).apply { requireNotNull(parentFile).mkdirs(); writeText("older snapshot") }
            SaveSnapshotManifestStore(manifests).save(
                SaveSnapshotManifest("game-a", 1L, listOf("game-a/slot.sav"), "generation"),
            )

            val result = SaveRestoreCoordinator(
                backupService = service,
                manifestStore = SaveSnapshotManifestStore(manifests),
                adapter = DirectorySaveAdapter(live),
            ).restoreLatest("game-a")

            assertEquals(0, result.successfulCount)
            assertEquals(
                "Live emulator saves already exist; automatic restore was skipped to protect current progress",
                result.message,
            )
            assertEquals("newer live progress", File(live, "game-a/slot.sav").readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun automaticRestoreDoesNothingWhenLiveSaveInspectionFails() {
        val root = createTempDirectory("gamebox-save-inspection-failure-").toFile()
        val backups = File(root, "backups")
        val manifests = File(root, "manifests")
        try {
            val backupPath = ".snapshots/generation/game-a/slot.sav"
            File(backups, backupPath).apply { requireNotNull(parentFile).mkdirs(); writeText("snapshot") }
            SaveSnapshotManifestStore(manifests).save(
                SaveSnapshotManifest("game-a", 1L, listOf("game-a/slot.sav"), "generation"),
            )
            val adapter = object : BidirectionalSaveAdapter {
                override fun discover(gameId: String): List<SaveArtifact> = error("storage unavailable")
                override fun openArtifact(artifact: SaveArtifact): InputStream = error("unused")
                override fun restoreArtifact(artifact: SaveArtifact, source: InputStream) = error("must not restore")
                override fun deleteArtifact(artifact: SaveArtifact) = error("must not delete")
            }

            val result = SaveRestoreCoordinator(
                backupService = SaveBackupService(File(root, "snapshot-source"), backups),
                manifestStore = SaveSnapshotManifestStore(manifests),
                adapter = adapter,
            ).restoreLatest("game-a")

            assertEquals(0, result.successfulCount)
            assertEquals(
                "Live emulator saves could not be checked; current progress was left untouched",
                result.message,
            )
            assertTrue(File(backups, backupPath).isFile)
        } finally {
            root.deleteRecursively()
        }
    }

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

    @Test
    fun invalidLaterBackupAbortsBeforeChangingAnyLiveArtifact() {
        val root = createTempDirectory("gamebox-save-restore-preflight-").toFile()
        val saves = File(root, "saves")
        val backups = File(root, "backups")
        try {
            File(saves, "game-a").mkdirs()
            File(saves, "game-a/slot1.sav").writeText("current one")
            File(saves, "game-a/slot2.sav").writeText("current two")
            val service = SaveBackupService(saves, backups)
            assertEquals(BackupResult.SUCCESS, service.createBackup("game-a/slot1.sav"))
            assertEquals(BackupResult.SUCCESS, service.createBackup("game-a/slot2.sav"))
            File(saves, "game-a/slot1.sav").writeText("newer live one")
            File(saves, "game-a/slot2.sav").writeText("newer live two")
            File(backups, "game-a/slot2.sav").writeText("corrupt snapshot")

            val result = SaveRestoreCoordinator(service).restore(
                "game-a",
                listOf("game-a/slot1.sav", "game-a/slot2.sav"),
            )

            assertEquals(0, result.successfulCount)
            assertEquals(BackupResult.SNAPSHOT_ABORTED, result.artifacts[0].result)
            assertEquals(BackupResult.CHECKSUM_MISMATCH, result.artifacts[1].result)
            assertEquals("newer live one", File(saves, "game-a/slot1.sav").readText())
            assertEquals("newer live two", File(saves, "game-a/slot2.sav").readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun failedLaterAdapterWriteRollsBackReplacedFilesAndRemovesNewFiles() {
        val root = createTempDirectory("gamebox-save-restore-rollback-").toFile()
        val live = File(root, "live")
        val saves = File(root, "snapshots-source")
        val backups = File(root, "backups")
        try {
            val paths = listOf("game-a/one.sav", "game-a/two.sav", "game-a/three.sav")
            paths.forEachIndexed { index, path ->
                File(saves, path).apply { requireNotNull(parentFile).mkdirs(); writeText("restore-$index") }
            }
            val service = SaveBackupService(saves, backups)
            paths.forEach { assertEquals(BackupResult.SUCCESS, service.createBackup(it)) }
            File(saves, paths[0]).writeText("original-one")
            File(saves, paths[1]).writeText("original-two")
            File(saves, paths[2]).delete()

            val directory = DirectorySaveAdapter(live)
            var writes = 0
            val adapter = object : BidirectionalSaveAdapter {
                override fun discover(gameId: String) = directory.discover(gameId)
                override fun openArtifact(artifact: SaveArtifact) = directory.openArtifact(artifact)
                override fun deleteArtifact(artifact: SaveArtifact) = directory.deleteArtifact(artifact)
                override fun restoreArtifact(artifact: SaveArtifact, source: InputStream) {
                    writes++
                    directory.restoreArtifact(artifact, source)
                    if (writes == 3) error("simulated provider failure after publication")
                }
            }
            paths.forEachIndexed { index, path ->
                if (index < 2) File(live, path).apply { requireNotNull(parentFile).mkdirs(); writeText("original-${if (index == 0) "one" else "two"}") }
            }

            val result = SaveRestoreCoordinator(service, adapter = adapter).restore("game-a", paths)

            assertEquals(0, result.successfulCount)
            assertTrue(result.message!!.contains("previous live saves were restored"))
            assertEquals("original-one", File(live, paths[0]).readText())
            assertEquals("original-two", File(live, paths[1]).readText())
            assertTrue(!File(live, paths[2]).exists())
            assertTrue(!File(backups, ".restore-transactions").exists())
        } finally {
            root.deleteRecursively()
        }
    }
}

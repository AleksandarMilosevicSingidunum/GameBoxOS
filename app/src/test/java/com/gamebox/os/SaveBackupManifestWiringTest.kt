package com.gamebox.os

import com.gamebox.os.storage.DirectorySaveAdapter
import com.gamebox.os.storage.SaveAdapterRegistry
import com.gamebox.os.storage.SaveBackupCoordinator
import com.gamebox.os.storage.SaveBackupService
import com.gamebox.os.storage.SaveSnapshotManifestStore
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveBackupManifestWiringTest {
    @Test
    fun persistsOnlySuccessfullyBackedUpArtifactPaths() {
        val root = createTempDirectory("gamebox-save-manifest-wiring-").toFile()
        val saves = File(root, "saves")
        val backups = File(root, "backups")
        val manifests = File(root, "manifests")
        try {
            File(saves, "game-a").mkdirs()
            File(saves, "game-a/slot.sav").writeText("progress")
            val store = SaveSnapshotManifestStore(manifests)
            val coordinator = SaveBackupCoordinator(
                registry = SaveAdapterRegistry(mapOf("ppsspp" to DirectorySaveAdapter(saves))),
                backupService = SaveBackupService(saves, backups),
                manifestStore = store,
                nowMillis = { 456L },
            )

            val result = coordinator.backup("ppsspp", "game-a")
            val manifest = SaveSnapshotManifestStore(manifests).load("game-a")

            assertEquals(1, result.successfulCount)
            assertEquals(456L, manifest?.createdAtMillis)
            assertEquals(listOf("game-a/slot.sav"), manifest?.relativePaths)
        } finally {
            root.deleteRecursively()
        }
    }
}

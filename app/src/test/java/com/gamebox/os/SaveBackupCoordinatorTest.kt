package com.gamebox.os

import com.gamebox.os.storage.BackupResult
import com.gamebox.os.storage.DirectorySaveAdapter
import com.gamebox.os.storage.SaveAdapterRegistry
import com.gamebox.os.storage.SaveBackupCoordinator
import com.gamebox.os.storage.SaveBackupService
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveBackupCoordinatorTest {
    @Test
    fun discoversAndBacksUpEveryArtifact() {
        val root = createTempDirectory("gamebox-save-coordinator-").toFile()
        val saves = File(root, "saves")
        val backups = File(root, "backups")
        try {
            File(saves, "game-a").mkdirs()
            File(saves, "game-a/slot1.sav").writeText("one")
            File(saves, "game-a/slot2.sav").writeText("two")

            val coordinator = SaveBackupCoordinator(
                SaveAdapterRegistry(mapOf("PPSSPP" to DirectorySaveAdapter(saves))),
                SaveBackupService(saves, backups),
            )

            val result = coordinator.backup("ppsspp", "game-a")

            assertEquals(2, result.successfulCount)
            assertEquals(0, result.failedCount)
            assertTrue(result.artifacts.all { it.result == BackupResult.SUCCESS })
            assertTrue(File(backups, "game-a/slot1.sav").isFile)
            assertTrue(File(backups, "game-a/slot2.sav").isFile)
        } finally {
            root.deleteRecursively()
        }
    }
}

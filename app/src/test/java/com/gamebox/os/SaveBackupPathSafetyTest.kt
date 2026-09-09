package com.gamebox.os

import com.gamebox.os.storage.SaveBackupService
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class SaveBackupPathSafetyTest {
    @Test fun traversalCannotReadOrOverwriteAnotherGamesSave() {
        val root = Files.createTempDirectory("save-path-safety-").toFile()
        try {
            val saves = root.resolve("saves").apply { mkdirs() }
            val backups = root.resolve("backups").apply { mkdirs() }
            val other = saves.resolve("other/progress.sav").apply { parentFile.mkdirs(); writeText("keep") }
            val service = SaveBackupService(saves, backups)
            for (path in listOf("game/../other/progress.sav", "game/./save", "game//save",
                "game\\save", "game/save:stream", "game/\u0000save")) {
                assertThrows(IllegalArgumentException::class.java) { service.createBackup(path) }
                assertThrows(IllegalArgumentException::class.java) { service.restore(path) }
                assertThrows(IllegalArgumentException::class.java) { service.hasBackup(path) }
            }
            assertEquals("keep", other.readText())
            assertTrue(backups.listFiles().orEmpty().isEmpty())
        } finally { root.deleteRecursively() }
    }
}


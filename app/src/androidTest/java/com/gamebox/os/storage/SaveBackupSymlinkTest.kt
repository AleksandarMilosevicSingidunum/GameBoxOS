package com.gamebox.os.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class SaveBackupSymlinkTest {
    @Test fun gameResolverRejectsLinkedFilesAndGameDirectories() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(app.cacheDir.toPath(), "save-identity-links-").toFile()
        try {
            val other = root.resolve("other/save.dat").apply { parentFile.mkdirs(); writeText("PRIVATE") }
            val linkedGame = root.resolve("linked-game")
            Files.createSymbolicLink(linkedGame.toPath(), other.parentFile.toPath())
            try {
                assertThrows(IllegalArgumentException::class.java) {
                    resolveGameSave(root, "linked-game", "linked-game/save.dat")
                }
            } finally { Files.deleteIfExists(linkedGame.toPath()) }
            val linkedFile = root.resolve("game/save.dat").apply { parentFile.mkdirs() }
            Files.createSymbolicLink(linkedFile.toPath(), other.toPath())
            try {
                assertThrows(IllegalArgumentException::class.java) {
                    resolveGameSave(root, "game", "game/save.dat")
                }
            } finally { Files.deleteIfExists(linkedFile.toPath()) }
            assertEquals("PRIVATE", other.readText())
        } finally { root.deleteRecursively() }
    }
    @Test fun backupAndTemporaryLinksCannotTouchOtherGameFiles() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(app.cacheDir.toPath(), "save-link-test-").toFile()
        try {
            val saves = root.resolve("saves").apply { mkdirs() }
            val backups = root.resolve("backups").apply { mkdirs() }
            val save = saves.resolve("game/save.dat").apply { parentFile.mkdirs(); writeText("SAVE") }
            val other = root.resolve("other.dat").apply { writeText("KEEP") }
            val service = SaveBackupService(saves, backups)
            for (suffix in listOf("", ".part", ".sha256", ".import.part")) {
                val link = backups.resolve("game/save.dat$suffix")
                link.parentFile.mkdirs()
                Files.createSymbolicLink(link.toPath(), other.toPath())
                try {
                    assertThrows(IllegalArgumentException::class.java) {
                        if (suffix == ".import.part") service.importBackup("game/save.dat", "NEW".byteInputStream())
                        else service.createBackup("game/save.dat")
                    }
                    assertEquals("KEEP", other.readText())
                    assertEquals("SAVE", save.readText())
                } finally { Files.deleteIfExists(link.toPath()) }
            }
            assertEquals(BackupResult.SUCCESS, service.createBackup("game/save.dat"))
            val restoreLink = saves.resolve("game/save.dat.restore.part")
            Files.createSymbolicLink(restoreLink.toPath(), other.toPath())
            try {
                assertThrows(IllegalArgumentException::class.java) { service.restore("game/save.dat") }
                assertEquals("KEEP", other.readText())
            } finally { Files.deleteIfExists(restoreLink.toPath()) }
        } finally { root.deleteRecursively() }
    }
}

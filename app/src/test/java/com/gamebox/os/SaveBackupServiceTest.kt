package com.gamebox.os

import com.gamebox.os.storage.BackupResult
import com.gamebox.os.storage.AtomicSaveSnapshot
import com.gamebox.os.storage.SaveBackupService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.IOException

class SaveBackupServiceTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun verifiedBackupRestoresChangedSave() {
        val saves = temporaryFolder.newFolder("saves")
        val backups = temporaryFolder.newFolder("backups")
        val save = saves.resolve("retro-test/save.dat")
        save.parentFile.mkdirs()
        save.writeText("SAVE")
        val service = SaveBackupService(saves, backups)

        assertEquals(BackupResult.SUCCESS, service.createBackup("retro-test/save.dat"))
        save.writeText("CHANGED")
        assertEquals(BackupResult.SUCCESS, service.restore("retro-test/save.dat"))
        assertEquals("SAVE", save.readText())
    }

    @Test fun tamperedBackupIsNeverRestored() {
        val saves = temporaryFolder.newFolder("saves")
        val backups = temporaryFolder.newFolder("backups")
        val save = saves.resolve("retro-test/save.dat")
        save.parentFile.mkdirs()
        save.writeText("SAVE")
        val service = SaveBackupService(saves, backups)
        service.createBackup("retro-test/save.dat")
        backups.resolve("retro-test/save.dat").writeText("TAMPERED")
        save.writeText("CURRENT")

        assertEquals(BackupResult.CHECKSUM_MISMATCH, service.restore("retro-test/save.dat"))
        assertEquals("CURRENT", save.readText())
    }

    @Test fun providerExportAndImportRemainVerifiedAndStaged() {
        val saves = temporaryFolder.newFolder("saves")
        val backups = temporaryFolder.newFolder("backups")
        val save = saves.resolve("retro-test/save.dat")
        save.parentFile.mkdirs()
        save.writeText("SAVE")
        val service = SaveBackupService(saves, backups)
        service.createBackup("retro-test/save.dat")
        val exported = ByteArrayOutputStream()

        assertEquals(
            BackupResult.SUCCESS,
            service.exportBackup("retro-test/save.dat", exported)
        )
        assertEquals(
            BackupResult.SUCCESS,
            service.importBackup(
                "retro-test/save.dat",
                ByteArrayInputStream("IMPORTED".toByteArray())
            )
        )
        save.writeText("CURRENT")
        assertEquals(BackupResult.SUCCESS, service.restore("retro-test/save.dat"))
        assertEquals("IMPORTED", save.readText())
        assertEquals("SAVE", exported.toString())
    }

    @Test fun providerImportHonorsSizeLimitWithoutReplacingBackup() {
        val saves = temporaryFolder.newFolder("saves")
        val backups = temporaryFolder.newFolder("backups")
        val save = saves.resolve("retro-test/save.dat")
        save.parentFile.mkdirs()
        save.writeText("ORIGINAL")
        val service = SaveBackupService(saves, backups)
        service.createBackup("retro-test/save.dat")

        assertEquals(
            BackupResult.SIZE_LIMIT_EXCEEDED,
            service.importBackup(
                "retro-test/save.dat",
                ByteArrayInputStream("TOO-LARGE".toByteArray()),
                maxBytes = 3
            )
        )
        save.writeText("CURRENT")
        assertEquals(BackupResult.SUCCESS, service.restore("retro-test/save.dat"))
        assertEquals("ORIGINAL", save.readText())
    }

    @Test fun backupCreationHonorsSizeLimitWithoutPublishingPartialFile() {
        val saves = temporaryFolder.newFolder("saves")
        val backups = temporaryFolder.newFolder("backups")
        val save = saves.resolve("retro-test/large.dat")
        save.parentFile.mkdirs()
        save.writeBytes(ByteArray(8) { 7 })
        val service = SaveBackupService(saves, backups, maxBackupBytes = 3)

        assertEquals(BackupResult.SIZE_LIMIT_EXCEEDED, service.createBackup("retro-test/large.dat"))
        assertEquals(false, backups.resolve("retro-test/large.dat").exists())
        assertEquals(false, backups.resolve("retro-test/large.dat.sha256").exists())
    }

    @Test fun emptySaveCannotReplaceLastGoodBackup() {
        val saves = temporaryFolder.newFolder("saves")
        val backups = temporaryFolder.newFolder("backups")
        val save = saves.resolve("game/save.dat").apply { parentFile.mkdirs(); writeText("GOOD") }
        val service = SaveBackupService(saves, backups)
        assertEquals(BackupResult.SUCCESS, service.createBackup("game/save.dat"))
        val retained = ByteArrayOutputStream().also {
            assertEquals(BackupResult.SUCCESS, service.exportBackup("game/save.dat", it))
        }.toByteArray()
        save.writeText("")
        assertThrows(IllegalArgumentException::class.java) { service.createBackup("game/save.dat") }
        val after = ByteArrayOutputStream().also {
            assertEquals(BackupResult.SUCCESS, service.exportBackup("game/save.dat", it))
        }.toByteArray()
        assertArrayEquals(retained, after)
        assertEquals(false, backups.resolve("game/save.dat.part").exists())
        assertEquals(BackupResult.SUCCESS, service.restore("game/save.dat"))
        assertEquals("GOOD", save.readText())
    }

    @Test fun checksumValidEmptyOrOversizedBackupCannotReplaceSaveOrExport() {
        val saves = temporaryFolder.newFolder("saves")
        val backups = temporaryFolder.newFolder("backups")
        val save = saves.resolve("game/save.dat").apply { parentFile.mkdirs(); writeText("CURRENT") }
        val backup = backups.resolve("game/save.dat").apply { parentFile.mkdirs() }
        val service = SaveBackupService(saves, backups, maxBackupBytes = 3)
        for (bytes in listOf(byteArrayOf(), ByteArray(4) { 7 })) {
            backup.writeBytes(bytes)
            backups.resolve("game/save.dat.sha256").writeText(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
                    .joinToString("") { "%02x".format(it) })
            val expected = if (bytes.isEmpty()) BackupResult.CHECKSUM_MISMATCH else BackupResult.SIZE_LIMIT_EXCEEDED
            assertEquals(expected, service.restore("game/save.dat"))
            val output = ByteArrayOutputStream()
            assertEquals(expected, service.exportBackup("game/save.dat", output))
            assertEquals(0, output.size())
            assertEquals("CURRENT", save.readText())
            assertEquals(false, saves.resolve("game/save.dat.restore.part").exists())
        }
    }

    @Test fun invalidChecksumDestinationDoesNotReplaceBackupBytes() {
        val saves = temporaryFolder.newFolder("saves")
        val backups = temporaryFolder.newFolder("backups")
        val path = "game/save.dat"
        saves.resolve(path).apply { parentFile.mkdirs(); writeText("NEW") }
        val backup = backups.resolve(path).apply { parentFile.mkdirs(); writeText("LAST GOOD") }
        backups.resolve(path + ".sha256").mkdir()
        val service = SaveBackupService(saves, backups)
        assertThrows(IllegalArgumentException::class.java) { service.createBackup(path) }
        assertEquals("LAST GOOD", backup.readText())
        assertThrows(IllegalArgumentException::class.java) {
            service.importBackup(path, ByteArrayInputStream("IMPORTED".toByteArray()))
        }
        assertEquals("LAST GOOD", backup.readText())
        assertEquals(false, backups.resolve(path + ".part").exists())
        assertEquals(false, backups.resolve(path + ".import.part").exists())
    }

    @Test fun legacyBackupRestoresAndNextBackupMigratesAtomically() {
        val saves = temporaryFolder.newFolder("saves")
        val backups = temporaryFolder.newFolder("backups")
        val path = "game/save.dat"
        val save = saves.resolve(path).apply { parentFile.mkdirs(); writeText("CURRENT") }
        val legacy = backups.resolve(path).apply { parentFile.mkdirs(); writeText("LEGACY") }
        backups.resolve(path + ".sha256").writeText(
            java.security.MessageDigest.getInstance("SHA-256").digest(legacy.readBytes())
                .joinToString("") { "%02x".format(it) })
        val service = SaveBackupService(saves, backups)
        assertEquals(true, service.hasBackup(path))
        assertEquals(BackupResult.SUCCESS, service.restore(path))
        assertEquals("LEGACY", save.readText())

        save.writeText("MIGRATED")
        assertEquals(BackupResult.SUCCESS, service.createBackup(path))
        assertEquals(true, AtomicSaveSnapshot.hasHeader(legacy))
        // A stale legacy sidecar is ignored once the atomic header is present.
        backups.resolve(path + ".sha256").writeText("0".repeat(64))
        val exported = ByteArrayOutputStream()
        assertEquals(BackupResult.SUCCESS, service.exportBackup(path, exported))
        assertEquals("MIGRATED", exported.toString())
    }

    @Test fun missingAndTraversalInputsFailClosed() {
        val service = SaveBackupService(
            temporaryFolder.newFolder("saves"),
            temporaryFolder.newFolder("backups")
        )

        assertEquals(BackupResult.SOURCE_MISSING, service.createBackup("game/missing.dat"))
        assertEquals(BackupResult.BACKUP_MISSING, service.restore("game/missing.dat"))
        assertThrows(IllegalArgumentException::class.java) {
            service.restore("../outside.dat")
        }
    }

    @Test fun emptyAndInterruptedImportsRetainExistingBackupAndCleanStaging() {
        val saves = temporaryFolder.newFolder("saves")
        val backups = temporaryFolder.newFolder("backups")
        val save = saves.resolve("game/save.dat").apply {
            parentFile.mkdirs()
            writeText("ORIGINAL")
        }
        val service = SaveBackupService(saves, backups)
        assertEquals(BackupResult.SUCCESS, service.createBackup("game/save.dat"))
        assertThrows(IllegalArgumentException::class.java) {
            service.importBackup("game/save.dat", ByteArrayInputStream(byteArrayOf()))
        }
        val interrupted = object : InputStream() {
            var reads = 0
            override fun read(): Int = throw IOException("provider disconnected")
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (reads++ > 0) throw IOException("provider disconnected")
                buffer[offset] = 42
                return 1
            }
        }
        assertThrows(IOException::class.java) {
            service.importBackup("game/save.dat", interrupted)
        }
        assertEquals(false, backups.resolve("game/save.dat.import.part").exists())
        save.writeText("CURRENT")
        assertEquals(BackupResult.SUCCESS, service.restore("game/save.dat"))
        assertEquals("ORIGINAL", save.readText())
    }
}

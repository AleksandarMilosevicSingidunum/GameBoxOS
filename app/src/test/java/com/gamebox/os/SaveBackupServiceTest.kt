package com.gamebox.os

import com.gamebox.os.storage.BackupResult
import com.gamebox.os.storage.SaveBackupService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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
}

package com.gamebox.os

import com.gamebox.os.storage.BackupResult
import com.gamebox.os.storage.backupResultMessage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveOperationMessageTest {
    @Test fun everyFailureHasSafeActionableFeedback() {
        BackupResult.entries.filter { it != BackupResult.SUCCESS }.forEach { result ->
            val operation = backupResultMessage("Import", result)
            assertFalse(operation.successful)
            assertTrue(operation.message?.startsWith("Import") == true)
        }
    }

    @Test fun successIsReportedWithoutInternalDetails() {
        val operation = backupResultMessage("Backup", BackupResult.SUCCESS)

        assertTrue(operation.successful)
        assertTrue(operation.message == "Backup completed")
    }
}

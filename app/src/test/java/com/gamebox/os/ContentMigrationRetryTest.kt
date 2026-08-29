package com.gamebox.os

import com.gamebox.os.storage.ContentCopyOperation
import com.gamebox.os.storage.ContentMigrationExecutor
import com.gamebox.os.storage.ContentMigrationItem
import com.gamebox.os.storage.ContentMigrationPlanner
import com.gamebox.os.storage.ExternalStorageUnavailableException
import com.gamebox.os.storage.MigrationItemStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentMigrationRetryTest {
    @Test
    fun classifiesStorageUnavailableAsRetryable() {
        val item = ContentMigrationItem("usb", "save.bin", 1)
        val result = ContentMigrationExecutor(
            ContentCopyOperation { Result.failure(ExternalStorageUnavailableException("unplugged")) },
        ).execute(ContentMigrationPlanner.plan(listOf(item)))
        assertEquals(1, result.retryableCount)
        assertEquals(0, result.failedCount)
        assertEquals(MigrationItemStatus.RETRYABLE, result.items.single().status)
    }
}

package com.gamebox.os

import com.gamebox.os.storage.ContentCopyOperation
import com.gamebox.os.storage.ContentMigrationExecutor
import com.gamebox.os.storage.ContentMigrationItem
import com.gamebox.os.storage.ContentMigrationPlanner
import com.gamebox.os.storage.MigrationItemStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentMigrationExecutorTest {
    @Test
    fun reportsPerItemFailuresWithoutStoppingOtherCopies() {
        val items = listOf(ContentMigrationItem("a", "a.bin", 1), ContentMigrationItem("b", "b.bin", 1))
        val plan = ContentMigrationPlanner.plan(items)
        val executor = ContentMigrationExecutor(ContentCopyOperation { item -> if (item.gameId == "a") Result.success(Unit) else Result.failure(IllegalStateException("offline")) })
        val result = executor.execute(plan)
        assertEquals(1, result.copiedCount)
        assertEquals(1, result.failedCount)
        assertEquals(MigrationItemStatus.FAILED, result.items.last().status)
    }
}
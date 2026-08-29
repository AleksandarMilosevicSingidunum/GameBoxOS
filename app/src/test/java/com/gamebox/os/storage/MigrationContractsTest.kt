package com.gamebox.os.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationContractsTest {
    @Test fun plannerDeduplicatesAndSanitizesDestination() {
        val plan = ContentMigrationPlanner.plan(listOf(ContentMigrationItem("a/b", "save.bin", 10), ContentMigrationItem("a/b", "save.bin", 10), ContentMigrationItem("other", "cfg/state", 2)))
        assertEquals(2, plan.items.size); assertEquals(12, plan.totalBytes)
        assertTrue(plan.items.first().destinationRelativePath.startsWith("a_b/"))
    }
    @Test(expected = IllegalArgumentException::class) fun plannerRejectsTraversal() {
        ContentMigrationPlanner.plan(listOf(ContentMigrationItem("game", "../private", 1)))
    }
    @Test fun capacityAndRetryableResultsRemainExplicit() {
        val plan = ContentMigrationPlanner.plan(listOf(ContentMigrationItem("game", "save", 10)))
        assertTrue(MigrationCapacityChecker.check(plan, 10).hasCapacity)
        val result = ContentMigrationExecutor { Result.failure(ExternalStorageUnavailableException("removed")) }.execute(plan)
        assertEquals(1, result.retryableCount); assertEquals(MigrationItemStatus.RETRYABLE, result.items.single().status)
    }
}

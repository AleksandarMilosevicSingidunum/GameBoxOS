package com.gamebox.os

import com.gamebox.os.storage.ContentMigrationItem
import com.gamebox.os.storage.ContentMigrationPlanner
import com.gamebox.os.storage.MigrationCapacityChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationCapacityCheckerTest {
    @Test
    fun reportsInsufficientCapacityWithoutMutatingPlan() {
        val plan = ContentMigrationPlanner.plan(listOf(ContentMigrationItem("game", "content.bin", 100)))
        assertFalse(MigrationCapacityChecker.check(plan, 99).hasCapacity)
        assertTrue(MigrationCapacityChecker.check(plan, 100).hasCapacity)
    }
}
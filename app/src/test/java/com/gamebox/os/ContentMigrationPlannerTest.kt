package com.gamebox.os

import com.gamebox.os.storage.ContentMigrationItem
import com.gamebox.os.storage.ContentMigrationPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentMigrationPlannerTest {
    @Test
    fun deduplicatesItemsAndTotalsBytes() {
        val plan = ContentMigrationPlanner.plan(listOf(ContentMigrationItem("g", "a.bin", 5), ContentMigrationItem("g", "a.bin", 5)))
        assertEquals(1, plan.items.size)
        assertEquals(5L, plan.totalBytes)
    }

    @Test
    fun rejectsTraversalPaths() {
        assertThrows(IllegalArgumentException::class.java) { ContentMigrationPlanner.plan(listOf(ContentMigrationItem("g", "../a.bin", 1))) }
    }
}
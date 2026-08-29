package com.gamebox.os.storage

import kotlin.test.Test
import kotlin.test.assertEquals

class MigrationConfirmationTest {
    private val plan = ContentMigrationPlanner.plan(listOf(ContentMigrationItem("game-1", "data/save.bin", 42)))
    @Test fun requiresExplicitConfirmationBeforeCopy() {
        val r = MigrationConfirmationEvaluator.evaluate(ExternalStorageStatus(ExternalStorageState.AVAILABLE_READ_WRITE, "SSD", "ok"), plan, false)
        assertEquals(MigrationConfirmationState.NEEDS_CONFIRMATION, r.state)
    }
    @Test fun allowsConfirmedCopyOnlyOnWritableStorage() {
        val r = MigrationConfirmationEvaluator.evaluate(ExternalStorageStatus(ExternalStorageState.AVAILABLE_READ_WRITE, "SSD", "ok"), plan, true)
        assertEquals(MigrationConfirmationState.READY, r.state)
    }
    @Test fun blocksReadOnlyAndDisconnectedStorage() {
        assertEquals(MigrationConfirmationState.BLOCKED_READ_ONLY, MigrationConfirmationEvaluator.evaluate(ExternalStorageStatus(ExternalStorageState.AVAILABLE_READ_ONLY, "SD", "read-only"), plan, true).state)
        assertEquals(MigrationConfirmationState.BLOCKED_DISCONNECTED, MigrationConfirmationEvaluator.evaluate(ExternalStorageStatus(ExternalStorageState.DISCONNECTED, message = "gone"), plan, true).state)
    }
    @Test fun exposesRetryableStateAfterDisconnectDuringCopy() {
        val previous = ContentMigrationResult(listOf(MigrationItemResult(plan.items.single(), MigrationItemStatus.RETRYABLE, "gone")))
        val r = MigrationConfirmationEvaluator.evaluate(ExternalStorageStatus(ExternalStorageState.AVAILABLE_READ_WRITE, "SSD", "ok"), plan, true, previous)
        assertEquals(MigrationConfirmationState.RETRYABLE, r.state)
    }
}

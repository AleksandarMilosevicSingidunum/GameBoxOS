package com.gamebox.os.storage

enum class MigrationConfirmationState {
    NEEDS_CONFIRMATION, BLOCKED_READ_ONLY, BLOCKED_DISCONNECTED, READY, RETRYABLE, COMPLETE
}

data class MigrationConfirmation(val state: MigrationConfirmationState, val message: String)

object MigrationConfirmationEvaluator {
    fun evaluate(status: ExternalStorageStatus, plan: ContentMigrationPlan, confirmed: Boolean, previousResult: ContentMigrationResult? = null): MigrationConfirmation {
        if (plan.isEmpty) return MigrationConfirmation(MigrationConfirmationState.COMPLETE, "Nothing to migrate")
        if ((previousResult?.retryableCount ?: 0) > 0) return MigrationConfirmation(MigrationConfirmationState.RETRYABLE, "External library was unavailable; reconnect it and retry")
        return when (status.state) {
            ExternalStorageState.AVAILABLE_READ_WRITE -> if (confirmed) MigrationConfirmation(MigrationConfirmationState.READY, "Migration confirmed") else MigrationConfirmation(MigrationConfirmationState.NEEDS_CONFIRMATION, "Confirm copying " + plan.totalBytes + " bytes to " + (status.displayName ?: "external library"))
            ExternalStorageState.AVAILABLE_READ_ONLY -> MigrationConfirmation(MigrationConfirmationState.BLOCKED_READ_ONLY, "External library is read-only")
            ExternalStorageState.DISCONNECTED, ExternalStorageState.PERMISSION_MISSING -> MigrationConfirmation(MigrationConfirmationState.BLOCKED_DISCONNECTED, "Reconnect or re-authorize the external library")
            else -> MigrationConfirmation(MigrationConfirmationState.BLOCKED_DISCONNECTED, status.message)
        }
    }
}

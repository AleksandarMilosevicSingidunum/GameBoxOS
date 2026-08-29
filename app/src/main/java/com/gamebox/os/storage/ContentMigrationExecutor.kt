package com.gamebox.os.storage

enum class MigrationItemStatus { COPIED, SKIPPED, FAILED, RETRYABLE }

/** Signals that removable storage is unavailable and the item can be retried safely. */
class ExternalStorageUnavailableException(message: String) : IllegalStateException(message)
data class MigrationItemResult(val item: ContentMigrationItem, val status: MigrationItemStatus, val message: String? = null)
data class ContentMigrationResult(val items: List<MigrationItemResult>) {
    val copiedCount get() = items.count { it.status == MigrationItemStatus.COPIED }
    val failedCount get() = items.count { it.status == MigrationItemStatus.FAILED }\n    val retryableCount get() = items.count { it.status == MigrationItemStatus.RETRYABLE }
}

fun interface ContentCopyOperation {
    fun copy(item: ContentMigrationItem): Result<Unit>
}

class ContentMigrationExecutor(private val copyOperation: ContentCopyOperation) {
    fun execute(plan: ContentMigrationPlan): ContentMigrationResult =
        ContentMigrationResult(plan.items.map { item ->
            copyOperation.copy(item).fold(
                onSuccess = { MigrationItemResult(item, MigrationItemStatus.COPIED) },
                onFailure = { error ->
                    val status = if (error is ExternalStorageUnavailableException) MigrationItemStatus.RETRYABLE else MigrationItemStatus.FAILED
                    MigrationItemResult(item, status, error.message ?: "copy failed")
                }
            )
        })
}
package com.gamebox.os.storage

enum class MigrationItemStatus { COPIED, SKIPPED, FAILED }
data class MigrationItemResult(val item: ContentMigrationItem, val status: MigrationItemStatus, val message: String? = null)
data class ContentMigrationResult(val items: List<MigrationItemResult>) {
    val copiedCount get() = items.count { it.status == MigrationItemStatus.COPIED }
    val failedCount get() = items.count { it.status == MigrationItemStatus.FAILED }
}

fun interface ContentCopyOperation {
    fun copy(item: ContentMigrationItem): Result<Unit>
}

class ContentMigrationExecutor(private val copyOperation: ContentCopyOperation) {
    fun execute(plan: ContentMigrationPlan): ContentMigrationResult =
        ContentMigrationResult(plan.items.map { item ->
            copyOperation.copy(item).fold(
                onSuccess = { MigrationItemResult(item, MigrationItemStatus.COPIED) },
                onFailure = { MigrationItemResult(item, MigrationItemStatus.FAILED, it.message ?: "copy failed") }
            )
        })
}
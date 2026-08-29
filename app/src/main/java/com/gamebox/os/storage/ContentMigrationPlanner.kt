package com.gamebox.os.storage

data class ContentMigrationItem(val gameId: String, val relativePath: String, val sizeBytes: Long) {
    val destinationRelativePath: String
        get() = gameId.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_').ifBlank { "game" } + "/" + relativePath.trimStart('/')
}

data class ContentMigrationPlan(val items: List<ContentMigrationItem>, val totalBytes: Long) {
    val isEmpty: Boolean get() = items.isEmpty()
}

object ContentMigrationPlanner {
    fun plan(items: List<ContentMigrationItem>): ContentMigrationPlan {
        require(items.all { it.gameId.isNotBlank() && it.relativePath.isNotBlank() && it.sizeBytes >= 0 }) {
            "migration items must have valid metadata"
        }
        require(items.all { !it.relativePath.split('/').any { segment -> segment == ".." } }) {
            "migration paths must not contain traversal segments"
        }
        return ContentMigrationPlan(items.distinctBy { it.gameId to it.relativePath }, items.sumOf { it.sizeBytes })
    }
}
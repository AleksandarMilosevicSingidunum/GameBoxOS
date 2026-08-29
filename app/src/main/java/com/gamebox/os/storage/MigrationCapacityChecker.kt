package com.gamebox.os.storage

data class MigrationCapacity(val availableBytes: Long, val requiredBytes: Long) {
    val hasCapacity: Boolean get() = availableBytes >= requiredBytes
}

object MigrationCapacityChecker {
    fun check(plan: ContentMigrationPlan, availableBytes: Long): MigrationCapacity {
        require(availableBytes >= 0) { "available capacity must not be negative" }
        return MigrationCapacity(availableBytes, plan.totalBytes)
    }
}
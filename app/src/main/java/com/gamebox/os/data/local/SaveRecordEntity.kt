package com.gamebox.os.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "save_records")
data class SaveRecordEntity(
    @PrimaryKey val gameId: String,
    val relativePath: String,
    val updatedAtMillis: Long,
    val sizeBytes: Long
)

package com.gamebox.os.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveRecordDao {
    @Query("SELECT * FROM save_records WHERE gameId = :gameId LIMIT 1")
    fun observe(gameId: String): Flow<SaveRecordEntity?>

    @Upsert
    suspend fun upsert(record: SaveRecordEntity)
}

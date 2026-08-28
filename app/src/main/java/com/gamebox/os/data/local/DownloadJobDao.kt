package com.gamebox.os.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadJobDao {
    @Query("SELECT * FROM download_jobs ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<DownloadJobEntity>>

    @Upsert
    suspend fun upsert(job: DownloadJobEntity)

    @Query("UPDATE download_jobs SET status = :status, downloadedBytes = :downloadedBytes, errorReason = :errorReason WHERE id = :id")
    suspend fun updateState(id: String, status: String, downloadedBytes: Long, errorReason: String?)
}

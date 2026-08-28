package com.gamebox.os.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId

@Entity(tableName = "download_jobs")
data class DownloadJobEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val title: String,
    val status: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val errorReason: String?
)

fun DownloadJobEntity.toDomain(): DownloadJob = DownloadJob(
    id = id,
    gameId = GameId(gameId),
    title = title,
    status = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.FAILED),
    totalBytes = totalBytes,
    downloadedBytes = downloadedBytes,
    errorReason = errorReason
)

fun Game.toDownloadEntity(): DownloadJobEntity = DownloadJobEntity(
    id = id.value,
    gameId = id.value,
    title = title,
    status = DownloadStatus.QUEUED.name,
    totalBytes = sizeMb.toLong() * 1024L * 1024L,
    downloadedBytes = 0L,
    errorReason = null
)

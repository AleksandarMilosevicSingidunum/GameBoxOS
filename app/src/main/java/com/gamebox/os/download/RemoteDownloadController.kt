package com.gamebox.os.download

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gamebox.os.data.DownloadRepository
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

interface RemoteDownloadController {
    fun install(game: Game)
    fun cancel(game: Game)
}

class WorkManagerRemoteDownloadController(
    context: Context,
    private val gameRepository: GameRepository,
    private val downloadRepository: DownloadRepository,
    scope: CoroutineScope
) : RemoteDownloadController {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val scheduler = RemoteDownloadScheduler(context.applicationContext)

    init {
        scope.launch {
            workManager.getWorkInfosByTagFlow(RemoteDownloadWorker.TAG).collect { workInfos ->
                workInfos.forEach(::reconcile)
            }
        }
    }

    override fun install(game: Game) {
        require(game.sourceUrl != null && game.expectedSha256 != null) {
            "Game has no verified remote source"
        }
        downloadRepository.enqueue(game)
        gameRepository.setInstallState(game.id, InstallState.QUEUED)
        scheduler.enqueue(game)
    }

    override fun cancel(game: Game) {
        scheduler.cancel(game)
        downloadRepository.updateState(game.id, DownloadStatus.CANCELLED, 0L, "cancelled")
        gameRepository.setInstallState(game.id, InstallState.NOT_INSTALLED)
    }

    private fun reconcile(info: WorkInfo) {
        val gameId = info.tags.firstNotNullOfOrNull { tag ->
            tag.removePrefix(GAME_TAG_PREFIX).takeIf { tag.startsWith(GAME_TAG_PREFIX) && it.isNotBlank() }
        }?.let(::GameId) ?: return
        val job = downloadRepository.observeJobs().value.firstOrNull { it.gameId == gameId } ?: return
        val bytes = when (info.state) {
            WorkInfo.State.SUCCEEDED -> info.outputData.getLong(RemoteDownloadWorker.KEY_BYTES_TRANSFERRED, job.totalBytes)
            else -> info.progress.getLong(RemoteDownloadWorker.KEY_BYTES_TRANSFERRED, job.downloadedBytes)
        }
        val error = info.outputData.getString(RemoteDownloadWorker.KEY_ERROR)
        val downloadStatus = when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadStatus.QUEUED
            WorkInfo.State.RUNNING -> DownloadStatus.DOWNLOADING
            WorkInfo.State.SUCCEEDED -> DownloadStatus.COMPLETED
            WorkInfo.State.FAILED -> DownloadStatus.FAILED
            WorkInfo.State.CANCELLED -> DownloadStatus.CANCELLED
        }
        downloadRepository.updateState(gameId, downloadStatus, bytes, error)
        gameRepository.setInstallState(
            gameId,
            when (downloadStatus) {
                DownloadStatus.QUEUED -> InstallState.QUEUED
                DownloadStatus.DOWNLOADING -> InstallState.DOWNLOADING
                DownloadStatus.PAUSED -> InstallState.PAUSED
                DownloadStatus.VERIFYING -> InstallState.VERIFYING
                DownloadStatus.INSTALLING -> InstallState.INSTALLING
                DownloadStatus.COMPLETED -> InstallState.INSTALLED
                DownloadStatus.FAILED -> InstallState.FAILED
                DownloadStatus.CANCELLED -> InstallState.NOT_INSTALLED
            }
        )
    }

    private companion object {
        const val GAME_TAG_PREFIX = RemoteDownloadWorker.TAG + ":"
    }
}

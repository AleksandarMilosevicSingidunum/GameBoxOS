package com.gamebox.os.data

import com.gamebox.os.data.local.DownloadJobDao
import com.gamebox.os.data.local.toDomain
import com.gamebox.os.data.local.toDownloadEntity
import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface DownloadRepository {
    fun observeJobs(): StateFlow<List<DownloadJob>>
    fun enqueue(game: Game)
    fun pause(id: GameId)
    fun resume(id: GameId)
    fun cancel(id: GameId)
    fun advance(id: GameId)
}

class RoomDownloadRepository(
    private val dao: DownloadJobDao,
    private val scope: CoroutineScope
) : DownloadRepository {
    private val jobs = dao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun observeJobs(): StateFlow<List<DownloadJob>> = jobs

    override fun enqueue(game: Game) {
        scope.launch { dao.upsert(game.toDownloadEntity()) }
    }

    override fun pause(id: GameId) = update(id) { job ->
        if (job.status == DownloadStatus.DOWNLOADING) job.copy(status = DownloadStatus.PAUSED) else job
    }

    override fun resume(id: GameId) = update(id) { job ->
        if (job.status == DownloadStatus.PAUSED) job.copy(status = DownloadStatus.DOWNLOADING) else job
    }

    override fun cancel(id: GameId) = update(id) { job ->
        if (job.status in terminalStatuses) job else job.copy(status = DownloadStatus.CANCELLED)
    }

    override fun advance(id: GameId) = update(id) { job ->
        when (job.status) {
            DownloadStatus.QUEUED -> job.copy(status = DownloadStatus.DOWNLOADING)
            DownloadStatus.DOWNLOADING -> job.copy(
                status = DownloadStatus.VERIFYING,
                downloadedBytes = job.totalBytes
            )
            DownloadStatus.PAUSED -> job
            DownloadStatus.VERIFYING -> job.copy(status = DownloadStatus.INSTALLING)
            DownloadStatus.INSTALLING -> job.copy(status = DownloadStatus.COMPLETED)
            else -> job
        }
    }

    private fun update(id: GameId, transform: (DownloadJob) -> DownloadJob) {
        val current = jobs.value.firstOrNull { it.gameId == id } ?: return
        val next = transform(current)
        scope.launch {
            dao.updateState(next.id, next.status.name, next.downloadedBytes, next.errorReason)
        }
    }

    private companion object {
        val terminalStatuses = setOf(
            DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED
        )
    }
}

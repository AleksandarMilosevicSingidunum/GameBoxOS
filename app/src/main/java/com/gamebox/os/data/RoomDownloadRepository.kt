package com.gamebox.os.data

import com.gamebox.os.data.local.DownloadJobDao
import com.gamebox.os.data.local.toDomain
import com.gamebox.os.data.local.toDownloadEntity
import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface DownloadRepository {
    fun observeJobs(): StateFlow<List<DownloadJob>>
    fun enqueue(game: Game)
    fun pause(id: GameId)
    fun resume(id: GameId)
    fun cancel(id: GameId)
    fun updateState(id: GameId, status: DownloadStatus, downloadedBytes: Long, errorReason: String? = null)
}

class RoomDownloadRepository(
    private val dao: DownloadJobDao,
    private val scope: CoroutineScope
) : DownloadRepository {
    private val writes = Mutex()
    private val jobs = dao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun observeJobs(): StateFlow<List<DownloadJob>> = jobs

    override fun enqueue(game: Game) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            writes.withLock { dao.upsert(game.toDownloadEntity()) }
        }
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

    override fun updateState(
        id: GameId,
        status: DownloadStatus,
        downloadedBytes: Long,
        errorReason: String?
    ) = update(id) { job ->
        job.copy(
            status = status,
            downloadedBytes = downloadedBytes.coerceIn(0L, job.totalBytes.coerceAtLeast(downloadedBytes)),
            errorReason = errorReason
        )
    }

    private fun update(id: GameId, transform: (DownloadJob) -> DownloadJob) {
        // Enter the write queue immediately, including before Room's first UI emission.
        // Read after prior writes finish so rapid enqueue/progress/pause cannot lose state.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            writes.withLock {
                val current = dao.getByGameId(id.value)?.toDomain() ?: return@withLock
                val next = transform(current)
                if (next != current) {
                    dao.updateState(next.id, next.status.name, next.downloadedBytes, next.errorReason)
                }
            }
        }
    }

    private companion object {
        val terminalStatuses = setOf(
            DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED
        )
    }
}

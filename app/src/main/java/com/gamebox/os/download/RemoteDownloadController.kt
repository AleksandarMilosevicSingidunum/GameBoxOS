package com.gamebox.os.download

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gamebox.os.data.DownloadRepository
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.content.GameContentPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

interface RemoteDownloadController {
    fun install(game: Game)
    fun pause(game: Game)
    fun resume(game: Game)
    fun cancel(game: Game)
}

class WorkManagerRemoteDownloadController(
    context: Context,
    private val gameRepository: GameRepository,
    private val downloadRepository: DownloadRepository,
    scope: CoroutineScope,
    private val downloadsUnmeteredOnly: () -> Boolean = { true },
    workInfoFlow: Flow<List<WorkInfo>> = WorkManager.getInstance(context.applicationContext)
        .getWorkInfosByTagFlow(RemoteDownloadWorker.TAG)
) : RemoteDownloadController {
    private val scheduler = RemoteDownloadScheduler(context.applicationContext)
    private val contentValidator = InstalledContentValidator(
        context.applicationContext.filesDir.resolve(AssetDownloadWorker.INSTALL_ROOT)
    )
    private val pausedGameIds = ConcurrentHashMap.newKeySet<String>()

    init {
        scope.launch {
            val applied = mutableMapOf<GameId, WorkInfo>()
            combine(
                workInfoFlow,
                downloadRepository.observeJobs(),
                gameRepository.observeGames()
            ) { workInfos, jobs, games -> Triple(workInfos, jobs, games.associateBy { it.id }) }
                .collect { (workInfos, jobs, gamesById) ->
                val jobsByGame = jobs.associateBy { it.gameId }
                val presentGameIds = workInfos.mapNotNull(::gameIdFrom).toSet()
                applied.keys.retainAll(presentGameIds)
                workInfos
                    .mapNotNull { info -> gameIdFrom(info)?.let { it.value to info } }
                    .groupBy({ it.first }, { it.second })
                    .values
                    .mapNotNull { it.lastOrNull() }
                    .forEach { info ->
                        val id = gameIdFrom(info) ?: return@forEach
                        val job = jobsByGame[id] ?: return@forEach
                        val game = gamesById[id] ?: return@forEach
                        if (applied[id] == info) return@forEach
                        // A retained successful WorkManager record is history, not
                        // permission to undo an uninstall after the app restarts.
                        if (info.state == WorkInfo.State.SUCCEEDED &&
                            job.status == DownloadStatus.COMPLETED &&
                            game.state == InstallState.NOT_INSTALLED
                        ) {
                            applied[id] = info
                            return@forEach
                        }
                        // Defer early WorkManager snapshots until both Room rows exist.
                        // Cache only applied work snapshots, not UI/database emissions:
                        // otherwise our own writes (or an uninstall) replay old success.
                        reconcile(info, job, game)
                        applied[id] = info
                    }
            }
        }
    }

    override fun install(game: Game) {
        require(game.sourceUrl != null && game.expectedSha256 != null) {
            "Game has no verified remote source"
        }
        pausedGameIds.remove(game.id.value)
        downloadRepository.enqueue(game)
        gameRepository.setInstallState(game.id, InstallState.QUEUED)
        scheduler.enqueue(game, unmeteredOnly = downloadsUnmeteredOnly())
    }

    override fun pause(game: Game) {
        val job = downloadRepository.observeJobs().value.firstOrNull { it.gameId == game.id } ?: return
        pausedGameIds.add(game.id.value)
        downloadRepository.updateState(game.id, DownloadStatus.PAUSED, job.downloadedBytes, null)
        gameRepository.setInstallState(game.id, InstallState.PAUSED)
        scheduler.cancel(game)
    }

    override fun resume(game: Game) {
        pausedGameIds.remove(game.id.value)
        val job = downloadRepository.observeJobs().value.firstOrNull { it.gameId == game.id }
        downloadRepository.updateState(
            game.id,
            DownloadStatus.QUEUED,
            job?.downloadedBytes ?: 0L,
            null
        )
        gameRepository.setInstallState(game.id, InstallState.QUEUED)
        scheduler.enqueue(game, replace = true, unmeteredOnly = downloadsUnmeteredOnly())
    }

    override fun cancel(game: Game) {
        pausedGameIds.remove(game.id.value)
        scheduler.cancel(game)
        scheduler.discardPartial(game)
        downloadRepository.updateState(game.id, DownloadStatus.CANCELLED, 0L, "cancelled")
        gameRepository.setInstallState(game.id, InstallState.NOT_INSTALLED)
    }

    private fun reconcile(info: WorkInfo, job: DownloadJob, game: Game) {
        val gameId = gameIdFrom(info) ?: return
        if (info.state == WorkInfo.State.SUCCEEDED) {
            val contentStatus = try {
                val checksum = requireNotNull(game.expectedSha256)
                val content = GameContentPolicy.describe(game.id.value, game.platform, game.sourceUrl)
                contentValidator.validate(content.relativePath, checksum)
            } catch (_: java.io.IOException) {
                InstalledContentStatus.ALTERED
            } catch (_: SecurityException) {
                InstalledContentStatus.ALTERED
            } catch (_: IllegalArgumentException) {
                InstalledContentStatus.ALTERED
            }
            if (contentStatus != InstalledContentStatus.VERIFIED) {
                val missing = contentStatus == InstalledContentStatus.MISSING
                downloadRepository.updateState(gameId, DownloadStatus.FAILED, 0L,
                    if (missing) "Installed content is missing; reinstall required"
                    else "Installed content could not be verified; reinstall required")
                gameRepository.setInstallState(gameId,
                    if (missing) InstallState.MISSING_FILES else InstallState.FAILED)
                return
            }
        }
        val bytes = when (info.state) {
            WorkInfo.State.SUCCEEDED -> info.outputData.getLong(RemoteDownloadWorker.KEY_BYTES_TRANSFERRED, job.totalBytes)
            else -> info.progress.getLong(RemoteDownloadWorker.KEY_BYTES_TRANSFERRED, job.downloadedBytes)
        }
        val error = info.outputData.getString(RemoteDownloadWorker.KEY_ERROR)
        val downloadStatus = when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadStatus.QUEUED
            WorkInfo.State.RUNNING -> DownloadStatus.DOWNLOADING
            WorkInfo.State.SUCCEEDED -> DownloadStatus.COMPLETED
            WorkInfo.State.FAILED ->
                if (pausedGameIds.contains(gameId.value) || job.status == DownloadStatus.PAUSED)
                    DownloadStatus.PAUSED else DownloadStatus.FAILED
            WorkInfo.State.CANCELLED ->
                if (pausedGameIds.contains(gameId.value) || job.status == DownloadStatus.PAUSED)
                    DownloadStatus.PAUSED else DownloadStatus.CANCELLED
        }
        if (downloadStatus == DownloadStatus.COMPLETED) pausedGameIds.remove(gameId.value)
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

    private fun gameIdFrom(info: WorkInfo): GameId? =
        info.tags.firstNotNullOfOrNull { tag ->
            tag.removePrefix(GAME_TAG_PREFIX)
                .takeIf { tag.startsWith(GAME_TAG_PREFIX) && it.isNotBlank() }
        }?.let(::GameId)

    private companion object {
        const val GAME_TAG_PREFIX = RemoteDownloadWorker.TAG + ":"
    }
}

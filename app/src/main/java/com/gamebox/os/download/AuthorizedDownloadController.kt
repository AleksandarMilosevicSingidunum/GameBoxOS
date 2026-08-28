package com.gamebox.os.download

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthorizedDownloadState(
    val status: Status = Status.IDLE,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = AuthorizedTestDownload.SIZE_BYTES,
    val error: String? = null
) {
    enum class Status { IDLE, QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED }
    val progress: Float
        get() = if (totalBytes <= 0L) 0f
        else (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

interface AuthorizedDownloadController {
    fun observeState(): StateFlow<AuthorizedDownloadState>
    fun install()
    fun cancel()
}

class WorkManagerAuthorizedDownloadController(
    context: Context,
    private val gameRepository: GameRepository,
    scope: CoroutineScope
) : AuthorizedDownloadController {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val scheduler = DownloadWorkScheduler(context)
    private val state = workManager
        .getWorkInfosForUniqueWorkFlow(AuthorizedTestDownload.UNIQUE_WORK_NAME)
        .map { workInfos -> workInfos.lastOrNull().toAuthorizedState() }
        .stateIn(scope, SharingStarted.Eagerly, AuthorizedDownloadState())

    init {
        scope.launch {
            state.collect { current ->
                current.status.toInstallState()?.let {
                    gameRepository.setInstallState(TEST_GAME_ID, it)
                }
            }
        }
    }

    override fun observeState(): StateFlow<AuthorizedDownloadState> = state

    override fun install() {
        gameRepository.setInstallState(TEST_GAME_ID, InstallState.QUEUED)
        scheduler.enqueueAuthorizedTest()
    }

    override fun cancel() {
        workManager.cancelUniqueWork(AuthorizedTestDownload.UNIQUE_WORK_NAME)
    }

    private fun WorkInfo?.toAuthorizedState(): AuthorizedDownloadState {
        if (this == null) return AuthorizedDownloadState()
        val transferred = if (state == WorkInfo.State.SUCCEEDED) {
            outputData.getLong(AssetDownloadWorker.KEY_BYTES_TRANSFERRED, 0L)
        } else {
            progress.getLong(AssetDownloadWorker.KEY_BYTES_TRANSFERRED, 0L)
        }
        val total = progress.getLong(
            AssetDownloadWorker.KEY_TOTAL_BYTES,
            AuthorizedTestDownload.SIZE_BYTES
        ).takeIf { it > 0L } ?: AuthorizedTestDownload.SIZE_BYTES
        return AuthorizedDownloadState(
            status = when (state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> AuthorizedDownloadState.Status.QUEUED
                WorkInfo.State.RUNNING -> AuthorizedDownloadState.Status.RUNNING
                WorkInfo.State.SUCCEEDED -> AuthorizedDownloadState.Status.SUCCEEDED
                WorkInfo.State.FAILED -> AuthorizedDownloadState.Status.FAILED
                WorkInfo.State.CANCELLED -> AuthorizedDownloadState.Status.CANCELLED
            },
            bytesTransferred = transferred,
            totalBytes = total,
            error = outputData.getString(AssetDownloadWorker.KEY_ERROR)
        )
    }

    private fun AuthorizedDownloadState.Status.toInstallState(): InstallState? = when (this) {
        AuthorizedDownloadState.Status.IDLE -> null
        AuthorizedDownloadState.Status.QUEUED -> InstallState.QUEUED
        AuthorizedDownloadState.Status.RUNNING -> InstallState.DOWNLOADING
        AuthorizedDownloadState.Status.SUCCEEDED -> InstallState.INSTALLED
        AuthorizedDownloadState.Status.FAILED -> InstallState.FAILED
        AuthorizedDownloadState.Status.CANCELLED -> InstallState.NOT_INSTALLED
    }

    private companion object {
        val TEST_GAME_ID = GameId("retro-test")
    }
}

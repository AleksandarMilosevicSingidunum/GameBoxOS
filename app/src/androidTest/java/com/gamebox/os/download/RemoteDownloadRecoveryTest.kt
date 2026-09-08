package com.gamebox.os.download

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.WorkInfo
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.data.DownloadRepository
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Deterministic controller/flow regression; not a live provider transfer test. */
@RunWith(AndroidJUnit4::class)
class RemoteDownloadRecoveryTest {
    @Test fun waitsForBothRowsAndDoesNotReplayUnchangedWork() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val id = GameId("recovery-test-" + UUID.randomUUID())
        val payload = "authorized regression content".toByteArray()
        val checksum = java.security.MessageDigest.getInstance("SHA-256")
            .digest(payload).joinToString("") { "%02x".format(it) }
        val game = Game(id, "Recovery", "Homebrew", 2026, "Test", 1, InstallState.QUEUED,
            sourceUrl = "https://example.com/recovery.nes", expectedSha256 = checksum)
        val descriptor = com.gamebox.os.content.GameContentPolicy.describe(id.value, game.platform, game.sourceUrl)
        val content = app.filesDir.resolve(AssetDownloadWorker.INSTALL_ROOT).resolve(descriptor.relativePath)
        content.parentFile?.mkdirs()
        content.writeBytes(payload)
        val job = DownloadJob(id.value, id, game.title, DownloadStatus.QUEUED, 1024, 0)
        val games = MutableStateFlow<List<Game>>(emptyList())
        val jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
        val gameWrites = mutableListOf<InstallState>()
        val jobWrites = mutableListOf<DownloadStatus>()
        val gameRepository = object : GameRepository by app.container.gameRepository {
            override fun observeGames() = games
            override fun setInstallState(id: GameId, state: InstallState) { gameWrites += state }
        }
        val downloadRepository = object : DownloadRepository by app.container.downloadRepository {
            override fun observeJobs() = jobs
            override fun updateState(id: GameId, status: DownloadStatus, downloadedBytes: Long, errorReason: String?) {
                jobWrites += status
            }
        }
        val workId = UUID.randomUUID()
        val tags = setOf(RemoteDownloadWorker.TAG, RemoteDownloadWorker.TAG + ":" + id.value)
        fun info(state: WorkInfo.State) = WorkInfo(workId, state, tags, Data.EMPTY, Data.EMPTY, 0)
        val work = MutableStateFlow(listOf(info(WorkInfo.State.SUCCEEDED)))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            WorkManagerRemoteDownloadController(app, gameRepository, downloadRepository, scope, work)
            yield()
            assertTrue(gameWrites.isEmpty())
            jobs.value = listOf(job)
            yield()
            assertTrue(jobWrites.isEmpty())
            games.value = listOf(game)
            yield()
            assertEquals(listOf(InstallState.INSTALLED), gameWrites)
            assertEquals(listOf(DownloadStatus.COMPLETED), jobWrites)

            games.value = listOf(game.copy(state = InstallState.NOT_INSTALLED))
            jobs.value = listOf(job.copy(status = DownloadStatus.COMPLETED))
            yield()
            assertEquals(1, gameWrites.size)
            assertEquals(1, jobWrites.size)

            // A new controller has no in-memory replay cache. Persisted uninstall
            // state must still win over the previous successful download record.
            scope.coroutineContext[Job]?.cancelAndJoin()
            val restartedScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                WorkManagerRemoteDownloadController(app, gameRepository, downloadRepository, restartedScope, work)
                yield()
                assertEquals(1, gameWrites.size)
                assertEquals(1, jobWrites.size)

                work.value = listOf(WorkInfo(UUID.randomUUID(), WorkInfo.State.RUNNING,
                    tags, Data.EMPTY, Data.EMPTY, 0))
                yield()
                assertEquals(InstallState.DOWNLOADING, gameWrites.last())
                assertEquals(2, gameWrites.size)

                games.value = listOf(game.copy(state = InstallState.INSTALLED))
                content.delete()
                work.value = listOf(WorkInfo(UUID.randomUUID(), WorkInfo.State.SUCCEEDED,
                    tags, Data.EMPTY, Data.EMPTY, 0))
                yield()
                assertEquals(InstallState.MISSING_FILES, gameWrites.last())
                assertEquals(DownloadStatus.FAILED, jobWrites.last())

                content.writeText("changed content")
                work.value = listOf(WorkInfo(UUID.randomUUID(), WorkInfo.State.SUCCEEDED,
                    tags, Data.EMPTY, Data.EMPTY, 0))
                yield()
                assertEquals(InstallState.FAILED, gameWrites.last())
                assertEquals(DownloadStatus.FAILED, jobWrites.last())
            } finally {
                restartedScope.coroutineContext[Job]?.cancelAndJoin()
            }
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
            content.delete()
        }
    }
}

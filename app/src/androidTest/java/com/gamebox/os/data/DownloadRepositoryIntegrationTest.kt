package com.gamebox.os.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.data.local.GameBoxDatabase
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadRepositoryIntegrationTest {
    @Test fun immediateUpdatesSurviveBeforeFirstObservedRow() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), GameBoxDatabase::class.java
        ).build()
        val executor = Executors.newSingleThreadExecutor()
        val gate = CountDownLatch(1)
        executor.submit { gate.await() }
        val dispatcher = executor.asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        try {
            val repository = RoomDownloadRepository(database.downloadJobDao(), scope)
            val game = Game(GameId("ordered-download"), "Ordered download", "Homebrew",
                2026, "Test", 1, InstallState.NOT_INSTALLED)

            // Hold UI observation until every command has been submitted. The old
            // cached-list implementation drops all updates here deterministically.
            repository.enqueue(game)
            repository.updateState(game.id, DownloadStatus.DOWNLOADING, 256)
            repository.pause(game.id)
            repository.resume(game.id)
            repository.updateState(game.id, DownloadStatus.COMPLETED, 1024)
            assertTrue(repository.observeJobs().value.isEmpty())
            gate.countDown()

            val job = withTimeout(10_000) {
                repository.observeJobs().first { rows ->
                    rows.singleOrNull()?.status == DownloadStatus.COMPLETED
                }.single()
            }
            assertEquals(1024L, job.downloadedBytes)
            assertEquals(DownloadStatus.COMPLETED, job.status)
            val stored = database.downloadJobDao().getByGameId(game.id.value)
            assertEquals(DownloadStatus.COMPLETED.name, stored?.status)
            assertEquals(1024L, stored?.downloadedBytes)
        } finally {
            gate.countDown()
            scope.coroutineContext[Job]?.cancelAndJoin()
            dispatcher.close()
            database.close()
        }
    }
}

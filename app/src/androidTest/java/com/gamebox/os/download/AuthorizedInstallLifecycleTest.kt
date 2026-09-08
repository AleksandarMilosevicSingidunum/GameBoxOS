package com.gamebox.os.download

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real WorkManager + Room + files; the save is synthetic, not emulator gameplay evidence. */
@RunWith(AndroidJUnit4::class)
class AuthorizedInstallLifecycleTest {
    @Test fun installUninstallReinstallPreservesSaveAndBackup() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val container = app.container
        val gameId = GameId("galaxy-patrol")
        val content = app.filesDir.resolve(AssetDownloadWorker.INSTALL_ROOT)
            .resolve(AuthorizedHomebrewDownload.RELATIVE_PATH)
        val save = app.filesDir.resolve("saves/galaxy-patrol/save.dat")
        // Never alter a pre-existing installation or save when run on a developer device.
        assumeTrue("Lifecycle test requires fresh app data", !content.exists() && !save.exists())
        withTimeout(20_000) {
            container.gameRepository.observeGames().first { games -> games.any { it.id == gameId } }
        }
        assertEquals(InstallState.NOT_INSTALLED, container.gameRepository.game(gameId)?.state)
        val validator = InstalledContentValidator(app.filesDir.resolve(AssetDownloadWorker.INSTALL_ROOT))

        suspend fun installAndVerify() {
            container.authorizedDownloadController.install()
            withTimeout(30_000) {
                while (!content.isFile ||
                    container.authorizedDownloadController.observeState().value.status != AuthorizedDownloadState.Status.SUCCEEDED ||
                    container.gameRepository.game(gameId)?.state != InstallState.INSTALLED
                ) delay(50)
            }
            assertEquals(AuthorizedHomebrewDownload.SIZE_BYTES, content.length())
            assertEquals(InstalledContentStatus.VERIFIED,
                validator.validate(AuthorizedHomebrewDownload.RELATIVE_PATH, AuthorizedHomebrewDownload.SHA256))
        }
        installAndVerify()

        // Restore real completed WorkManager state while the library is still loading.
        // Delegate everything except the delayed read and captured installation write.
        val delayedGames = MutableStateFlow<List<Game>>(emptyList())
        val restoredWrites = Channel<InstallState>(Channel.UNLIMITED)
        val delayedRepository = object : GameRepository by container.gameRepository {
            override fun observeGames() = delayedGames
            override fun setInstallState(id: GameId, state: InstallState) {
                assertEquals(gameId, id)
                restoredWrites.trySend(state)
            }
        }
        val restoreScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val restoredController = WorkManagerAuthorizedDownloadController(
                app, delayedRepository, restoreScope
            )
            withTimeout(10_000) {
                restoredController.observeState().first {
                    it.status == AuthorizedDownloadState.Status.SUCCEEDED
                }
            }
            assertTrue("No write before the target row exists", restoredWrites.tryReceive().isFailure)
            delayedGames.value = container.gameRepository.observeGames().value
            assertEquals(InstallState.INSTALLED, withTimeout(10_000) { restoredWrites.receive() })
        } finally {
            restoreScope.coroutineContext[Job]?.cancelAndJoin()
            restoredWrites.close()
        }

        container.saveSafetyController.createTestSaveRecord()
        withTimeout(10_000) {
            container.saveSafetyController.observeState().first { it.saveRecordPresent }
        }
        val originalSave = save.readBytes()
        assertTrue(originalSave.isNotEmpty())
        container.saveSafetyController.backupSave()
        withTimeout(10_000) {
            container.saveSafetyController.observeState().first { it.backupPresent && it.operationSuccessful }
        }
        val preview = container.saveSafetyController.uninstallPreview()
        assertEquals(AuthorizedHomebrewDownload.SIZE_BYTES, preview.bytesFreed)
        assertTrue(preview.retainsProgress)
        assertEquals(originalSave.size.toLong(), preview.retainedSaveBytes)

        container.saveSafetyController.uninstallTestContent()
        withTimeout(10_000) {
            container.gameRepository.observeGames().first { games ->
                games.any { it.id == gameId && it.state == InstallState.NOT_INSTALLED }
            }
        }
        assertFalse(content.exists())
        assertArrayEquals(originalSave, save.readBytes())
        assertTrue(container.saveSafetyController.observeState().value.backupPresent)

        installAndVerify()
        assertArrayEquals(originalSave, save.readBytes())
        assertTrue(container.saveSafetyController.observeState().value.backupPresent)

        // Exercise the actual restore service after changing this test-owned save.
        save.writeText("CHANGED BY LIFECYCLE TEST")
        container.saveSafetyController.restoreSave()
        withTimeout(10_000) {
            container.saveSafetyController.observeState().first {
                it.operationSuccessful && it.operationMessage == "Restore completed"
            }
        }
        assertArrayEquals(originalSave, save.readBytes())
    }
}

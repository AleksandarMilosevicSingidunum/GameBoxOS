package com.gamebox.os.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.data.DownloadRepository
import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.ImportedGameRegistration
import com.gamebox.os.download.RemoteDownloadController
import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.GameMetadataOverrides
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.ProviderMetadataSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadsScreenStateTest {
    @get:Rule val compose = createComposeRule()

    private val game = Game(
        id = GameId("download-state-test"),
        title = "Download State Test",
        platform = "Retro",
        year = 2026,
        genre = "Test",
        sizeMb = 1,
        state = InstallState.NOT_INSTALLED,
        sourceUrl = "https://catalog.example.test/download-state-test.nes",
        expectedSha256 = "0".repeat(64),
    )
    private val job = DownloadJob(
        id = game.id.value,
        gameId = game.id,
        title = game.title,
        status = DownloadStatus.QUEUED,
        totalBytes = 1024,
        downloadedBytes = 128,
    )
    private val jobs = MutableStateFlow(listOf(job))
    private val downloadRepository = object : DownloadRepository {
        override fun observeJobs(): StateFlow<List<DownloadJob>> = jobs
        override fun enqueue(game: Game) = Unit
        override fun pause(id: GameId) = Unit
        override fun resume(id: GameId) = Unit
        override fun cancel(id: GameId) = Unit
        override fun updateState(id: GameId, status: DownloadStatus, downloadedBytes: Long, errorReason: String?) = Unit
    }
    private val gameRepository = object : GameRepository {
        private val games = MutableStateFlow(listOf(game))
        override fun observeGames(): StateFlow<List<Game>> = games
        override fun game(id: GameId): Game? = games.value.firstOrNull { it.id == id }
        override fun setFavorite(id: GameId, favorite: Boolean) = Unit
        override fun setEmulatorSettings(id: GameId, packageName: String?, graphicsProfile: String) = Unit
        override fun setInstallState(id: GameId, state: InstallState) = Unit
        override fun recordPlaySession(id: GameId, endedAtMillis: Long, minutesPlayed: Int) = Unit
        override fun observeCatalogRefreshState(): StateFlow<CatalogRefreshState> =
            MutableStateFlow(CatalogRefreshState.IDLE)
        override fun refreshCatalog() = Unit
        override suspend fun registerImportedGame(imported: ImportedGameRegistration) = Unit
    }
    private val remoteController = object : RemoteDownloadController {
        override fun install(game: Game) = Unit
        override fun pause(game: Game) = Unit
        override fun resume(game: Game) = Unit
        override fun cancel(game: Game) = Unit
    }

    @Test
    fun rendersDownloadStatesWithAccessibleActions() {
        compose.setContent {
            MaterialTheme {
                DownloadsScreen(
                    repository = gameRepository,
                    downloadRepository = downloadRepository,
                    remoteDownloadController = remoteController,
                    compact = false,
                    onStore = {},
                    onSettings = {},
                )
            }
        }

        listOf(
            DownloadStatus.QUEUED to "Cancel",
            DownloadStatus.DOWNLOADING to "Pause",
            DownloadStatus.PAUSED to "Resume",
            DownloadStatus.FAILED to "Retry",
            DownloadStatus.COMPLETED to null,
        ).forEach { (status, action) ->
            val current = job.copy(
                status = status,
                errorReason = if (status == DownloadStatus.FAILED) "Network unavailable" else null,
            )
            jobs.value = listOf(current)
            compose.waitForIdle()
            compose.onNodeWithContentDescription(GameBoxSemantics.downloadDescription(current)).assertIsDisplayed()
            action?.let {
                compose.onNodeWithText(it)
                    .assertIsDisplayed()
                    .assert(hasRequestFocusAction())
            }
            if (status == DownloadStatus.COMPLETED) {
                compose.onNodeWithText("Pause").assertDoesNotExist()
                compose.onNodeWithText("Resume").assertDoesNotExist()
                compose.onNodeWithText("Retry").assertDoesNotExist()
            }
        }
    }

    @Test
    fun emptyDownloadsProvidesControllerReachableStoreRoute() {
        var storeOpened = false
        jobs.value = emptyList()
        compose.setContent {
            MaterialTheme {
                DownloadsScreen(
                    repository = gameRepository,
                    downloadRepository = downloadRepository,
                    remoteDownloadController = remoteController,
                    compact = false,
                    onStore = { storeOpened = true },
                    onSettings = {},
                )
            }
        }

        compose.onNodeWithText("No active downloads").assertIsDisplayed()
        compose.onNodeWithText("Browse Store")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performClick()
        assert(storeOpened)
    }
}

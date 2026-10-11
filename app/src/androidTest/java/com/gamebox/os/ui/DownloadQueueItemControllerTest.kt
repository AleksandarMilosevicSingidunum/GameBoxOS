package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.download.RemoteDownloadController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadQueueItemControllerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun gamepadAResumesFocusedDownload() {
        val game = Game(
            id = GameId("controller-download"),
            title = "Controller Download — A Long Game Title for a Living Room Screen",
            platform = "PSP",
            year = 2024,
            genre = "Action",
            sizeMb = 512,
            state = InstallState.PAUSED,
        )
        val job = DownloadJob(
            id = "controller-download-job",
            gameId = game.id,
            title = game.title,
            status = DownloadStatus.PAUSED,
            totalBytes = 1024,
            downloadedBytes = 512,
        )
        var resumed = 0
        val controller = object : RemoteDownloadController {
            override fun install(game: Game) = Unit
            override fun pause(game: Game) = Unit
            override fun resume(game: Game) { resumed++ }
            override fun cancel(game: Game) = Unit
        }

        compose.setContent {
            MaterialTheme {
                DownloadQueueItem(
                    job = job,
                    remoteGame = game,
                    remoteDownloadController = controller,
                    telemetryDescription = null,
                    capacityWarning = null,
                    primaryActionFocusRequester = null,
                    onDismissUnavailable = {},
                )
            }
        }

        compose.onNodeWithText("Resume").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText(game.title).assertIsDisplayed()
        compose.onNodeWithText("Paused").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, resumed) }
    }
}

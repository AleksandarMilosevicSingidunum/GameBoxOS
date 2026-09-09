package com.gamebox.os.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.storage.ContentRemovalPreview
import com.gamebox.os.storage.SaveSafetyController
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentRemovalDialogTest {
    @get:Rule val compose = createComposeRule()
    private val game = Game(GameId("dialog-test"), "Dialog Test", "PSP", 2000, "Test", 1, InstallState.INSTALLED)

    @Test fun invalidPreviewCannotConfirmAndCancelDoesNotDelete() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        var closed = 0
        val controller = object : SaveSafetyController by app.container.saveSafetyController {
            override fun contentRemovalPreview(game: Game): ContentRemovalPreview = throw IllegalArgumentException("unsafe manifest")
            override suspend fun uninstallContent(game: Game): String = error("Must not remove unverified content")
        }
        compose.setContent { MaterialTheme { ContentRemovalDialog(game, controller) { closed++ } } }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Cannot verify this game's owned content. No files were removed.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Uninstall content").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(1, closed) }
    }

    @Test fun confirmationRunsOnceAndStaysBusyUntilRemovalCompletes() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val result = CompletableDeferred<String>()
        var removals = 0
        var closed = 0
        val controller = object : SaveSafetyController by app.container.saveSafetyController {
            override fun contentRemovalPreview(game: Game) = ContentRemovalPreview(7, 1)
            override suspend fun uninstallContent(game: Game): String { removals++; return result.await() }
        }
        compose.setContent { MaterialTheme { ContentRemovalDialog(game, controller) { closed++ } } }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("1 content file(s), 7 bytes").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Uninstall content").assertIsEnabled().performClick()
        compose.onNodeWithText("Uninstall content").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, removals)
            result.complete("Content removed; saves retained")
        }
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle { assertEquals(1, closed); assertEquals(1, removals) }
    }
}

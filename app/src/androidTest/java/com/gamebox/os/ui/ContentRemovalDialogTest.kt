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
import com.gamebox.os.storage.CloudBackupPreflight
import com.gamebox.os.storage.CloudBackupPreflightStatus
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
            override suspend fun cloudBackupPreflight(game: Game) = CloudBackupPreflight(
                CloudBackupPreflightStatus.NOT_REQUIRED,
                "No managed save copy requires cloud protection.",
            )
            override suspend fun uninstallContent(game: Game): String = error("Must not remove unverified content")
        }
        compose.setContent { MaterialTheme { ContentRemovalDialog(game, controller) { closed++ } } }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Cannot verify this game's owned content and backup readiness. No files were removed.").fetchSemanticsNodes().isNotEmpty()
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
            override suspend fun cloudBackupPreflight(game: Game) = CloudBackupPreflight(
                CloudBackupPreflightStatus.NOT_REQUIRED,
                "No managed save copy requires cloud protection.",
            )
            override suspend fun uninstallContent(game: Game): String =
                uninstallContent(game, allowWithoutCloudBackup = false)
            override suspend fun uninstallContent(game: Game, allowWithoutCloudBackup: Boolean): String {
                assertFalse("Cloud acknowledgement must not be supplied when preflight is ready", allowWithoutCloudBackup)
                removals++
                return result.await()
            }
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
    @Test fun cloudFailureRequiresExplicitAcknowledgement() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        var allowedWithoutCloud = false
        val controller = object : SaveSafetyController by app.container.saveSafetyController {
            override fun contentRemovalPreview(game: Game) = ContentRemovalPreview(7, 1)
            override suspend fun cloudBackupPreflight(game: Game) = CloudBackupPreflight(
                CloudBackupPreflightStatus.ACKNOWLEDGEMENT_REQUIRED,
                "Cloud backup is unavailable: network is offline. A verified local backup is still required.",
            )
            override suspend fun uninstallContent(game: Game): String =
                error("Two-argument removal contract required")
            override suspend fun uninstallContent(game: Game, allowWithoutCloudBackup: Boolean): String {
                allowedWithoutCloud = allowWithoutCloudBackup
                return "Content removed using verified local backup"
            }
        }

        compose.setContent { MaterialTheme { ContentRemovalDialog(game, controller) {} } }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Cloud backup is unavailable", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Uninstall content").assertIsNotEnabled()
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("Uninstall content").assertIsEnabled().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Close").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle { assertTrue(allowedWithoutCloud) }
    }

}

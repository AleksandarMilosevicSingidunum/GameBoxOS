package com.gamebox.os

import android.app.ActivityOptions
import android.content.pm.ActivityInfo
import android.content.ComponentName
import android.content.res.Configuration
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.download.AssetDownloadWorker
import com.gamebox.os.download.AuthorizedHomebrewDownload
import com.gamebox.os.domain.InstallState
import com.gamebox.os.ui.GameBoxSemantics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import com.gamebox.os.display.toGameBoxFullscreenLaunchBundle

/** Exercises the dedicated external-display activity contract on a real Android runtime. */
@RunWith(AndroidJUnit4::class)
class TvActivityTest {
    @get:Rule
    val rule = createAndroidComposeRule<TvActivity>()

    @Test
    fun tvActivityRequestsLandscapeAndRendersBlueprintHome() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, rule.activity.requestedOrientation)
        val flags = rule.activity.window.attributes.flags
        assertTrue(flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0)
        assertTrue(flags and WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN == 0)
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Home tab").assertIsDisplayed()
        rule.onNodeWithContentDescription("GameBox logo").assertIsDisplayed()
    }

    @Test
    fun tvActivityAcceptsControllerOverlayAndBack() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        sendButton(KeyEvent.KEYCODE_BUTTON_START)
        rule.onNodeWithContentDescription("Controller quick actions").assertIsDisplayed()
        sendButton(KeyEvent.KEYCODE_BUTTON_B)
        rule.onAllNodesWithContentDescription("Controller quick actions").assertCountEquals(0)
        sendButton(KeyEvent.KEYCODE_BUTTON_SELECT)
        rule.onNodeWithContentDescription("System status overlay").assertIsDisplayed()
        sendButton(KeyEvent.KEYCODE_BUTTON_B)
        rule.onAllNodesWithContentDescription("System status overlay").assertCountEquals(0)
        sendButton(KeyEvent.KEYCODE_BUTTON_Y)
        rule.onNodeWithText("Switch profile").assertIsDisplayed()
        sendButton(KeyEvent.KEYCODE_BUTTON_B)
        rule.onAllNodesWithText("Switch profile").assertCountEquals(0)
    }

    @Test
    fun tvActivityMovesBlueprintTabsWithControllerShoulders() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Home tab").assertIsSelected()
        sendButton(KeyEvent.KEYCODE_BUTTON_R1)
        rule.onNodeWithContentDescription("Library tab").assertIsSelected()
        sendButton(KeyEvent.KEYCODE_BUTTON_L1)
        rule.onNodeWithContentDescription("Home tab").assertIsSelected()
    }

    @Test
    fun tvControllerTraversesTopTabsWithDpad() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Home tab")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        sendButton(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.onNodeWithContentDescription("Library tab").assertIsFocused()
        sendButton(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.waitUntil(5_000) {
            runCatching {
                rule.onNodeWithContentDescription("Library tab").fetchSemanticsNode()
                    .config[androidx.compose.ui.semantics.SemanticsProperties.Selected] == true
            }.getOrDefault(false)
        }
        rule.onNodeWithContentDescription("Library tab").assertIsSelected()
    }

    @Test
    fun tvActivityNavigatesWideDestinationsAndDownloadsWithController() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        listOf("Library", "Store", "Media", "PC", "Settings").forEach { destination ->
            rule.onNodeWithContentDescription("$destination tab").performClick()
            rule.waitUntil(5_000) {
                runCatching {
                    rule.onNodeWithContentDescription("$destination tab")
                        .fetchSemanticsNode()
                        .config[androidx.compose.ui.semantics.SemanticsProperties.Selected] == true
                }.getOrDefault(false)
            }
            rule.onNodeWithContentDescription("$destination tab").assertIsSelected()
        }
        sendButton(KeyEvent.KEYCODE_BUTTON_START)
        rule.onNodeWithContentDescription("Controller quick actions").assertIsDisplayed()
        rule.onNodeWithContentDescription("Open download activity").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("Download Manager").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Download Manager").assertIsDisplayed()
    }

    @Test
    fun tvControllerLeavesEmptyDownloadsForStore() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        sendButton(KeyEvent.KEYCODE_BUTTON_START)
        rule.onNodeWithContentDescription("Controller quick actions").assertIsDisplayed()
        val downloadsAction = rule.onNodeWithContentDescription("Downloads")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        rule.onNodeWithContentDescription("Downloads")
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("No active downloads").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("No active downloads").assertIsDisplayed()
        rule.onAllNodesWithText("Browse Store")
            .filter(hasClickAction())
            .onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        rule.onAllNodesWithText("Browse Store")
            .filter(hasClickAction())
            .onFirst()
            .performClick()
        rule.waitForIdle()
        rule.waitUntil(5_000) {
            runCatching {
                rule.onNodeWithContentDescription("Store tab").fetchSemanticsNode()
                    .config[androidx.compose.ui.semantics.SemanticsProperties.Selected] == true
            }.getOrDefault(false)
        }
        rule.onNodeWithContentDescription("Store tab").assertIsSelected()
    }

    @Test
    fun tvControllerOpensInstalledGameAndShowsPlayNow() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as GameBoxApplication
        val repository = app.container.gameRepository
        val target = repository.observeGames().value.firstOrNull()
            ?: error("Installed-game TV coverage requires a seeded catalog record")
        val originalState = target.state
        try {
            runBlocking { repository.setInstallStateAndAwait(target.id, InstallState.INSTALLED) }
            val installedTarget = runBlocking {
                withTimeout(5_000) {
                    repository.observeGames().first { games ->
                        games.any { it.id == target.id && it.state == InstallState.INSTALLED }
                    }
                }.first { it.id == target.id }
            }
            rule.onNodeWithContentDescription("Library tab").performClick()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithContentDescription(
                    GameBoxSemantics.gameCardDescription(installedTarget, hero = false)
                ).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithContentDescription(
                GameBoxSemantics.gameCardDescription(installedTarget, hero = false)
            ).performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
            sendButton(KeyEvent.KEYCODE_BUTTON_A)
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Play Now").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("Play Now").performScrollTo().assertIsDisplayed()
            rule.onNodeWithText("Play Now")
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .assertIsFocused()
            rule.onAllNodesWithText("Uninstall content")
                .filter(hasClickAction())
                .onFirst()
                .performScrollTo()
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .assertIsFocused()
            sendButton(KeyEvent.KEYCODE_BUTTON_B)
            rule.waitUntil(5_000) {
                runCatching {
                    rule.onNodeWithContentDescription("Library tab").fetchSemanticsNode()
                        .config[androidx.compose.ui.semantics.SemanticsProperties.Selected] == true
                }.getOrDefault(false)
            }
            rule.onNodeWithContentDescription("Library tab").assertIsSelected()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithContentDescription(
                    GameBoxSemantics.gameCardDescription(installedTarget, hero = false)
                ).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithContentDescription(
                GameBoxSemantics.gameCardDescription(installedTarget, hero = false)
            ).assertIsFocused()
        } finally {
            runBlocking { repository.setInstallStateAndAwait(target.id, originalState) }
        }
    }

    @Test
    fun tvControllerConfirmsAndDismissesUninstallDialog() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as GameBoxApplication
        val repository = app.container.gameRepository
        val target = repository.observeGames().value.firstOrNull()
            ?: error("Uninstall-dialog TV coverage requires a seeded catalog record")
        val originalState = target.state
        try {
            runBlocking { repository.setInstallStateAndAwait(target.id, InstallState.INSTALLED) }
            val installedTarget = runBlocking {
                withTimeout(5_000) {
                    repository.observeGames().first { games ->
                        games.any { it.id == target.id && it.state == InstallState.INSTALLED }
                    }
                }.first { it.id == target.id }
            }
            rule.onNodeWithContentDescription("Library tab").performClick()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithContentDescription(
                    GameBoxSemantics.gameCardDescription(installedTarget, hero = false)
                ).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithContentDescription(
                GameBoxSemantics.gameCardDescription(installedTarget, hero = false)
            ).performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
            sendButton(KeyEvent.KEYCODE_BUTTON_A)
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Play Now").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("Play Now")
                .performScrollTo()
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .assertIsFocused()
            rule.onAllNodesWithText("Uninstall content")
                .filter(hasClickAction())
                .onFirst()
                .performScrollTo()
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .assertIsFocused()
            sendButton(KeyEvent.KEYCODE_BUTTON_A)
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Uninstall ${installedTarget.title}?")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("Uninstall ${installedTarget.title}?").assertIsDisplayed()
            sendButton(KeyEvent.KEYCODE_BUTTON_B)
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Uninstall ${installedTarget.title}?")
                    .fetchSemanticsNodes().isEmpty()
            }
            rule.waitUntil(5_000) {
                runCatching {
                    rule.onNodeWithContentDescription("Library tab").fetchSemanticsNode()
                        .config[androidx.compose.ui.semantics.SemanticsProperties.Selected] == true
                }.getOrDefault(false)
            }
            rule.onNodeWithContentDescription("Library tab").assertIsSelected()
            rule.onNodeWithContentDescription(
                GameBoxSemantics.gameCardDescription(installedTarget, hero = false)
            ).assertIsFocused()
        } finally {
            runBlocking { repository.setInstallStateAndAwait(target.id, originalState) }
        }
    }

    @Test
    fun tvControllerFocusesAndStartsAuthorizedInstallAction() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as GameBoxApplication
        val repository = app.container.gameRepository
        val target = repository.observeGames().value.firstOrNull { it.id.value == "galaxy-patrol" }
            ?: error("Authorized fixture is required for TV install coverage")
        val originalState = target.state
        val installedContent = app.filesDir.resolve(AssetDownloadWorker.INSTALL_ROOT)
            .resolve(AuthorizedHomebrewDownload.RELATIVE_PATH)
        try {
            app.container.authorizedDownloadController.cancel()
            runBlocking { repository.setInstallStateAndAwait(target.id, InstallState.NOT_INSTALLED) }
            val notInstalledTarget = runBlocking {
                withTimeout(5_000) {
                    repository.observeGames().first { games ->
                        games.firstOrNull { it.id == target.id }?.state == InstallState.NOT_INSTALLED
                    }
                }.first { it.id == target.id }
            }
            rule.onNodeWithContentDescription("Store tab").performClick()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithContentDescription(
                    GameBoxSemantics.gameCardDescription(notInstalledTarget, hero = false)
                ).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithContentDescription(
                GameBoxSemantics.gameCardDescription(notInstalledTarget, hero = false)
            ).performClick()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Install game").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onAllNodesWithText("Install game")
                .filter(hasClickAction())
                .onFirst()
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .assertIsFocused()
            sendButton(KeyEvent.KEYCODE_BUTTON_A)
            runBlocking {
                withTimeout(5_000) {
                    repository.observeGames().first { games ->
                        games.firstOrNull { it.id == target.id }?.state == InstallState.INSTALLED
                    }
                }
            }
            assertTrue(installedContent.isFile)
            assertEquals(AuthorizedHomebrewDownload.SIZE_BYTES, installedContent.length())
            rule.onNodeWithContentDescription("Library tab").performClick()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithContentDescription(
                    GameBoxSemantics.gameCardDescription(target.copy(state = InstallState.INSTALLED), hero = false)
                ).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithContentDescription(
                GameBoxSemantics.gameCardDescription(target.copy(state = InstallState.INSTALLED), hero = false)
            ).assertIsDisplayed()
        } finally {
            app.container.authorizedDownloadController.cancel()
            if (originalState != InstallState.INSTALLED) installedContent.delete()
            runBlocking { repository.setInstallStateAndAwait(target.id, originalState) }
        }
    }

    @Test
    fun tvControllerFocusesAuthorizedCopyImportAction() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as GameBoxApplication
        val repository = app.container.gameRepository
        val target = repository.observeGames().value.firstOrNull { it.id.value != "galaxy-patrol" }
            ?: error("Non-authorized catalog fixture is required for TV import coverage")
        val originalState = target.state
        try {
            runBlocking { repository.setInstallStateAndAwait(target.id, InstallState.NOT_INSTALLED) }
            val notInstalledTarget = runBlocking {
                withTimeout(5_000) {
                    repository.observeGames().first { games ->
                        games.firstOrNull { it.id == target.id }?.state == InstallState.NOT_INSTALLED
                    }
                }.first { it.id == target.id }
            }
            rule.onNodeWithContentDescription("Store tab").performClick()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithContentDescription(
                    GameBoxSemantics.gameCardDescription(notInstalledTarget, hero = false)
                ).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithContentDescription(
                GameBoxSemantics.gameCardDescription(notInstalledTarget, hero = false)
            ).performClick()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Import your copy").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("Import your copy")
                .performScrollTo()
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .assertIsFocused()
            sendButton(KeyEvent.KEYCODE_BUTTON_B)
            rule.waitUntil(5_000) {
                runCatching {
                    rule.onNodeWithContentDescription("Store tab").fetchSemanticsNode()
                        .config[androidx.compose.ui.semantics.SemanticsProperties.Selected] == true
                }.getOrDefault(false)
            }
        } finally {
            runBlocking { repository.setInstallStateAndAwait(target.id, originalState) }
        }
    }

    @Test
    fun tvControllerFocusesManagedSaveImportAction() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as GameBoxApplication
        val repository = app.container.gameRepository
        val target = repository.observeGames().value.firstOrNull()
            ?: error("Managed-save TV coverage requires a seeded catalog record")
        val originalState = target.state
        try {
            runBlocking { repository.setInstallStateAndAwait(target.id, InstallState.INSTALLED) }
            val installedTarget = runBlocking {
                withTimeout(5_000) {
                    repository.observeGames().first { games ->
                        games.any { it.id == target.id && it.state == InstallState.INSTALLED }
                    }
                }.first { it.id == target.id }
            }
            rule.onNodeWithContentDescription("Library tab").performClick()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithContentDescription(
                    GameBoxSemantics.gameCardDescription(installedTarget, hero = false)
                ).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithContentDescription(
                GameBoxSemantics.gameCardDescription(installedTarget, hero = false)
            ).performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
            sendButton(KeyEvent.KEYCODE_BUTTON_A)
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Managed save data").fetchSemanticsNodes().isNotEmpty()
            }
            rule.waitUntil(2_000) {
                runCatching {
                    rule.onNodeWithText("Play Now").fetchSemanticsNode()
                        .config[androidx.compose.ui.semantics.SemanticsProperties.Focused] == true
                }.getOrDefault(false)
            }
            rule.onNodeWithText("Import save copy")
                .performScrollTo()
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .assertIsFocused()
            sendButton(KeyEvent.KEYCODE_BUTTON_B)
            rule.waitUntil(5_000) {
                runCatching {
                    rule.onNodeWithContentDescription("Library tab").fetchSemanticsNode()
                        .config[androidx.compose.ui.semantics.SemanticsProperties.Selected] == true
                }.getOrDefault(false)
            }
            rule.onNodeWithContentDescription("Library tab").assertIsSelected()
        } finally {
            runBlocking { repository.setInstallStateAndAwait(target.id, originalState) }
        }
    }

    @Test
    fun tvControllerActivatesSafeAreaSetting() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as GameBoxApplication
        val settings = app.container.settingsRepository
        val originalPercent = runBlocking { settings.settings.first().safeAreaPercent }
        try {
            rule.onNodeWithContentDescription("Settings tab").performClick()
            rule.waitForIdle()
            rule.onNodeWithText("Display").performScrollTo().assertIsDisplayed()
            rule.onNodeWithContentDescription("TV safe area 8%")
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .assertIsFocused()
            sendButton(KeyEvent.KEYCODE_BUTTON_A)
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Active inset: 8% on every edge.")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("Active inset: 8% on every edge.")
                .performScrollTo()
                .assertIsDisplayed()
            runBlocking {
                withTimeout(5_000) {
                    settings.settings.first { current -> kotlin.math.abs(current.safeAreaPercent - 0.08f) < 0.001f }
                }
            }
        } finally {
            runBlocking { settings.setSafeAreaPercent(originalPercent) }
        }
    }

    @Test
    fun tvControllerDisablesSafeAreaInset() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as GameBoxApplication
        val settings = app.container.settingsRepository
        val originalPercent = runBlocking { settings.settings.first().safeAreaPercent }
        try {
            runBlocking { settings.setSafeAreaPercent(0.06f) }
            rule.onNodeWithContentDescription("Settings tab").performClick()
            rule.waitForIdle()
            rule.onNodeWithContentDescription("TV safe area Off")
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .assertIsFocused()
            sendButton(KeyEvent.KEYCODE_BUTTON_A)
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Active inset: 0% on every edge.")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("Active inset: 0% on every edge.")
                .performScrollTo()
                .assertIsDisplayed()
            runBlocking {
                withTimeout(5_000) {
                    settings.settings.first { kotlin.math.abs(it.safeAreaPercent) < 0.001f }
                }
            }
        } finally {
            runBlocking { settings.setSafeAreaPercent(originalPercent) }
        }
    }

    @Test
    fun tvControllerConfirmsFocusedBlueprintQuickLaunchCard() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        val desktop = rule.onNodeWithContentDescription("Quick launch Desktop; opens PC Hub")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        desktop.performSemanticsAction(SemanticsActions.RequestFocus)
        sendButton(KeyEvent.KEYCODE_BUTTON_A)
        rule.waitUntil(5_000) {
            runCatching {
                rule.onNodeWithContentDescription("PC tab").fetchSemanticsNode()
                    .config[androidx.compose.ui.semantics.SemanticsProperties.Selected] == true
            }.getOrDefault(false)
        }
        rule.onNodeWithContentDescription("PC tab").assertIsSelected()
    }

    @Test
    fun tvActivityKeepsNonExportedHostBoundary() {
        val info = rule.activity.packageManager.getActivityInfo(
            ComponentName(rule.activity, TvActivity::class.java),
            0,
        )
        assertFalse(info.exported)
    }

    @Test
    fun tvActivityReappliesBorderlessPolicyAfterConfigurationChange() {
        val changed = Configuration(rule.activity.resources.configuration)
        changed.uiMode = changed.uiMode xor Configuration.UI_MODE_NIGHT_YES
        rule.runOnUiThread { rule.activity.onConfigurationChanged(changed) }
        rule.waitForIdle()

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, rule.activity.requestedOrientation)
        val flags = rule.activity.window.attributes.flags
        assertTrue(flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0)
        assertTrue(flags and WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN == 0)
    }

    @Test
    fun tvActivityKeepsFullscreenWindowSizeOnApi34AndNewer() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        // Android does not expose a synchronous fullscreen-promotion state;
        // verify the host remains in the supported fullscreen configuration.
        assertEquals(android.view.WindowManager.LayoutParams.MATCH_PARENT, rule.activity.window.attributes.width)
        assertEquals(android.view.WindowManager.LayoutParams.MATCH_PARENT, rule.activity.window.attributes.height)
    }

    @Test
    fun tvActivityUsesBorderlessViewportAndCutoutPolicy() {
        val flags = rule.activity.window.attributes.flags
        assertTrue(flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0)
        assertTrue(flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS != 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            assertEquals(
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
                rule.activity.window.attributes.layoutInDisplayCutoutMode,
            )
        }
    }

    @Test
    fun externalDisplayLaunchBundleRequestsFullscreenWindowingMode() {
        val launchBundle = ActivityOptions.makeBasic().toGameBoxFullscreenLaunchBundle()
        assertEquals(1, launchBundle.getInt("android.activity.windowingMode", -1))
        // A display-sized launchBounds rectangle can make desktop shells create
        // a captioned/freeform task instead of a borderless TV surface.
        assertFalse(launchBundle.containsKey("android:activity.launchBounds"))
    }

    private fun sendButton(keyCode: Int) {
        rule.runOnUiThread {
            rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
        rule.waitForIdle()
    }
}

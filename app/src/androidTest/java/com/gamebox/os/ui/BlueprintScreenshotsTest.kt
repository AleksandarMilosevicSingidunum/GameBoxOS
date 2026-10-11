package com.gamebox.os.ui

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.MainActivity
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.download.AssetDownloadWorker
import com.gamebox.os.download.AuthorizedDownloadState
import com.gamebox.os.download.AuthorizedHomebrewDownload
import com.gamebox.os.download.InstalledContentStatus
import com.gamebox.os.download.InstalledContentValidator
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Captures the production activity, not synthetic representations of the screens. */
@RunWith(AndroidJUnit4::class)
class BlueprintScreenshotsTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureRealScreens() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        val hero = rule.onAllNodes(hasContentDescription("continue playing", substring = true))
        rule.waitUntil(15_000) {
            hero.fetchSemanticsNodes().isNotEmpty()
        }
        if (rule.activity.resources.configuration.screenWidthDp >= 900) {
            rule.onNodeWithContentDescription("X button: Search").assertIsDisplayed()
            rule.onNodeWithContentDescription("Y button: Switch Profile").assertIsDisplayed()
        }
        capture("01-home")
        hero.onFirst().performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("About this game").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(1, rule.onAllNodesWithText("About this game").fetchSemanticsNodes().size)
        capture("02-game-details")
        listOf("Library", "Store", "Media", "PC", "Settings").forEachIndexed { index, title ->
            val tab = rule.onNodeWithContentDescription("$title tab")
            // A scrolling tab row is used on phones; desktop tabs have no scroll parent.
            runCatching { tab.performScrollTo() }
            tab.performClick()
            rule.waitForIdle()
            tab.assertIsSelected()
            if (rule.activity.resources.configuration.screenWidthDp >= 900) {
                when (title) {
                    "Library" -> {
                        rule.onNodeWithContentDescription("X button: Search").assertIsDisplayed()
                        rule.onNodeWithContentDescription("Y button: Favorites").assertIsDisplayed()
                    }
                    "Store" -> {
                        rule.onNodeWithContentDescription("X button: Search").assertIsDisplayed()
                        rule.onNodeWithContentDescription("Y button: Filters").assertIsDisplayed()
                    }
                    "PC" -> rule.onNodeWithContentDescription("Show GameBox on external display")
                        .assertIsDisplayed()
                        .performSemanticsAction(SemanticsActions.RequestFocus)
                        .assertIsFocused()
                    "Settings" -> {
                        rule.onNodeWithContentDescription("Storage settings section").assertIsSelected()
                        rule.onNodeWithText("Interface").assertIsNotDisplayed()
                        rule.onNodeWithContentDescription("System settings section").performClick()
                        rule.onNodeWithText("Interface").assertIsDisplayed()
                        rule.onNodeWithText("UI sound effects").performScrollTo().assertIsDisplayed()
                        rule.onNodeWithContentDescription("Developer settings section").performClick()
                        rule.onNodeWithText("Developer and diagnostics").assertIsDisplayed()
                        rule.onNodeWithContentDescription("Focus diagnostics overlay")
                            .performScrollTo().assertIsDisplayed()
                        rule.onNodeWithContentDescription("Display settings section").performClick()
                        rule.onNodeWithText("Display").performScrollTo().assertIsDisplayed()
                        rule.onAllNodes(hasContentDescription("TV safe area", substring = true))
                            .onFirst()
                            .assertExists()
                            .performSemanticsAction(SemanticsActions.RequestFocus)
                            .assertIsFocused()
                            .assertIsDisplayed()
                    }
                }
            }
            if (title == "Store" &&
                rule.activity.resources.configuration.screenWidthDp >= 900
            ) {
                listOf("Genre: All", "Region: All", "Language: All").forEach { label ->
                    val filter = rule.onNodeWithText(label)
                    // At enlarged font scales the horizontal filter rail intentionally scrolls.
                    // Prove each control is reachable before requiring it to be visible.
                    runCatching { filter.performScrollTo() }
                    filter.assertIsDisplayed()
                }
            }
            capture("${index + 3}-${title.lowercase()}")
        }
        // Downloads is a utility destination in the Blueprint, not a primary tab.
        if (rule.activity.resources.configuration.screenWidthDp >= 900) {
            rule.onNodeWithContentDescription("Open download activity").performClick()
        } else {
            sendButton(KeyEvent.KEYCODE_BUTTON_START)
            rule.onNodeWithContentDescription("Controller quick actions").assertIsDisplayed()
            // Settings may also expose a Downloads section chip in the same compact
            // semantics tree; the dialog action is the last matching entry.
            rule.onAllNodesWithText("Downloads").onLast().performClick()
        }
        rule.waitForIdle()
        if (rule.activity.resources.configuration.screenWidthDp >= 900) {
            // The production screen enters through the same animated transition
            // as every other Blueprint destination; wait for the destination
            // content rather than only for the previous composition to idle.
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Download Manager").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("Download Manager").performScrollTo().assertIsDisplayed()
            val browseStore = rule.onAllNodesWithText("Browse Store").filter(hasClickAction())
            browseStore.assertCountEquals(1)
            browseStore.onFirst().assertIsFocused()
        } else {
            rule.onNodeWithText("Downloads").assertIsDisplayed()
        }
        capture("08-downloads")
        capturePopulatedLayout()
    }

    @Test
    fun updateAvailableStateIsVisibleInLibraryAndDetails() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as GameBoxApplication
        val repository = app.container.gameRepository
        val target = repository.observeGames().value.firstOrNull()
            ?: error("Update-state coverage requires a seeded catalog record")
        val originalState = target.state
        try {
            runBlocking { repository.setInstallStateAndAwait(target.id, InstallState.UPDATE_AVAILABLE) }
            rule.onNodeWithContentDescription("Library tab").performClick()
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Availability: Update").assertIsDisplayed()
            rule.onNode(hasContentDescription(target.title, substring = true)).performClick()
            rule.waitForIdle()
            // The details action and the controller footer both expose the same label.
            rule.onAllNodesWithText("Update").onLast().assertIsDisplayed()
        } finally {
            runBlocking { repository.setInstallStateAndAwait(target.id, originalState) }
        }
    }

    @Test
    fun installedGameIsPlayReadyOnWideTv() {
        if (rule.activity.resources.configuration.screenWidthDp < 900) return
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as GameBoxApplication
        val repository = app.container.gameRepository
        val targetId = GameId("galaxy-patrol")
        org.junit.Assume.assumeTrue(
            "Play-ready coverage requires the bundled Galaxy Patrol fixture in this catalog",
            repository.observeGames().value.any { it.id == targetId },
        )
        val target = repository.game(targetId) ?: error("Galaxy Patrol disappeared during TV setup")
        val installedContent = app.filesDir.resolve(AssetDownloadWorker.INSTALL_ROOT)
            .resolve(AuthorizedHomebrewDownload.RELATIVE_PATH)
        val validator = InstalledContentValidator(app.filesDir.resolve(AssetDownloadWorker.INSTALL_ROOT))
        val validContentBefore = validator.validate(
            AuthorizedHomebrewDownload.RELATIVE_PATH,
            AuthorizedHomebrewDownload.SHA256,
        ) == InstalledContentStatus.VERIFIED
        val originalState = target.state
        org.junit.Assume.assumeTrue(
            "Refusing to replace a non-verified pre-existing Galaxy Patrol install",
            validContentBefore || !installedContent.exists(),
        )
        try {
            if (!validContentBefore) {
                app.container.authorizedDownloadController.cancel()
                runBlocking {
                    repository.setInstallStateAndAwait(target.id, InstallState.NOT_INSTALLED)
                }
                val previousWorkId = app.container.authorizedDownloadController.observeState().value.workId
                app.container.authorizedDownloadController.install()
                rule.waitUntil(120_000) {
                    val current = app.container.authorizedDownloadController.observeState().value
                    current.status == AuthorizedDownloadState.Status.SUCCEEDED &&
                        current.workId != previousWorkId &&
                        installedContent.isFile &&
                        validator.validate(
                            AuthorizedHomebrewDownload.RELATIVE_PATH,
                            AuthorizedHomebrewDownload.SHA256,
                        ) == InstalledContentStatus.VERIFIED
                }
            }
            runBlocking { repository.setInstallStateAndAwait(target.id, InstallState.INSTALLED) }
            val installedTarget = runBlocking {
                withTimeout(5_000) {
                    repository.observeGames().first { games ->
                        games.any { it.id == target.id && it.state == InstallState.INSTALLED }
                    }
                }.first { it.id == target.id }
            }
            rule.onNodeWithContentDescription("Library tab").performClick()
            rule.waitForIdle()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithContentDescription(
                    GameBoxSemantics.gameCardDescription(installedTarget, hero = false)
                ).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithContentDescription(
                GameBoxSemantics.gameCardDescription(installedTarget, hero = false)
            ).assertIsDisplayed()

            rule.onNodeWithContentDescription(
                GameBoxSemantics.gameCardDescription(installedTarget, hero = false)
            ).performClick()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Play Now").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("Play Now").performScrollTo().assertIsDisplayed()
        } finally {
            app.container.authorizedDownloadController.cancel()
            if (!validContentBefore) installedContent.delete()
            runBlocking { repository.setInstallStateAndAwait(target.id, originalState) }
        }
    }

    @Test
    fun tvSafeAreaSelectionPersistsInWideSettings() {
        if (rule.activity.resources.configuration.screenWidthDp < 900) return
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
                .performSemanticsAction(SemanticsActions.OnClick)
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Active inset: 8% on every edge.").fetchSemanticsNodes().isNotEmpty()
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
    fun compactStoreExposesDirectConsoleFilters() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        if (rule.activity.resources.configuration.screenWidthDp >= 900) return
        rule.onNodeWithContentDescription("Store tab").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Browse authorized games, metadata, and configured sources").assertIsDisplayed()
        rule.onNodeWithContentDescription("Store console filters").assertIsDisplayed()
        rule.onNodeWithContentDescription("Store console filter: All").assertIsDisplayed()
        val pspFilter = rule.onNodeWithContentDescription("Store console filter: PSP")
        // The compact Blueprint rail scrolls horizontally on narrow/phone displays.
        runCatching { pspFilter.performScrollTo() }
        pspFilter.assertIsDisplayed()
        rule.onNodeWithText("Search games").assertIsDisplayed()
    }

    @Test
    fun compactSettingsKeepsExternalLibraryActionReachable() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        if (rule.activity.resources.configuration.screenWidthDp >= 900) return

        rule.onNodeWithContentDescription("Settings tab").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("External game library").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Choose folder").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Emulator save root").performScrollTo().assertIsDisplayed()
        val saveActionLabel = listOf("Choose save folder", "Change save folder")
            .firstOrNull { rule.onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty() }
            ?: error("Expected either Choose save folder or Change save folder")
        rule.onNodeWithText(saveActionLabel).performScrollTo().assertIsDisplayed()
    }

    private fun sendButton(keyCode: Int) {
        rule.runOnUiThread {
            rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
        rule.waitForIdle()
    }

    /** Test-only installed flags exercise layout, not installation or gameplay. */
    private fun capturePopulatedLayout() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as GameBoxApplication
        val repository = app.container.gameRepository
        val originals = repository.observeGames().value.take(6)
        check(originals.isNotEmpty()) { "Populated capture requires catalog records" }
        try {
            runBlocking {
                originals.forEach { repository.setInstallStateAndAwait(it.id, InstallState.INSTALLED) }
            }
            listOf("Home", "Library").forEach { title ->
                val tab = rule.onNodeWithContentDescription("$title tab")
                runCatching { tab.performScrollTo() }
                tab.performClick()
                rule.waitForIdle()
                tab.assertIsSelected()
                capture("populated-layout-${title.lowercase()}")
            }
        } finally {
            runBlocking {
                originals.forEach { repository.setInstallStateAndAwait(it.id, it.state) }
            }
        }
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        // Allow asynchronous artwork to arrive; captures also exercise the offline fallback.
        Thread.sleep(2_000)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "ui-captures").apply { mkdirs() }
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

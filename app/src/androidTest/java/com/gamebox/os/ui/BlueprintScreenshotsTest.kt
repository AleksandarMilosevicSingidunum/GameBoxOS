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
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.runBlocking
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
        if (rule.activity.resources.configuration.screenWidthDp >= 900) {
            // The TV Details hero keeps the primary authorized-install action in
            // the right-side installation panel instead of below the fold.
            rule.onAllNodesWithText("Install game").onLast().assertIsDisplayed()
        }
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

package com.gamebox.os.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
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
        capture("02-game-details")
        listOf("Library", "Store", "Downloads", "Media", "PC", "Settings").forEachIndexed { index, title ->
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
                }
            }
            if (title == "Store" &&
                rule.activity.resources.configuration.screenWidthDp >= 900
            ) {
                rule.onNodeWithText("Genre: All").assertIsDisplayed()
                rule.onNodeWithText("Region: All").assertIsDisplayed()
                rule.onNodeWithText("Language: All").assertIsDisplayed()
            }
            capture("${index + 3}-${title.lowercase()}")
        }
        capturePopulatedLayout()
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

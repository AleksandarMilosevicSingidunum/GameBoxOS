package com.gamebox.os.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.MainActivity
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
            capture("${index + 3}-${title.lowercase()}")
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

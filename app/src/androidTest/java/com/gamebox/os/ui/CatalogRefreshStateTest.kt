package com.gamebox.os.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.MainActivity
import com.gamebox.os.settings.CatalogFailureSimulation
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Production-activity coverage for deterministic catalog fallback/error presentation. */
@RunWith(AndroidJUnit4::class)
class CatalogRefreshStateTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val app: GameBoxApplication
        get() = rule.activity.application as GameBoxApplication

    @After
    fun restoreLiveCatalogState() {
        runBlocking { app.container.settingsRepository.setCatalogFailureSimulation(CatalogFailureSimulation.LIVE) }
    }

    @Test
    fun fallbackAndErrorStatesRemainAccessibleAndActionable() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Store tab").performClick()
        rule.waitForIdle()
        // Start from a known value so a prior interrupted run cannot make the
        // first simulated state a no-op in DataStore.
        runBlocking { app.container.settingsRepository.setCatalogFailureSimulation(CatalogFailureSimulation.LIVE) }
        rule.waitForIdle()

        val states = listOf(
            CatalogFailureSimulation.OFFLINE_FALLBACK to
                "Offline; bundled catalog loaded; installed games remain playable",
            CatalogFailureSimulation.REMOTE_FALLBACK to
                "Catalog provider unavailable; bundled catalog loaded; choose Refresh to retry",
            CatalogFailureSimulation.ERROR to
                "Catalog refresh failed; cached catalog remains available; choose Refresh to retry",
        )
        states.forEach { (simulation, message) ->
            runBlocking { app.container.settingsRepository.setCatalogFailureSimulation(simulation) }
            rule.waitUntil(5_000) {
                rule.onAllNodesWithContentDescription(message, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithContentDescription(message, useUnmergedTree = true).assertIsDisplayed()
            rule.onNodeWithText("Refresh").assertIsDisplayed()
        }
    }
}

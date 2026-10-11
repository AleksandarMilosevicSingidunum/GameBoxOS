package com.gamebox.os.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.launch.EmulatorCapabilityRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises emulator readiness messaging against apps visible on this device. */
@RunWith(AndroidJUnit4::class)
class GameSettingsPanelReadinessTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun pspSettingsExplainMissingPpssppAndDisableUnavailableChoices() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as GameBoxApplication
        val game = Game(
            id = GameId("readiness-test-psp"),
            title = "Readiness Test PSP",
            platform = "PSP",
            year = 2006,
            genre = "Homebrew",
            sizeMb = 1,
            state = InstallState.NOT_INSTALLED,
        )
        val options = EmulatorCapabilityRegistry().optionsFor(game)
        val installedOptions = options.filterTo(mutableSetOf()) { packageName ->
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        }

        composeRule.setContent {
            MaterialTheme {
                GameSettingsPanel(
                    game = game,
                    repository = app.container.gameRepository,
                    settingsRepository = app.container.settingsRepository,
                )
            }
        }

        if (installedOptions.isEmpty()) {
            composeRule.onNodeWithText("Install PPSSPP before launching this game.")
                .assertExists()
            composeRule.onNodeWithText(
                "Install the approved package, then return to GameBox to refresh readiness."
            ).assertExists()
        } else {
            composeRule.onNodeWithText("PPSSPP").assertExists()
        }
        val installedChoices = composeRule.onAllNodesWithText("PPSSPP · Installed")
        assertEquals("Each installed package appears in two settings rows", 2 * installedOptions.size,
            installedChoices.fetchSemanticsNodes().size)
        repeat(installedChoices.fetchSemanticsNodes().size) { index ->
            installedChoices[index].assertIsEnabled()
        }
        val unavailableChoices = composeRule.onAllNodesWithText("PPSSPP · Not installed")
        assertEquals("Each unavailable package appears in two settings rows",
            2 * (options.size - installedOptions.size), unavailableChoices.fetchSemanticsNodes().size)
        repeat(unavailableChoices.fetchSemanticsNodes().size) { index ->
            unavailableChoices[index].assertIsNotEnabled()
        }
    }
}

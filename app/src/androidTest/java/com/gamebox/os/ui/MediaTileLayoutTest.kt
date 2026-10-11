package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaTileLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun squareTileKeepsLabelsAndClickAction() = verifyTile(1f)
    @Test fun enlargedTileKeepsLabelsAndClickAction() = verifyTile(2f)

    @Test
    fun gamepadAActivatesFocusedPcHubShortcutTile() {
        lateinit var inputModeManager: androidx.compose.ui.input.InputModeManager
        var launches = 0
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            MaterialTheme {
                BlueprintShortcutTile(
                    shortcut = AppShortcut("Steam", "PC games", "com.valvesoftware.android.steam.community"),
                    installed = true,
                    modifier = Modifier.size(128.dp).testTag("steam-shortcut"),
                    onClick = { launches++ },
                )
            }
        }

        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithTag("steam-shortcut")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, launches) }
    }

    @Test
    fun gamepadAActivatesFocusedMediaHubCard() {
        lateinit var inputModeManager: androidx.compose.ui.input.InputModeManager
        var launches = 0
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            MaterialTheme {
                ShortcutCard(
                    shortcut = AppShortcut("Spotify", "Music streaming", "com.spotify.music"),
                    installed = true,
                    modifier = Modifier.width(220.dp).testTag("spotify-shortcut"),
                    onClick = { launches++ },
                )
            }
        }

        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithTag("spotify-shortcut")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        compose.onNodeWithText("Spotify", useUnmergedTree = true).assertHeightIsAtLeast(22.dp)
        compose.onNodeWithText("READY", useUnmergedTree = true).assertHeightIsAtLeast(14.dp)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, launches) }
    }

    @Test
    fun pcHeroIsOneControllerFocusableLaunchTarget() {
        lateinit var inputModeManager: androidx.compose.ui.input.InputModeManager
        var launches = 0
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            MaterialTheme {
                BlueprintPcHero(
                    shortcut = AppShortcut(
                        "Moonlight",
                        "PC game streaming",
                        "com.limelight",
                        AppShortcutRole.FEATURED_PC_STREAMING,
                    ),
                    installed = true,
                    onClick = { launches++ },
                    modifier = Modifier.size(320.dp, 220.dp).testTag("pc-hero"),
                )
            }
        }

        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithTag("pc-hero")
            .assertContentDescriptionEquals("Moonlight, ready to launch")
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, launches) }
    }

    @Test
    fun missingPcHeroOffersAnInstallActionInsteadOfADeadLaunchAction() {
        var openedInstallPage = 0
        compose.setContent {
            MaterialTheme {
                BlueprintPcHero(
                    shortcut = AppShortcut(
                        "Moonlight",
                        "PC game streaming",
                        "com.limelight",
                        AppShortcutRole.FEATURED_PC_STREAMING,
                    ),
                    installed = false,
                    onClick = { openedInstallPage++ },
                    modifier = Modifier.size(320.dp, 220.dp).testTag("pc-hero-missing"),
                )
            }
        }

        compose.onNodeWithContentDescription("Moonlight not installed, open install page")
            .assertHasClickAction()
            .performClick()
        compose.onNodeWithText("Install Moonlight to stream").assertExists()
        compose.onNodeWithText("Get Moonlight", useUnmergedTree = true).assertExists()
        compose.runOnIdle { assertEquals(1, openedInstallPage) }
    }

    private fun verifyTile(fontScale: Float) {
        var launches = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                MaterialTheme {
                    BlueprintShortcutTile(AppShortcut("YouTube", "Video", "test.youtube"), false,
                        Modifier.size(128.dp, (128 + 48 * (fontScale - 1)).dp).testTag("media-tile")) { launches++ }
                }
            }
        }
        val tile = compose.onNodeWithTag("media-tile")
        val bounds = tile.fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText("YouTube", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val status = compose.onNodeWithText("Setup required", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(title.top >= bounds.top && title.bottom <= status.top)
        assertTrue(title.height >= 15f)
        assertTrue(status.bottom <= bounds.bottom && status.height >= 13f)
        tile.assertIsDisplayed()
            .assertContentDescriptionEquals("YouTube, Setup required")
            .assertHasClickAction()
            .performClick()
        compose.runOnIdle { assertEquals(1, launches) }
    }
}


package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.ui.theme.GameBoxTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscoveryDetailsImportActionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactAuthorizedImportActionIsFocusableAndActivates() {
        var imports = 0
        compose.setContent {
            GameBoxTheme {
                Box(Modifier.width(560.dp)) {
                    TvDetailsSecondaryAction(
                        label = "Import your copy",
                        compact = true,
                        enabled = true,
                        onClick = { imports++ },
                    )
                }
            }
        }

        compose.onNodeWithText("Import your copy")
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)

        compose.runOnIdle { assertEquals(1, imports) }
    }

    @Test
    fun vimmCopyImportRemainsAvailableInDetailsImportPanel() {
        var imports = 0
        compose.setContent {
            GameBoxTheme {
                DiscoveryImportSection(
                    platformLabel = "Game Boy Advance",
                    acceptedFormats = ".gba",
                    importing = false,
                    progress = null,
                    message = null,
                    supportsDiscSet = false,
                    showCopyImport = true,
                    onCancel = {},
                    onImportCopy = { imports++ },
                    onImportDiscSet = {},
                )
            }
        }

        compose.onNodeWithText("Import your copy")
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, imports) }
    }

    @Test
    fun vimmDetailsShowOnePrimaryDownloadAndKeepCopyImportAsSecondaryAction() {
        var vimmOpened = 0
        var fileImports = 0
        compose.setContent {
            GameBoxTheme {
                Column(Modifier.width(560.dp)) {
                    DiscoveryDetailsHeroActions(
                        compact = false,
                        importing = false,
                        downloadAvailable = true,
                        favoriteEnabled = false,
                        favorited = false,
                        onImport = { fileImports++ },
                        onDownload = { vimmOpened++ },
                        onFavorite = {},
                        onBack = {},
                    )
                    DiscoveryImportSection(
                        platformLabel = "Game Boy Advance",
                        acceptedFormats = ".gba",
                        importing = false,
                        progress = null,
                        message = null,
                        supportsDiscSet = false,
                        showCopyImport = true,
                        onCancel = {},
                        onImportCopy = { fileImports++ },
                        onImportDiscSet = {},
                    )
                }
            }
        }

        compose.onAllNodesWithText("Download", useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithText("Download", useUnmergedTree = true)
            .assertHasClickAction()
            .performClick()
        compose.onNodeWithText("Import your copy")
            .assertHasClickAction()
            .performClick()
        compose.runOnIdle {
            assertEquals(1, vimmOpened)
            assertEquals(1, fileImports)
        }
    }
}

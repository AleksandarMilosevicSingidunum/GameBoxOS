package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscoveryDetailsHeroActionsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun primaryActionReceivesFocusWhenLaidOut() {
        val focusRequester = FocusRequester()
        compose.setContent {
            MaterialTheme {
                DiscoveryDetailsHeroActions(
                    compact = true,
                    primaryActionFocusRequester = focusRequester,
                    importing = false,
                    downloadAvailable = true,
                    favoriteEnabled = true,
                    favorited = false,
                    onImport = {},
                    onDownload = {},
                    onFavorite = {},
                    onBack = {},
                )
            }
        }

        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        compose.onNodeWithText("Download").assertIsFocused()
    }

    @Test
    fun gamepadAStartsAuthorizedCopyImport() {
        var imports = 0
        compose.setContent {
            MaterialTheme {
                DiscoveryDetailsHeroActions(
                    compact = true,
                    importing = false,
                    favoriteEnabled = true,
                    favorited = false,
                    onImport = { imports++ },
                    onFavorite = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Import your copy")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, imports) }
    }

    @Test
    fun gamepadAStartsTrackedVimmDownloadFromPrimaryHeroAction() {
        var downloads = 0
        var imports = 0
        compose.setContent {
            MaterialTheme {
                DiscoveryDetailsHeroActions(
                    compact = true,
                    importing = false,
                    downloadAvailable = true,
                    favoriteEnabled = true,
                    favorited = false,
                    onImport = { imports++ },
                    onDownload = { downloads++ },
                    onFavorite = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Download")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle {
            assertEquals(1, downloads)
            assertEquals(0, imports)
        }
    }

    @Test
    fun activeTrackedDownloadPrimaryActionOpensDownloadManager() {
        var managerOpens = 0
        var downloadsStarted = 0
        compose.setContent {
            MaterialTheme {
                DiscoveryDetailsHeroActions(
                    compact = true,
                    importing = false,
                    downloadAvailable = true,
                    downloadWaiting = true,
                    favoriteEnabled = false,
                    favorited = false,
                    onImport = {},
                    onDownload = { downloadsStarted++ },
                    onOpenDownloads = { managerOpens++ },
                    onFavorite = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("View Download")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle {
            assertEquals(1, managerOpens)
            assertEquals(0, downloadsStarted)
        }
    }
}

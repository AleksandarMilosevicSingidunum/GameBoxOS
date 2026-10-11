package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.importer.BrowserDownloadStatus

@RunWith(AndroidJUnit4::class)
class BlueprintNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun focusDoesNotChangeSelectionAndClickSelectsOneTab() {
        lateinit var inputModeManager: InputModeManager
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            var selected by remember { mutableStateOf(Destination.HOME) }
            MaterialTheme {
                Row {
                    listOf(Destination.HOME, Destination.LIBRARY).forEach { item ->
                        NavButton(item, selected) { selected = it }
                    }
                }
            }
        }
        val home = compose.onNodeWithContentDescription("Home tab")
        val library = compose.onNodeWithContentDescription("Library tab")
        home.assertIsSelected()
        library.assertIsNotSelected()
        // Clickable tabs only accept non-touch focus in keyboard/controller mode.
        compose.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        library.performSemanticsAction(SemanticsActions.RequestFocus)
        library.assertIsFocused().assertIsNotSelected()
        home.assertIsSelected()
        library.performClick()
        library.assertIsSelected()
        home.assertIsNotSelected()
    }

    @Test fun gamepadAActivatesHeaderSearchAction() {
        lateinit var inputModeManager: InputModeManager
        var selected by mutableStateOf(Destination.HOME)
        var searchRequests = 0
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            MaterialTheme {
                Box(Modifier.width(1200.dp)) {
                    TopNav(
                        selected = selected,
                        compact = false,
                        onSelect = { selected = it },
                        onSearch = { searchRequests++ },
                    )
                }
            }
        }

        val search = compose.onNodeWithContentDescription("Browse and search games")
        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        search.performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle {
            assertEquals(Destination.HOME, selected)
            assertEquals(1, searchRequests)
        }
    }

    @Test fun downloadActionShowsAndReportsOutstandingTransfers() {
        var selected by mutableStateOf(Destination.HOME)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(1200.dp)) {
                    TopNav(
                        selected = selected,
                        compact = false,
                        downloadActivityCount = downloadActivityCount(
                            remoteStatuses = listOf(DownloadStatus.DOWNLOADING, DownloadStatus.COMPLETED),
                            browserStatuses = listOf(BrowserDownloadStatus.FAILED),
                        ),
                        onSelect = { selected = it },
                    )
                }
            }
        }

        val downloads = compose.onNodeWithContentDescription(
            "Open download activity, 2 items active or needing attention",
        )
        downloads.assertIsDisplayed().performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        downloads.performClick()
        compose.runOnIdle { assertEquals(Destination.DOWNLOADS, selected) }
    }

    @Test fun dpadMovesTopTabFocusWithoutChangingTheSelectedDestination() {
        lateinit var inputModeManager: InputModeManager
        var selected by mutableStateOf(Destination.HOME)
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            MaterialTheme {
                Box(Modifier.width(1920.dp)) {
                    TopNav(selected = selected, compact = false, onSelect = { selected = it })
                }
            }
        }

        compose.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        val home = compose.onNodeWithContentDescription("Home tab")
        val library = compose.onNodeWithContentDescription("Library tab")
        home.performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)

        library.assertIsFocused().assertIsNotSelected()
        home.assertIsSelected()
        compose.runOnIdle { assertEquals(Destination.HOME, selected) }
    }

    @Test fun wideHeaderDateAndTabsMeetLivingRoomTextSizeMinimum() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(1920.dp)) {
                    TopNav(selected = Destination.HOME, compact = false, onSelect = {})
                }
            }
        }

        compose.onNodeWithTag("wide-nav-date").assertHeightIsAtLeast(14.dp)
        compose.onNodeWithText("Home", useUnmergedTree = true).assertHeightIsAtLeast(16.dp)
    }
}


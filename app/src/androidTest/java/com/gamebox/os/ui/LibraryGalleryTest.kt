package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.unit.dp
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryGalleryTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun libraryRailShowsCompactConsoleNameAndKeepsFullAccessibleName() {
        val game = Game(
            GameId("gba-rail"), "Test Adventure", "Nintendo Game Boy Advance", 2001,
            "Adventure", 8, InstallState.INSTALLED,
        )
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.width(1000.dp).height(700.dp)) {
                    val filters = remember { RememberedGameFilters("", null, null, null, null, false) }
                    BlueprintLibraryScreen(
                        games = listOf(game),
                        filtered = listOf(game),
                        filters = filters,
                        focusTarget = game.id,
                        onFocused = {},
                        open = {},
                        onStore = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("GBA").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Nintendo Game Boy Advance, 1 game").assertExists()
    }

    @Test fun cachedLibraryScreenshotOpensAndCloses() {
        val detail = GameDetailPresentation.from(
            Game(GameId("gallery"), "Gallery Game", "PSP", 2006, "Racing", 1, InstallState.INSTALLED)
        ).copy(screenshots = listOf("invalid-test-url"))
        composeRule.setContent { GameScreenshotGallery(detail, compact = true) }
        composeRule.onNodeWithContentDescription("Enlarge screenshot of Gallery Game").performClick()
        composeRule.onNodeWithText("Close").assertExists().performClick()
        composeRule.onNodeWithText("Close").assertDoesNotExist()
        composeRule.onNodeWithText("Screenshots").assertExists()
    }

    @Test fun screenshotViewerClosesWithControllerA() {
        val detail = GameDetailPresentation.from(
            Game(GameId("gallery-controller"), "Gallery Game", "PSP", 2006, "Racing", 1, InstallState.INSTALLED)
        ).copy(screenshots = listOf("invalid-test-url"))
        composeRule.setContent { GameScreenshotGallery(detail, compact = true) }
        composeRule.onNodeWithContentDescription("Enlarge screenshot of Gallery Game").performClick()
        composeRule.onNodeWithText("Close").performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        composeRule.onNodeWithText("Close").assertDoesNotExist()
        composeRule.onNodeWithText("Screenshots").assertExists()
    }
}

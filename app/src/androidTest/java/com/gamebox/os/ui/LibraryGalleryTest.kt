package com.gamebox.os.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryGalleryTest {
    @get:Rule val composeRule = createComposeRule()

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
}

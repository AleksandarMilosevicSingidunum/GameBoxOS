package com.gamebox.os.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PosterGameCardLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun coverArtAndTitleMetadataOccupySeparateNonOverlappingAreas() {
        val game = Game(
            id = GameId("poster-layout-test"),
            title = "Mario & Luigi: Partners in Time",
            platform = "Nintendo DS",
            year = 2005,
            genre = "RPG",
            sizeMb = 1,
            state = InstallState.INSTALLED,
        )

        compose.setContent {
            MaterialTheme {
                GameCard(
                    game = game,
                    modifier = Modifier.size(124.dp, 168.dp),
                    poster = true,
                    onClick = {},
                )
            }
        }

        val artwork = compose.onNodeWithTag("game-card-poster-artwork")
            .fetchSemanticsNode().boundsInRoot
        val footer = compose.onNodeWithTag("game-card-poster-info")
            .fetchSemanticsNode().boundsInRoot
        val card = compose.onNodeWithContentDescription(
            GameBoxSemantics.gameCardDescription(game, hero = false),
        ).fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText(game.title, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val metadata = compose.onNodeWithText("Nintendo DS · Installed", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        assertTrue("Poster artwork must end before the information footer", artwork.bottom <= footer.top)
        assertTrue("The footer must remain inside the poster card", footer.bottom <= card.bottom)
        assertTrue("Title must stay inside the information footer", title.top >= footer.top && title.bottom <= footer.bottom)
        assertTrue("Platform/status must stay inside the information footer", metadata.top >= footer.top && metadata.bottom <= footer.bottom)
        compose.onNodeWithContentDescription("Availability: Installed", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun landscapeCardsKeepStatusInCaptionWithoutCoveringArtwork() {
        val game = Game(
            id = GameId("landscape-status-layout-test"),
            title = "Yu-Gi-Oh! Forbidden Memories",
            platform = "PlayStation",
            year = 1999,
            genre = "Card Battle",
            sizeMb = 1,
            state = InstallState.INSTALLED,
        )

        compose.setContent {
            MaterialTheme {
                GameCard(
                    game = game,
                    modifier = Modifier.size(198.dp, 100.dp),
                    onClick = {},
                )
            }
        }

        compose.onNodeWithText("PlayStation · Installed", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Availability: Installed", useUnmergedTree = true)
            .assertDoesNotExist()
    }
}

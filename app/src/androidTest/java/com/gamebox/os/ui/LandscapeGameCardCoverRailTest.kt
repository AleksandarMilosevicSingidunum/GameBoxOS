package com.gamebox.os.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class LandscapeGameCardCoverRailTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun titleAndAvailabilityBadgeStayBesideTheContainedCover() {
        val game = Game(
            id = GameId("partners-in-time"),
            title = "Mario & Luigi: Partners in Time",
            platform = "Nintendo DS",
            year = 2005,
            genre = "RPG",
            sizeMb = 1,
            state = InstallState.INSTALLED,
        )
        var railContentStart = 0f

        compose.setContent {
            MaterialTheme {
                railContentStart = with(LocalDensity.current) {
                    gameCardCoverRailWidth(140.dp).toPx() + 6.dp.toPx()
                }
                GameCard(
                    game = game,
                    modifier = Modifier.size(240.dp, 140.dp),
                    poster = false,
                    onClick = {},
                )
            }
        }

        val card = compose.onNodeWithContentDescription(
            GameBoxSemantics.gameCardDescription(game, hero = false),
        ).fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText(game.title, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val badge = compose.onNodeWithContentDescription("Availability: Installed")
            .fetchSemanticsNode().boundsInRoot

        assertTrue("The title should start after the cover rail", title.left >= card.left + railContentStart)
        assertTrue("The status badge should not cover the cover art", badge.left >= card.left + railContentStart)
        assertTrue("The title should remain within the landscape card", title.right <= card.right)
    }
}

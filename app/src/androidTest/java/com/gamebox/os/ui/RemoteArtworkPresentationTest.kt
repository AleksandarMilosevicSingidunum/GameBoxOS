package com.gamebox.os.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class RemoteArtworkPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun ambientFallbackOmitsMonogramWhileCoverFallbackKeepsIt() {
        compose.setContent {
            MaterialTheme {
                RemoteArtwork(url = null, fallbackKey = "Cave Story", showFallbackMonogram = false)
            }
        }
        compose.onNodeWithText("CS").assertDoesNotExist()

        compose.setContent {
            MaterialTheme {
                RemoteArtwork(url = null, fallbackKey = "Cave Story")
            }
        }
        compose.onNodeWithText("CS").assertExists()
    }

    @Test
    fun wideGameCardShowsMonogramOnlyOnItsCoverRail() {
        val game = Game(
            id = GameId("artwork-card-test"),
            title = "Cave Story",
            platform = "Homebrew",
            year = 2004,
            genre = "Adventure",
            sizeMb = 1,
            state = InstallState.INSTALLED,
        )
        compose.setContent {
            MaterialTheme {
                GameCard(game = game, modifier = Modifier.size(198.dp, 100.dp), onClick = {})
            }
        }

        compose.onAllNodesWithText("CS", useUnmergedTree = true).assertCountEquals(1)
    }
}

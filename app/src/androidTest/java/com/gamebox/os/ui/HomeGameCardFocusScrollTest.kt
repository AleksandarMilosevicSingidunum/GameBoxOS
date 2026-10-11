package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeGameCardFocusScrollTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun focusingGameCardBelowHomeViewportScrollsItIntoView() {
        val first = game("first")
        val belowViewport = game("below-viewport")
        compose.setContent {
            MaterialTheme {
                Column(
                    Modifier.fillMaxWidth()
                        .height(190.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    GameCard(
                        game = first,
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        onClick = {},
                    )
                    GameCard(
                        game = belowViewport,
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        onClick = {},
                    )
                }
            }
        }

        compose.onNodeWithContentDescription(GameBoxSemantics.gameCardDescription(belowViewport, hero = false))
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .assertIsDisplayed()
    }

    private fun game(id: String) = Game(
        id = GameId(id),
        title = id,
        platform = "Retro",
        year = 2026,
        genre = "Test",
        sizeMb = 1,
        state = InstallState.INSTALLED,
    )
}

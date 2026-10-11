package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.GameId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscoveryGameCardActivationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun gamepadAOpensFocusedDiscoveryCard() {
        val game = DiscoveryGame(
            id = GameId("controller-discovery-test"),
            title = "Controller Discovery Test",
            platformId = "nes",
            releaseDate = null,
            description = null,
            players = null,
            rating = null,
            coverUrl = null,
            backgroundUrl = null,
            logoUrl = null,
            screenshots = emptyList(),
            favorite = false,
        )
        var opened = 0

        compose.setContent {
            MaterialTheme {
                Box {
                    DiscoveryGameCard(game, Modifier.size(220.dp), onClick = { opened++ })
                }
            }
        }
        compose.waitForIdle()

        val card = compose.onNodeWithContentDescription(
            "Controller Discovery Test, nes, discover only, import an authorized copy to play",
        )
        compose.onNodeWithText("Discover · Import your copy").assertIsDisplayed()
        card.performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, opened) }
    }
}

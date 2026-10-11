package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameDetailsDiscoveryCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun controllerAOpensVimmDiscoveryFromDetailsCard() {
        var storeOpens = 0
        compose.setContent {
            MaterialTheme {
                GameDetailsDiscoveryCard(compact = false, onOpenStore = { storeOpens++ })
            }
        }

        compose.onNodeWithText("Download")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("VIMM LAIR").assertIsDisplayed()
        compose.onNodeWithText("Discover and download").assertIsDisplayed()
        compose.onNodeWithText(
            "Browse Vimm Lair in GameBox. Supported downloads are tracked and imported automatically."
        ).assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("Import your copy").fetchSemanticsNodes().isEmpty())
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.runOnIdle { assertEquals(1, storeOpens) }
    }
}

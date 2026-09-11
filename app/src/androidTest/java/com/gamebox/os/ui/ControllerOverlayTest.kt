package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ControllerOverlayTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun menuAndViewButtonsOpenSafeGlobalOverlaysAndBackClosesThem() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }

        sendButton(KeyEvent.KEYCODE_BUTTON_START)
        rule.onNodeWithContentDescription("Controller quick actions").assertIsDisplayed()
        sendButton(KeyEvent.KEYCODE_BUTTON_B)
        rule.onNodeWithContentDescription("Controller quick actions").assertDoesNotExist()

        sendButton(KeyEvent.KEYCODE_BUTTON_SELECT)
        rule.onNodeWithContentDescription("System status overlay").assertIsDisplayed()
        sendButton(KeyEvent.KEYCODE_BUTTON_B)
        rule.onNodeWithContentDescription("System status overlay").assertDoesNotExist()
    }

    private fun sendButton(keyCode: Int) {
        rule.runOnUiThread {
            rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
        rule.waitForIdle()
    }
}

package com.gamebox.os

import android.content.pm.ActivityInfo
import android.content.ComponentName
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/** Exercises the dedicated external-display activity contract on a real Android runtime. */
@RunWith(AndroidJUnit4::class)
class TvActivityTest {
    @get:Rule
    val rule = createAndroidComposeRule<TvActivity>()

    @Test
    fun tvActivityRequestsLandscapeAndRendersBlueprintHome() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, rule.activity.requestedOrientation)
        val flags = rule.activity.window.attributes.flags
        assertTrue(flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0)
        assertTrue(flags and WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN == 0)
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Home tab").assertIsDisplayed()
        rule.onNodeWithContentDescription("GameBox logo").assertIsDisplayed()
    }

    @Test
    fun tvActivityAcceptsControllerOverlayAndBack() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        sendButton(KeyEvent.KEYCODE_BUTTON_START)
        rule.onNodeWithContentDescription("Controller quick actions").assertIsDisplayed()
        sendButton(KeyEvent.KEYCODE_BUTTON_B)
        rule.onAllNodesWithContentDescription("Controller quick actions").assertCountEquals(0)
    }

    @Test
    fun tvActivityMovesBlueprintTabsWithControllerShoulders() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("Home tab").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Home tab").assertIsSelected()
        sendButton(KeyEvent.KEYCODE_BUTTON_R1)
        rule.onNodeWithContentDescription("Library tab").assertIsSelected()
        sendButton(KeyEvent.KEYCODE_BUTTON_L1)
        rule.onNodeWithContentDescription("Home tab").assertIsSelected()
    }

    @Test
    fun tvActivityKeepsNonExportedHostBoundary() {
        val info = rule.activity.packageManager.getActivityInfo(
            ComponentName(rule.activity, TvActivity::class.java),
            0,
        )
        assertFalse(info.exported)
    }

    private fun sendButton(keyCode: Int) {
        rule.runOnUiThread {
            rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
        rule.waitForIdle()
    }
}

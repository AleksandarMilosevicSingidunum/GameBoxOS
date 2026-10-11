package com.gamebox.os.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import android.webkit.WebResourceRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.InputDevice
import android.view.KeyEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.ui.theme.GameBoxTheme
import com.gamebox.os.input.gamepadBrowserActivationKeyCode
import com.gamebox.os.input.gamepadBrowserKeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackedSourceBrowserToolbarTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun toolbarPresentsSourceAndControllerActivatableReturnAction() {
        var backCount = 0
        compose.setContent {
            GameBoxTheme {
                Box(Modifier.size(width = 1920.dp, height = 1080.dp)) {
                    TrackedSourceBrowserToolbar(
                        sourceLabel = "Vimm's Lair",
                        backLabel = "Back to GameBox",
                        onBack = { backCount++ },
                    )
                }
            }
        }

        compose.onNodeWithText("Vimm's Lair").assertExists()
        compose.onNodeWithContentDescription("D-pad button: Navigate").assertExists()
        compose.onNodeWithContentDescription("A button: Select").assertExists()
        compose.onNodeWithContentDescription("B button: Back").assertExists()
        compose.onNodeWithTag("tracked-source-back-to-gamebox")
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performClick()
        compose.runOnIdle { assertEquals(1, backCount) }
    }

    @Test
    fun controllerActivationKeepsBlankTargetDownloadInTrackedWebView() {
        val downloadUrl = "https://downloads.example.test/legal-homebrew.rom"
        val arrivedAtTrackedView = CountDownLatch(1)
        val trustedPopupCount = intArrayOf(0)
        lateinit var webView: WebView

        compose.setContent {
            AndroidView(factory = { context ->
                WebView(context).apply {
                    webView = this
                    settings.javaScriptEnabled = true
                    settings.setSupportMultipleWindows(true)
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            if (request.url.toString() == downloadUrl) {
                                arrivedAtTrackedView.countDown()
                                return true
                            }
                            return false
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            if (url.startsWith("https://vimm.net/")) {
                                view.evaluateJavascript("document.querySelector('#download').focus()") {
                                    val now = android.os.SystemClock.uptimeMillis()
                                    val code = requireNotNull(
                                        gamepadBrowserActivationKeyCode(KeyEvent.KEYCODE_BUTTON_A),
                                    )
                                    listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
                                        val controllerEvent = KeyEvent(
                                            now,
                                            now,
                                            action,
                                            KeyEvent.KEYCODE_BUTTON_A,
                                            0,
                                            0,
                                            1,
                                            0,
                                            0,
                                            InputDevice.SOURCE_GAMEPAD,
                                        )
                                        val browserEvent = gamepadBrowserKeyEvent(controllerEvent, code, 0)
                                        assertEquals(InputDevice.SOURCE_KEYBOARD, browserEvent.source)
                                        view.dispatchKeyEvent(browserEvent)
                                    }
                                }
                            }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onCreateWindow(
                            view: WebView,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: android.os.Message,
                        ): Boolean {
                            if (isUserGesture) trustedPopupCount[0]++
                            val popup = WebView(view.context).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        if (request.url.toString() == downloadUrl) arrivedAtTrackedView.countDown()
                                        return true
                                    }
                                }
                            }
                            (resultMsg.obj as WebView.WebViewTransport).webView = popup
                            resultMsg.sendToTarget()
                            return true
                        }
                    }
                    loadDataWithBaseURL(
                        "https://vimm.net/",
                        "<a id='download' target='_blank' href='$downloadUrl'>Download</a>",
                        "text/html",
                        "UTF-8",
                        null,
                    )
                }
            }, modifier = Modifier.size(1.dp))
        }

        assertTrue(
            "A controller-confirm click on a target=_blank download must stay in the tracked WebView",
            arrivedAtTrackedView.await(10, TimeUnit.SECONDS),
        )
        compose.runOnIdle { org.junit.Assert.assertTrue("Controller confirm must retain a user gesture", trustedPopupCount[0] > 0) }
        compose.runOnIdle { webView.destroy() }
    }
}

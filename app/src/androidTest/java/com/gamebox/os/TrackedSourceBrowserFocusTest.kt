package com.gamebox.os

import android.content.Intent
import android.webkit.ValueCallback
import android.view.InputDevice
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackedSourceBrowserFocusTest {
    @Test
    fun trackedBrowserActivityRoutesControllerAIntoItsFocusedVimmControl() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(instrumentation.targetContext, TrackedSourceBrowserActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(TrackedSourceBrowserActivity.EXTRA_SOURCE_URL, "https://vimm.net.invalid/vault/test")
            .putExtra(TrackedSourceBrowserActivity.EXTRA_SOURCE_LABEL, "Vimm's Lair")
            .putExtra(TrackedSourceBrowserActivity.EXTRA_PLATFORM, "NES")
        val activity = instrumentation.startActivitySync(intent) as TrackedSourceBrowserActivity
        val webViewRef = AtomicReference<WebView>()
        val pageReady = CountDownLatch(1)
        val downloadReady = CountDownLatch(1)
        val clickState = AtomicReference<String>()
        val clicked = CountDownLatch(1)
        val popupCreated = AtomicReference<Boolean>()
        val popupRead = CountDownLatch(1)
        val focusState = AtomicReference("not observed")
        try {
            activity.runOnUiThread {
                val view = requireNotNull(findWebView(activity.window.decorView))
                webViewRef.set(view)
                view.stopLoading()
                view.loadDataWithBaseURL(
                    "https://vimm.net/vault/test",
                    "<a id='vault-result' href='/vault/999' " +
                        "onclick=\"event.preventDefault();window.resultClicked=1\">Quantum Theory</a>",
                    "text/html",
                    "UTF-8",
                    "https://vimm.net/vault/test",
                )
                view.postDelayed(object : Runnable {
                    override fun run() {
                        if (pageReady.count == 0L) return
                        view.evaluateJavascript("document.activeElement.id") { focused ->
                            focusState.set("element=$focused viewFocus=${view.hasFocus()} url=${view.url}")
                            if (focused == "\"vault-result\"" && view.hasFocus()) {
                                pageReady.countDown()
                            } else {
                                view.postDelayed(this, 50)
                            }
                        }
                    }
                }, 50)
            }
            assertTrue("Production page-finished handler did not focus the fixture: ${focusState.get()}",
                pageReady.await(5, TimeUnit.SECONDS))
            val webView = requireNotNull(webViewRef.get())
            val focused = CountDownLatch(1)
            val focusedElement = AtomicReference<String>()
            activity.runOnUiThread {
                webView.evaluateJavascript("document.activeElement.id") { result ->
                    focusedElement.set(result.trim('"'))
                    focused.countDown()
                }
            }
            assertTrue("Vimm vault result was not focused", focused.await(5, TimeUnit.SECONDS))
            assertEquals("vault-result", focusedElement.get())
            assertTrue("Tracked browser WebView did not retain Android view focus", webView.hasFocus())
            val pageUrl = AtomicReference<String>()
            val pageUrlRead = CountDownLatch(1)
            activity.runOnUiThread { pageUrl.set(webView.url); pageUrlRead.countDown() }
            assertTrue(pageUrlRead.await(5, TimeUnit.SECONDS))
            assertTrue("Local fixture did not retain the Vimm origin: ${pageUrl.get()}",
                pageUrl.get()?.startsWith("https://vimm.net/") == true)

            val now = android.os.SystemClock.uptimeMillis()
            activity.runOnUiThread {
                listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
                    activity.dispatchKeyEvent(
                        KeyEvent(now, now, action, KeyEvent.KEYCODE_BUTTON_A, 0, 0, 1, 0, 0,
                            InputDevice.SOURCE_GAMEPAD),
                    )
                }
            }
            val resultActivated = CountDownLatch(1)
            val resultActivation = AtomicReference<String>()
            activity.runOnUiThread {
                webView.evaluateJavascript("window.resultClicked||0", ValueCallback { result ->
                    resultActivation.set(result.trim('"'))
                    resultActivated.countDown()
                })
            }
            assertTrue("Controller A did not activate the focused vault result", resultActivated.await(5, TimeUnit.SECONDS))
            assertEquals("1", resultActivation.get())
            activity.runOnUiThread {
                webView.loadDataWithBaseURL(
                    "https://vimm.net/vault/999",
                    "<a id='site-download' href='/downloads'>Download</a>" +
                        "<div style='display:none'><div id='hidden-download' role='button' " +
                        "aria-label='Download game'>Download</div></div>" +
                    "<script>setTimeout(()=>{const b=document.createElement('button');b.id='download';" +
                        "b.textContent='Download this game';b.onclick=function(event){" +
                        "window.clicked=(window.clicked||0)+1;window.trusted=event.isTrusted;" +
                        "window.popup=!!window.open('about:blank','_blank')};document.body.append(b)},250)</script>",
                    "text/html",
                    "UTF-8",
                    "https://vimm.net/vault/999",
                )
                webView.postDelayed(object : Runnable {
                    override fun run() {
                        if (downloadReady.count == 0L) return
                        webView.evaluateJavascript("document.activeElement.id") { focused ->
                            if (focused == "\"download\"" && webView.hasFocus()) {
                                downloadReady.countDown()
                            } else {
                                webView.postDelayed(this, 50)
                            }
                        }
                    }
                }, 50)
            }
            assertTrue("Download control was not focused after opening a Vimm detail", downloadReady.await(5, TimeUnit.SECONDS))
            val focusedDownload = CountDownLatch(1)
            activity.runOnUiThread {
                webView.evaluateJavascript("document.activeElement.id", ValueCallback { result ->
                    focusedElement.set(result.trim('"'))
                    focusedDownload.countDown()
                })
            }
            assertTrue(focusedDownload.await(5, TimeUnit.SECONDS))
            assertEquals("download", focusedElement.get())
            val outlineRead = CountDownLatch(1)
            val focusedOutline = AtomicReference<String>()
            activity.runOnUiThread {
                webView.evaluateJavascript(
                    "getComputedStyle(document.activeElement).outlineWidth+':'" +
                        "+getComputedStyle(document.activeElement).outlineStyle+':'" +
                        "+getComputedStyle(document.activeElement).outlineColor",
                    ValueCallback { result ->
                        focusedOutline.set(result.trim('"'))
                        outlineRead.countDown()
                    },
                )
            }
            assertTrue("Focused Download outline was not read", outlineRead.await(5, TimeUnit.SECONDS))
            val outline = focusedOutline.get().split(':')
            assertTrue("Focused Download outline should remain clearly visible", outline[0]
                .removeSuffix("px").toFloat() >= 2f)
            assertEquals("solid", outline[1])
            assertEquals("rgb(255, 212, 0)", outline[2])
            val confirmTime = android.os.SystemClock.uptimeMillis()
            activity.runOnUiThread {
                listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
                    activity.dispatchKeyEvent(
                        KeyEvent(confirmTime, confirmTime, action, KeyEvent.KEYCODE_BUTTON_A, 0, 0, 1, 0, 0,
                            InputDevice.SOURCE_GAMEPAD),
                    )
                }
            }
            activity.runOnUiThread {
                webView.evaluateJavascript(
                    "(window.clicked || 0)+':'+window.trusted+':'+window.popup",
                    ValueCallback { result ->
                        clickState.set(result.trim('"'))
                        clicked.countDown()
                    },
                )
            }
            assertTrue("Download activation was not observed", clicked.await(5, TimeUnit.SECONDS))
            assertEquals(
                "Controller A must produce a trusted click and keep its popup in GameBox",
                "1:true:true",
                clickState.get(),
            )
            activity.runOnUiThread {
                val popup = TrackedSourceBrowserActivity::class.java.getDeclaredField("popupWebView")
                    .apply { isAccessible = true }
                popupCreated.set(popup.get(activity) != null)
                popupRead.countDown()
            }
            assertTrue(popupRead.await(5, TimeUnit.SECONDS))
            assertEquals("The controller-activated new-tab action must stay inside GameBox", true, popupCreated.get())
            val popupClosed = CountDownLatch(1)
            val backTime = android.os.SystemClock.uptimeMillis()
            activity.runOnUiThread {
                listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
                    activity.dispatchKeyEvent(
                        KeyEvent(backTime, backTime, action, KeyEvent.KEYCODE_BUTTON_B, 0, 0, 1, 0, 0,
                            InputDevice.SOURCE_GAMEPAD),
                    )
                }
                val popup = TrackedSourceBrowserActivity::class.java.getDeclaredField("popupWebView")
                    .apply { isAccessible = true }
                if (popup.get(activity) == null) popupClosed.countDown()
            }
            assertTrue("Controller B did not close the in-app popup", popupClosed.await(5, TimeUnit.SECONDS))
        } finally {
            activity.runOnUiThread { activity.finish() }
        }
    }

    private fun findWebView(view: View): WebView? = when (view) {
        is WebView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { index ->
            findWebView(view.getChildAt(index))
        }
        else -> null
    }

}

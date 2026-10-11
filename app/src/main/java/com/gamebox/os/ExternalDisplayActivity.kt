package com.gamebox.os

import android.content.pm.ActivityInfo
import android.hardware.display.DisplayManager
import android.os.Bundle
import com.gamebox.os.display.applyBorderlessFullscreenWindow
import com.gamebox.os.display.currentDisplayId
import com.gamebox.os.display.hideSystemBars
import com.gamebox.os.display.requestExternalFullscreenAfterFocus
import com.gamebox.os.display.shouldFinishExternalDisplayActivityForRemovedDisplay
import com.gamebox.os.display.shouldReapplyExternalDisplayWindowPolicyForChangedDisplay

/** Dedicated fullscreen host for GameBox when launched from the phone onto an external display. */
class ExternalDisplayActivity : GameBoxHostActivity() {
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            val currentDisplayId = this@ExternalDisplayActivity.currentDisplayId()
            if (shouldReapplyExternalDisplayWindowPolicyForChangedDisplay(currentDisplayId, displayId)) {
                runOnUiThread {
                    if (!isFinishing) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        applyBorderlessFullscreenWindow(window)
                        hideSystemBars(window)
                    }
                }
            }
        }

        override fun onDisplayRemoved(displayId: Int) {
            val currentDisplayId = this@ExternalDisplayActivity.currentDisplayId()
            if (shouldFinishExternalDisplayActivityForRemovedDisplay(currentDisplayId, displayId)) {
                runOnUiThread {
                    if (!isFinishing) finish()
                }
            }
        }
    }

    private val displayManager: DisplayManager by lazy {
        getSystemService(DisplayManager::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        applyBorderlessFullscreenWindow(window)
        super.onCreate(savedInstanceState)
        hideSystemBars(window)
    }

    override fun onStart() {
        super.onStart()
        displayManager.registerDisplayListener(displayListener, null)
    }

    override fun onStop() {
        displayManager.unregisterDisplayListener(displayListener)
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars(window)
            // Retry after focus returns from system pickers, which may otherwise
            // restore an OEM freeform window instead of the fullscreen TV surface.
            requestExternalFullscreenAfterFocus()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars(window)
    }

    override fun onMultiWindowModeChanged(
        isInMultiWindowMode: Boolean,
        newConfig: android.content.res.Configuration,
    ) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        applyBorderlessFullscreenWindow(window)
        hideSystemBars(window)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        applyBorderlessFullscreenWindow(window)
        hideSystemBars(window)
    }
}

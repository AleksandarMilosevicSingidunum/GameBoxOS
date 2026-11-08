package com.gamebox.os

import android.content.pm.ActivityInfo
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import com.gamebox.os.display.firstPresentationDisplayTarget
import com.gamebox.os.display.hideSystemBars
import com.gamebox.os.display.isExternalDisplayId
import com.gamebox.os.display.isLikelyVirtualPresentationDisplay
import com.gamebox.os.display.launchGameBoxOnExternalDisplay
import com.gamebox.os.display.presentationDisplayTargets
import com.gamebox.os.display.requestExternalFullscreenAfterFocus
import com.gamebox.os.display.requestExternalActivityFullscreen
import com.gamebox.os.display.requestPrimaryFullscreenAfterFocus
import com.gamebox.os.display.currentDisplayId

/** Android Home/launcher host. Its singleTask contract is intentionally preserved. */
class MainActivity : GameBoxHostActivity() {
    private var externalLaunchRequestedDisplayId: Int? = null
    private var externalFullscreenRequestedFromUserInteraction = false
    private val displayManager by lazy { getSystemService(DisplayManager::class.java) }
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = openOnExternalDisplayIfAvailable()

        override fun onDisplayChanged(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) {
            if (externalLaunchRequestedDisplayId == displayId) {
                externalLaunchRequestedDisplayId = null
            }
        }
    }

    private val isExternalDisplay: Boolean
        get() = isExternalDisplayId(currentDisplayId())

    override fun onStart() {
        super.onStart()
        displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
    }

    override fun onStop() {
        displayManager.unregisterDisplayListener(displayListener)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLauncherWindowPolicy()
        super.onCreate(savedInstanceState)
        applyLauncherWindowPolicy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyLauncherWindowPolicy()
            requestPrimaryFullscreenAfterFocus()
            requestExternalFullscreenAfterFocus()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (
            isExternalDisplay && isInMultiWindowMode &&
            !externalFullscreenRequestedFromUserInteraction
        ) {
            // Desktop windowing requires fullscreen promotion to follow user input.
            externalFullscreenRequestedFromUserInteraction = true
            requestExternalActivityFullscreen(this)
        }
    }

    override fun onResume() {
        super.onResume()
        applyLauncherWindowPolicy()
        requestPrimaryFullscreenAfterFocus()
        requestExternalFullscreenAfterFocus()
        openOnExternalDisplayIfAvailable()
    }

    override fun onMultiWindowModeChanged(
        isInMultiWindowMode: Boolean,
        newConfig: android.content.res.Configuration,
    ) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        applyLauncherWindowPolicy()
        requestPrimaryFullscreenAfterFocus()
        requestExternalFullscreenAfterFocus()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        applyLauncherWindowPolicy()
    }

    private fun applyLauncherWindowPolicy() {
        if (isExternalDisplay) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        // HONOR wired projection can mirror the primary display instead of
        // registering a secondary Android display. The launcher must remain
        // immersive in both routing modes so the dock/taskbar cannot frame the UI.
        hideSystemBars(window)
    }

    private fun openOnExternalDisplayIfAvailable() {
        if (isFinishing || isExternalDisplay || externalLaunchRequestedDisplayId != null) return
        val target = firstPresentationDisplayTarget(presentationDisplayTargets(this)) ?: return
        if (target.isVirtual || isLikelyVirtualPresentationDisplay(target.name)) return

        externalLaunchRequestedDisplayId = target.displayId
        window.decorView.postDelayed({
            if (isFinishing || isExternalDisplay) return@postDelayed
            if (presentationDisplayTargets(this).none { it.displayId == target.displayId }) {
                externalLaunchRequestedDisplayId = null
                return@postDelayed
            }
            val result = launchGameBoxOnExternalDisplay(this)
            if (result.startsWith("Could not") || result.startsWith("No external")) {
                externalLaunchRequestedDisplayId = null
            }
        }, 400L)
    }
}

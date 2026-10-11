package com.gamebox.os

import android.content.pm.ActivityInfo
import android.os.Bundle
import com.gamebox.os.display.hideSystemBars
import com.gamebox.os.display.isExternalDisplayId
import com.gamebox.os.display.requestExternalFullscreenAfterFocus
import com.gamebox.os.display.requestPrimaryFullscreenAfterFocus
import com.gamebox.os.display.currentDisplayId

/** Android Home/launcher host. Its singleTask contract is intentionally preserved. */
class MainActivity : GameBoxHostActivity() {
    private val isExternalDisplay: Boolean
        get() = isExternalDisplayId(currentDisplayId())

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

    override fun onResume() {
        super.onResume()
        applyLauncherWindowPolicy()
        requestPrimaryFullscreenAfterFocus()
        requestExternalFullscreenAfterFocus()
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
}

package com.gamebox.os

import android.content.pm.ActivityInfo
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gamebox.os.display.shouldFinishTvActivityForRemovedDisplay
import com.gamebox.os.display.shouldReapplyTvWindowPolicyForChangedDisplay

/** Dedicated standard activity instance for the Android presentation display. */
class TvActivity : GameBoxHostActivity() {
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            if (shouldReapplyTvWindowPolicyForChangedDisplay(this@TvActivity.display?.displayId ?: -1, displayId)) {
                runOnUiThread {
                    if (!isFinishing) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        applyBorderlessFullscreenWindow()
                        hideSystemBars()
                    }
                }
            }
        }

        override fun onDisplayRemoved(displayId: Int) {
            if (shouldFinishTvActivityForRemovedDisplay(this@TvActivity.display?.displayId ?: -1, displayId)) {
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
        applyBorderlessFullscreenWindow()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        hideSystemBars()
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
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        applyBorderlessFullscreenWindow()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun applyBorderlessFullscreenWindow() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )
        window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
    }
}

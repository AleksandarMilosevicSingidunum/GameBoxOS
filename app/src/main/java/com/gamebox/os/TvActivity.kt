package com.gamebox.os

import android.content.pm.ActivityInfo
import android.hardware.display.DisplayManager
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gamebox.os.display.shouldFinishTvActivityForRemovedDisplay

/** Dedicated standard activity instance for the Android presentation display. */
class TvActivity : GameBoxHostActivity() {
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) = Unit

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
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

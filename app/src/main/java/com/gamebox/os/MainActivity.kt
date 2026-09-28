package com.gamebox.os

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Display
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gamebox.os.display.isExternalDisplayId

/** Android Home/launcher host. Its singleTask contract is intentionally preserved. */
class MainActivity : GameBoxHostActivity() {
    private val isExternalDisplay: Boolean
        get() = isExternalDisplayId(display?.displayId ?: Display.DEFAULT_DISPLAY)

    override fun onCreate(savedInstanceState: Bundle?) {
        applyExternalDisplayWindowPolicy()
        super.onCreate(savedInstanceState)
        applyExternalDisplayWindowPolicy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyExternalDisplayWindowPolicy()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        applyExternalDisplayWindowPolicy()
    }

    private fun applyExternalDisplayWindowPolicy() {
        if (!isExternalDisplay) return
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

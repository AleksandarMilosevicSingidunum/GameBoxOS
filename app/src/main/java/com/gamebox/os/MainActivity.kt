package com.gamebox.os

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
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
        if (hasFocus) {
            applyExternalDisplayWindowPolicy()
        }
    }

    override fun onResume() {
        super.onResume()
        applyExternalDisplayWindowPolicy()
    }

    override fun onMultiWindowModeChanged(
        isInMultiWindowMode: Boolean,
        newConfig: android.content.res.Configuration,
    ) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        applyExternalDisplayWindowPolicy()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        applyExternalDisplayWindowPolicy()
    }

    private fun applyExternalDisplayWindowPolicy() {
        if (!isExternalDisplay) return
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

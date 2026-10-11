package com.gamebox.os.display

import android.app.Activity
import android.os.OutcomeReceiver
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Shared edge-to-edge policy for GameBox's Home and external-display hosts. */
internal fun applyBorderlessFullscreenWindow(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    // System bars are hidden with WindowInsetsControllerCompat below. Avoid
    // legacy fullscreen flags so window state has one modern source of truth.
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
    window.setLayout(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
    )
}

internal fun hideSystemBars(window: Window) {
    applyBorderlessFullscreenWindow(window)
    WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
    // Request a fresh dispatch so Compose can account for persistent OEM
    // navigation/taskbar bounds while immersive mode hides standard bars.
    androidx.core.view.ViewCompat.requestApplyInsets(window.decorView)
}

/** Returns the Activity's routed display on all supported Android versions. */
internal fun Activity.currentDisplayId(): Int {
    window.decorView.display?.displayId?.let { return it }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display?.displayId ?: Display.DEFAULT_DISPLAY
    } else {
        Display.DEFAULT_DISPLAY
    }
}

internal fun shouldRequestExternalFullscreenOnFocus(
    displayId: Int,
    hasFocus: Boolean,
): Boolean =
    hasFocus && isExternalDisplayId(displayId)

internal fun shouldRequestPrimaryFullscreenOnFocus(
    displayId: Int,
    hasFocus: Boolean,
    isInMultiWindowMode: Boolean,
): Boolean =
    displayId == Display.DEFAULT_DISPLAY && hasFocus && isInMultiWindowMode

/** Retry after the desktop shell has completed its focus/window-mode transition. */
internal fun Activity.requestExternalFullscreenAfterFocus(delayMillis: Long = 300L) {
    if (!hasWindowFocus() || !isExternalDisplayId(currentDisplayId())) return
    window.decorView.postDelayed({
        if (
            !isFinishing && hasWindowFocus() &&
            shouldRequestExternalFullscreenOnFocus(
                currentDisplayId(),
                hasFocus = true,
            )
        ) {
            requestExternalActivityFullscreen(this)
        }
    }, delayMillis)
}

/** Promote a windowed launcher on the primary display into the TV-style full-screen surface. */
internal fun Activity.requestPrimaryFullscreenAfterFocus(delayMillis: Long = 300L) {
    val displayId = currentDisplayId()
    if (!shouldRequestPrimaryFullscreenOnFocus(displayId, hasWindowFocus(), isInMultiWindowMode)) return
    window.decorView.postDelayed({
        if (
            !isFinishing && currentDisplayId() == displayId && hasWindowFocus() &&
            shouldRequestPrimaryFullscreenOnFocus(
                displayId,
                hasFocus = true,
                isInMultiWindowMode = isInMultiWindowMode,
            )
        ) {
            requestPrimaryActivityFullscreen(this)
        }
    }, delayMillis)
}

internal fun externalFullscreenRetryDelayMillis(retryIndex: Int): Long? =
    when (retryIndex) {
        0 -> 500L
        1 -> 1_200L
        else -> null
    }

/** Ask desktop-mode window managers to promote an external-display host to fullscreen. */
internal fun requestExternalActivityFullscreen(activity: Activity, retryIndex: Int = 0): Boolean {
    val displayId = activity.currentDisplayId()
    if (!isExternalDisplayId(displayId)) return false
    return requestFullscreenModeOnDisplay(activity, displayId, retryIndex)
}

internal fun requestPrimaryActivityFullscreen(activity: Activity, retryIndex: Int = 0): Boolean {
    val displayId = activity.currentDisplayId()
    if (
        !shouldRequestPrimaryFullscreenOnFocus(
            displayId,
            hasFocus = activity.hasWindowFocus(),
            isInMultiWindowMode = activity.isInMultiWindowMode,
        )
    ) return false
    return requestFullscreenModeOnDisplay(activity, displayId, retryIndex, requireMultiWindow = true)
}

private fun requestFullscreenModeOnDisplay(
    activity: Activity,
    displayId: Int,
    retryIndex: Int,
    requireMultiWindow: Boolean = false,
): Boolean {
    Log.d(
        "GameBoxDisplay",
        "Fullscreen request check: sdk=${Build.VERSION.SDK_INT}, display=$displayId, multiWindow=${activity.isInMultiWindowMode}",
    )
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false

    runCatching {
        activity.requestFullscreenMode(
            Activity.FULLSCREEN_MODE_REQUEST_ENTER,
            object : OutcomeReceiver<Void, Throwable> {
                override fun onResult(result: Void?) {
                    Log.i("GameBoxDisplay", "Fullscreen request accepted on display $displayId")
                }

                override fun onError(error: Throwable) {
                    Log.w("GameBoxDisplay", "Fullscreen request rejected on display $displayId", error)
                    if (isRetryableExternalFullscreenError(error)) {
                        externalFullscreenRetryDelayMillis(retryIndex)?.let { delayMillis ->
                            activity.window.decorView.postDelayed({
                                if (
                                    !activity.isFinishing && activity.hasWindowFocus() &&
                                    activity.currentDisplayId() == displayId &&
                                    (!requireMultiWindow || activity.isInMultiWindowMode)
                                ) {
                                    requestFullscreenModeOnDisplay(
                                        activity,
                                        displayId,
                                        retryIndex + 1,
                                        requireMultiWindow,
                                    )
                                }
                            }, delayMillis)
                        }
                    }
                }
            },
        )
    }.onFailure { error ->
        Log.w("GameBoxDisplay", "Fullscreen request failed on display $displayId", error)
    }
    return true
}

internal fun isRetryableExternalFullscreenError(error: Throwable): Boolean =
    error is IllegalStateException &&
        error.message?.contains("not the top focused window", ignoreCase = true) == true

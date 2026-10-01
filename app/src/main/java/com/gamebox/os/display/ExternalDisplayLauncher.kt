package com.gamebox.os.display

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import java.util.Locale
import com.gamebox.os.TvActivity

internal data class ExternalDisplayTarget(
    val displayId: Int,
    val name: String,
    val width: Int = 0,
    val height: Int = 0,
    val isVirtual: Boolean = false,
)

internal val externalDisplayActivityClass = TvActivity::class.java

// ActivityOptions exposes this through a hidden setter on some Android builds. The
// bundle key is understood by the platform window manager and is safely ignored by
// builds that do not support launch-windowing hints.
private const val fullscreenWindowingMode = 1
private const val launchWindowingModeKey = "android.activity.windowingMode"

internal fun ActivityOptions.toGameBoxFullscreenLaunchBundle(): Bundle =
    apply {
        // Some Android desktop shells honor the public bundle hint below,
        // while OEM shells expose the same request only through the hidden
        // ActivityOptions setter. Use the setter opportunistically and retain
        // the bundle key as the compatible fallback.
        runCatching {
            ActivityOptions::class.java
                .getDeclaredMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                .apply { isAccessible = true }
                .invoke(this, fullscreenWindowingMode)
        }
    }
        .toBundle()
        .apply { putInt(launchWindowingModeKey, fullscreenWindowingMode) }

internal fun firstPresentationDisplayTarget(targets: List<ExternalDisplayTarget>): ExternalDisplayTarget? =
    targets.firstOrNull { !it.isVirtual } ?: targets.firstOrNull()

internal fun isLikelyVirtualPresentationDisplay(name: String): Boolean {
    val normalized = name.lowercase(Locale.ROOT)
    return listOf("android studio", "screen sharing", "screen.sharing", "virtual", "capture")
        .any(normalized::contains)
}

internal fun externalDisplayLaunchRequestedMessage(target: ExternalDisplayTarget): String =
    "GameBox TV mode requested on ${target.name}; confirm the display switch if prompted"

internal fun isExternalDisplayId(displayId: Int): Boolean = displayId > Display.DEFAULT_DISPLAY

internal fun shouldFinishTvActivityForRemovedDisplay(
    currentDisplayId: Int,
    removedDisplayId: Int,
): Boolean =
    isExternalDisplayId(currentDisplayId) && currentDisplayId == removedDisplayId

internal fun shouldReapplyTvWindowPolicyForChangedDisplay(
    currentDisplayId: Int,
    changedDisplayId: Int,
): Boolean =
    isExternalDisplayId(currentDisplayId) && currentDisplayId == changedDisplayId

internal fun presentationDisplayTargets(context: Context): List<ExternalDisplayTarget> =
    context.getSystemService(DisplayManager::class.java)
        // Honor Desktop Mode exposes a wired HDMI panel as a normal external
        // display rather than in DISPLAY_CATEGORY_PRESENTATION. Enumerate the
        // complete display inventory so the real TV handoff is not lost on OEM
        // desktop shells; firstPresentationDisplayTarget still prefers a
        // physical panel over screen-sharing/capture displays.
        ?.getDisplays()
        .orEmpty()
        .filter { it.displayId != Display.DEFAULT_DISPLAY }
        .map { display ->
            val mode = display.mode
            ExternalDisplayTarget(
                displayId = display.displayId,
                name = display.name.ifBlank { "External display" },
                width = mode.physicalWidth,
                height = mode.physicalHeight,
                isVirtual = isLikelyVirtualPresentationDisplay(display.name),
            )
        }

internal fun launchGameBoxOnExternalDisplay(activity: Activity): String {
    val target = firstPresentationDisplayTarget(presentationDisplayTargets(activity))
        ?: return "No external display detected"
    return runCatching {
        val options = ActivityOptions.makeBasic().apply {
            launchDisplayId = target.displayId
            // Do not provide launchBounds here. On Android desktop shells,
            // even bounds equal to the physical display can force a freeform
            // task with a caption. The host activities request MATCH_PARENT;
            // the display-targeted fullscreen windowing hint should decide
            // the final surface size.
        }
        val launchOptions = options.toGameBoxFullscreenLaunchBundle()
        // Always target the dedicated TV host. OEM desktop shells can resolve
        // an ACTION_MAIN/CATEGORY_HOME launch back to MainActivity even when
        // the user explicitly requested an external display, which creates a
        // phone/freeform surface instead of the borderless TV host.
        val launchIntent = Intent(activity, externalDisplayActivityClass)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT,
            )
        activity.startActivity(launchIntent, launchOptions)
        externalDisplayLaunchRequestedMessage(target)
    }.getOrElse { error ->
        "Could not open GameBox on ${target.name}: ${error.message ?: "display launch failed"}"
    }
}

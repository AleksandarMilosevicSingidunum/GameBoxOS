package com.gamebox.os.display

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.content.pm.PackageManager
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
    toBundle().apply { putInt(launchWindowingModeKey, fullscreenWindowingMode) }

internal fun firstPresentationDisplayTarget(targets: List<ExternalDisplayTarget>): ExternalDisplayTarget? =
    targets.firstOrNull { !it.isVirtual } ?: targets.firstOrNull()

internal fun isLikelyVirtualPresentationDisplay(name: String): Boolean {
    val normalized = name.lowercase(Locale.ROOT)
    return listOf("android studio", "screen sharing", "screen.sharing", "virtual", "capture")
        .any(normalized::contains)
}

internal fun externalDisplayLaunchRequestedMessage(target: ExternalDisplayTarget): String =
    "Display switch requested for ${target.name}; confirm it if the system prompts you"

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
        ?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
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
            if (target.width > 0 && target.height > 0) {
                launchBounds = Rect(0, 0, target.width, target.height)
            }
        }
        val launchOptions = options.toGameBoxFullscreenLaunchBundle()
        val launchIntent = if (isDefaultHomeApp(activity)) {
            // When GameBox owns HOME, let the system launch it as the actual
            // launcher task on the TV. This produces a HOME surface instead of
            // a normal desktop/freeform activity window.
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } else {
            Intent(activity, externalDisplayActivityClass)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT,
                )
        }
        activity.startActivity(launchIntent, launchOptions)
        if (isDefaultHomeApp(activity)) {
            "GameBox launcher requested on ${target.name}"
        } else {
            externalDisplayLaunchRequestedMessage(target) + "; choose GameBox as the default Home app for launcher mode"
        }
    }.getOrElse { error ->
        "Could not open GameBox on ${target.name}: ${error.message ?: "display launch failed"}"
    }
}

private fun isDefaultHomeApp(context: Context): Boolean {
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val packageName = context.packageManager
        .resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo
        ?.packageName
    return packageName == context.packageName
}

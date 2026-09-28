package com.gamebox.os.display

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.Display
import com.gamebox.os.TvActivity

internal data class ExternalDisplayTarget(
    val displayId: Int,
    val name: String,
)

internal val externalDisplayActivityClass = TvActivity::class.java

internal fun firstPresentationDisplayTarget(targets: List<ExternalDisplayTarget>): ExternalDisplayTarget? =
    targets.firstOrNull()

internal fun presentationDisplayTargets(context: Context): List<ExternalDisplayTarget> =
    context.getSystemService(DisplayManager::class.java)
        ?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        .orEmpty()
        .filter { it.displayId != Display.DEFAULT_DISPLAY }
        .map { ExternalDisplayTarget(it.displayId, it.name.ifBlank { "External display" }) }

internal fun launchGameBoxOnExternalDisplay(activity: Activity): String {
    val target = firstPresentationDisplayTarget(presentationDisplayTargets(activity))
        ?: return "No external display detected"
    return runCatching {
        val options = ActivityOptions.makeBasic().apply {
            launchDisplayId = target.displayId
        }
        activity.startActivity(
            Intent(activity, externalDisplayActivityClass)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT,
                ),
            options.toBundle(),
        )
        "Opened GameBox on ${target.name}"
    }.getOrElse { error ->
        "Could not open GameBox on ${target.name}: ${error.message ?: "display launch failed"}"
    }
}

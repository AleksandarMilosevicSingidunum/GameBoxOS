package com.gamebox.os.ui

import com.gamebox.os.domain.DownloadStatus

internal enum class DownloadControllerAction {
    OPEN_STORE,
    RETRY_FAILED,
    OPEN_SETTINGS,
    PAUSE_ACTIVE,
    RESUME_PAUSED,
}

internal data class DownloadControllerActionPlan(
    val xLabel: String,
    val xAction: DownloadControllerAction,
    val yLabel: String,
    val yAction: DownloadControllerAction,
)

internal fun downloadControllerActionPlan(statuses: Collection<DownloadStatus>): DownloadControllerActionPlan {
    val xAction = if (statuses.any { it == DownloadStatus.FAILED || it == DownloadStatus.CANCELLED }) {
        DownloadControllerAction.RETRY_FAILED
    } else {
        DownloadControllerAction.OPEN_STORE
    }
    val yAction = when {
        DownloadStatus.DOWNLOADING in statuses -> DownloadControllerAction.PAUSE_ACTIVE
        DownloadStatus.PAUSED in statuses -> DownloadControllerAction.RESUME_PAUSED
        else -> DownloadControllerAction.OPEN_SETTINGS
    }
    return DownloadControllerActionPlan(
        xLabel = if (xAction == DownloadControllerAction.RETRY_FAILED) "Retry failed" else "Browse Store",
        xAction = xAction,
        yLabel = when (yAction) {
            DownloadControllerAction.PAUSE_ACTIVE -> "Pause all"
            DownloadControllerAction.RESUME_PAUSED -> "Resume all"
            else -> "Settings"
        },
        yAction = yAction,
    )
}

package com.gamebox.os.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.Game
import com.gamebox.os.download.RemoteDownloadController

@Composable
internal fun DownloadQueueItem(
    job: DownloadJob,
    remoteGame: Game?,
    remoteDownloadController: RemoteDownloadController,
    telemetryDescription: String?,
    capacityWarning: String?,
    primaryActionFocusRequester: FocusRequester?,
    onDismissUnavailable: () -> Unit,
) {
    val primaryActionModifier = primaryActionFocusRequester?.let { focusRequester ->
        Modifier.focusRequester(focusRequester).bringFocusedControlIntoView()
    } ?: Modifier
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics {
            contentDescription = GameBoxSemantics.downloadDescription(job)
            if (job.status == DownloadStatus.FAILED) liveRegion = LiveRegionMode.Assertive
        },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    job.title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                DownloadQueueStatusPill(job.status)
            }
            Spacer(Modifier.height(8.dp))
            DownloadProgressIndicator(job, Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(
                "${formatDownloadBytes(job.downloadedBytes)} of ${formatDownloadBytes(job.totalBytes)}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontSize = 12.sp,
            )
            telemetryDescription?.let { description ->
                Text(
                    description,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.semantics {
                        contentDescription = "Download speed and time remaining: $description"
                    },
                )
            }
            capacityWarning?.let { warning ->
                Text(
                    warning,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.semantics { contentDescription = warning },
                )
            }
            job.errorReason?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (remoteGame != null) {
                    when (job.status) {
                        DownloadStatus.DOWNLOADING -> {
                            BlueprintOutlinedButton(
                                onClick = { remoteDownloadController.pause(remoteGame) },
                                modifier = primaryActionModifier,
                            ) { Text("Pause") }
                        }
                        DownloadStatus.PAUSED -> {
                            BlueprintButton(
                                onClick = { remoteDownloadController.resume(remoteGame) },
                                modifier = primaryActionModifier,
                            ) { Text("Resume") }
                        }
                        DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                            BlueprintButton(
                                onClick = { remoteDownloadController.install(remoteGame) },
                                modifier = primaryActionModifier,
                            ) { Text("Retry") }
                        }
                        else -> Unit
                    }
                    if (job.status !in setOf(
                            DownloadStatus.COMPLETED,
                            DownloadStatus.FAILED,
                            DownloadStatus.CANCELLED,
                    )
                    ) {
                        BlueprintOutlinedButton(
                            onClick = { remoteDownloadController.cancel(remoteGame) },
                            modifier = Modifier.bringFocusedControlIntoView(),
                        ) { Text("Cancel") }
                    }
                } else if (job.status !in setOf(
                        DownloadStatus.COMPLETED,
                        DownloadStatus.FAILED,
                        DownloadStatus.CANCELLED,
                    )
                ) {
                    Column {
                        Text(
                            "This transfer has no available source. Open the game to choose a source or import a copy.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                        )
                        BlueprintOutlinedButton(
                            onClick = onDismissUnavailable,
                        ) {
                            Text("Dismiss unavailable transfer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadQueueStatusPill(status: DownloadStatus) {
    val targetTone = when (status) {
        DownloadStatus.COMPLETED -> Color(0xFF58D68D)
        DownloadStatus.FAILED, DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.error
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.QUEUED,
        DownloadStatus.DOWNLOADING,
        DownloadStatus.VERIFYING,
        DownloadStatus.INSTALLING -> MaterialTheme.colorScheme.primary
    }
    val reducedMotion = LocalReducedMotion.current
    val tone = animateColorAsState(
        targetValue = targetTone,
        animationSpec = if (reducedMotion) snap() else tween(durationMillis = 180),
        label = "download status tone",
    ).value
    Surface(
        color = tone.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).background(tone, CircleShape))
            Text(
                status.displayName().replaceFirstChar { it.uppercase() },
                color = tone,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

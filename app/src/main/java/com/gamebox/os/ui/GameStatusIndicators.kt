package com.gamebox.os.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.InstallState

@Composable
internal fun GameAvailabilityBadge(game: Game, modifier: Modifier = Modifier) {
    val availableFromSource = game.state == InstallState.NOT_INSTALLED && !game.sourceUrl.isNullOrBlank()
    val (label, icon) = when {
        availableFromSource -> "Available" to Icons.Rounded.CloudDownload
        game.state == InstallState.NOT_INSTALLED -> "Import copy" to Icons.Rounded.FolderOpen
        game.state == InstallState.INSTALLED -> "Installed" to Icons.Rounded.CheckCircle
        game.state == InstallState.UPDATE_AVAILABLE -> "Update" to Icons.Rounded.SystemUpdate
        game.state == InstallState.QUEUED -> "Queued" to Icons.Rounded.Schedule
        game.state == InstallState.DOWNLOADING -> "Downloading" to Icons.Rounded.Downloading
        game.state == InstallState.PAUSED -> "Paused" to Icons.Rounded.PauseCircle
        game.state == InstallState.VERIFYING -> "Verifying" to Icons.Rounded.Verified
        game.state == InstallState.INSTALLING -> "Installing" to Icons.Rounded.InstallMobile
        game.state == InstallState.MISSING_FILES -> "Missing files" to Icons.Rounded.FolderOff
        else -> "Retry" to Icons.Rounded.ErrorOutline
    }
    val accent = when {
        game.state == InstallState.INSTALLED -> Color(0xFF58D68D)
        game.state in setOf(InstallState.FAILED, InstallState.MISSING_FILES) -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = modifier.semantics { contentDescription = "Availability: $label" },
        shape = RoundedCornerShape(5.dp),
        color = Color(0xDC07101A),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.82f)),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
            Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun DownloadProgressIndicator(job: DownloadJob, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { job.progress },
        modifier = modifier.semantics {
            contentDescription = "Download progress for " + job.title
            stateDescription = GameBoxSemantics.downloadProgressDescription(job)
            progressBarRangeInfo = ProgressBarRangeInfo(job.progress, 0f..1f)
        },
    )
}

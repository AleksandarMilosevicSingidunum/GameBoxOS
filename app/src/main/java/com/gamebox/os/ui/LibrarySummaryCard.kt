package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.domain.LibrarySummary

@Composable
internal fun LibrarySummaryCard(summary: LibrarySummary) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).semantics {
            contentDescription = "${quantityLabel(summary.totalGames, "game")}, " +
                "${summary.installedGames} installed, ${quantityLabel(summary.favorites, "favorite")}, " +
                "${quantityLabel(summary.totalHoursPlayed, "hour")} played"
        },
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(quantityLabel(summary.totalGames, "game"), fontWeight = FontWeight.Bold)
                Text("${summary.installedGames} installed", modifier = Modifier.weight(1f))
                Text("${summary.favorites} favorites")
            }
            Text(
                "${summary.totalHoursPlayed}h ${summary.remainingMinutes.toString().padStart(2, '0')}m total playtime",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontSize = 12.sp,
            )
            summary.resumeGame?.let { resume ->
                Text(
                    "Ready to resume: ${resume.title}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

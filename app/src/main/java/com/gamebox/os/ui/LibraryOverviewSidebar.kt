package com.gamebox.os.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.LibrarySummary

@Composable
internal fun LibraryOverviewSidebar(
    modifier: Modifier = Modifier,
    summary: LibrarySummary,
    totalStorageBytes: Long,
    freeStorageBytes: Long,
    gamesWithSaves: List<Game>,
) {
    val saveBytes = gamesWithSaves.sumOf(Game::saveSizeBytes)

    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BlueprintPanel(Modifier.fillMaxWidth()) {
            Text("Storage", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Icon(Icons.Rounded.Storage, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text("${formatBytes(freeStorageBytes)} free", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("of ${formatBytes(totalStorageBytes)} device storage", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            LinearProgressIndicator(
                progress = {
                    if (totalStorageBytes <= 0L) 0f
                    else ((totalStorageBytes - freeStorageBytes).toDouble() / totalStorageBytes)
                        .toFloat().coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
            )
        }
        if (gamesWithSaves.isNotEmpty()) {
            BlueprintPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Local Saves", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Icon(Icons.Rounded.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                }
                Text(
                    quantityLabel(gamesWithSaves.size, "game with saves", "games with saves"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
                Text(
                    "${formatBytes(saveBytes)} of detected save data",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                )
                Text("Open a game to manage its backups.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
        BlueprintPanel(Modifier.fillMaxWidth()) {
            Text("Library Overview", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            HomeStatusItem(Icons.Rounded.VideoLibrary, "Games", summary.totalGames.toString())
            HomeStatusItem(Icons.Rounded.Favorite, "Favorites", summary.favorites.toString())
            HomeStatusItem(Icons.Rounded.Schedule, "Play time", "${summary.totalHoursPlayed}h")
        }
    }
}

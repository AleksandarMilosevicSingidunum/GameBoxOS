package com.gamebox.os.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.importer.RomImportProgress

/** Keeps copy-import explanation, progress, cancellation, and disc-set action together. */
@Composable
internal fun DiscoveryImportSection(
    platformLabel: String,
    acceptedFormats: String,
    importing: Boolean,
    progress: RomImportProgress?,
    message: String?,
    supportsDiscSet: Boolean,
    showCopyImport: Boolean = false,
    actionsEnabled: Boolean = true,
    onCancel: () -> Unit,
    onImportCopy: () -> Unit = {},
    onImportDiscSet: () -> Unit,
) {
    if (importing) {
        GameImportProgressActions(
            modifier = Modifier.fillMaxWidth(),
            progress = progress,
            onCancel = onCancel,
        )
    }
    message?.let { status ->
        Text(
            status,
            color = if (status.startsWith("Import failed") || status.startsWith("Import rejected")) {
                MaterialTheme.colorScheme.error
            } else MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics {
                contentDescription = status
                liveRegion = LiveRegionMode.Polite
            },
        )
    }
    BlueprintPanel(Modifier.fillMaxWidth()) {
        Text("Bring your game", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Text("Accepted for $platformLabel: $acceptedFormats", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
        Text(
            "GameBox verifies selected files before adding them to your Library. ZIP and 7z archives are extracted and removed only after successful registration. You still need a compatible emulator and any required firmware or keys.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        if (showCopyImport) {
            BlueprintOutlinedButton(
                onClick = onImportCopy,
                enabled = actionsEnabled && !importing,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text("Import your copy", fontSize = 11.sp)
            }
        }
        if (supportsDiscSet) {
            BlueprintOutlinedButton(
                onClick = onImportDiscSet,
                enabled = actionsEnabled && !importing,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text("Import multi-file disc set", fontSize = 11.sp)
            }
        }
    }
}

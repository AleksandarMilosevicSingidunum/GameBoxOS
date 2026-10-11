package com.gamebox.os.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.settings.GameBoxSettings
import com.gamebox.os.storage.ContentMigrationPlan
import com.gamebox.os.storage.ExternalStorageState
import com.gamebox.os.storage.ExternalStorageStatus

@Composable
internal fun SettingsStorageSection(
    modifier: Modifier = Modifier,
    compact: Boolean,
    currentSettings: GameBoxSettings,
    externalStorageStatus: ExternalStorageStatus?,
    emulatorSaveStorageStatus: ExternalStorageStatus?,
    migrationPlan: ContentMigrationPlan?,
    migrationScanFailed: Boolean = false,
    onChooseExternalLibrary: () -> Unit,
    onForgetExternalLibrary: () -> Unit,
    onMigrateInstalledContent: () -> Unit,
    onRetryMigrationScan: () -> Unit = {},
    onChooseEmulatorSaveRoot: () -> Unit,
    onForgetEmulatorSaveRoot: () -> Unit,
) {
        SettingsSectionHeader("Storage", modifier)
        BlueprintPanel(Modifier.fillMaxWidth()) {
            Text("Keep your games if you uninstall GameBox", fontWeight = FontWeight.SemiBold)
            Text(
                "Imported games use app storage by default. Uninstalling GameBox removes that local storage. Choose a shared library and migrate your games to preserve the files independently.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        Text("External game library", fontWeight = FontWeight.Bold)
        Text(
            externalStorageStatus?.displayName ?: externalStorageStatus?.message ?: "Checking external library…",
            modifier = Modifier.semantics { contentDescription = "External game library: " + (externalStorageStatus?.displayName ?: externalStorageStatus?.message ?: "Checking") },
            color = when (externalStorageStatus?.state) {
                ExternalStorageState.AVAILABLE_READ_WRITE -> MaterialTheme.colorScheme.primary
                ExternalStorageState.AVAILABLE_READ_ONLY -> MaterialTheme.colorScheme.tertiary
                ExternalStorageState.NOT_CONFIGURED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                null -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.error
            }
        )
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BlueprintButton(
                    onClick = { onChooseExternalLibrary() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (currentSettings.externalLibraryUri.isBlank()) "Choose folder" else "Change folder")
                }
                if (currentSettings.externalLibraryUri.isNotBlank()) {
                    BlueprintOutlinedButton(
                        onClick = {
                            onForgetExternalLibrary()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Forget") }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BlueprintButton(onClick = { onChooseExternalLibrary() }) {
                    Text(if (currentSettings.externalLibraryUri.isBlank()) "Choose folder" else "Change folder")
                }
                if (currentSettings.externalLibraryUri.isNotBlank()) {
                    BlueprintOutlinedButton(onClick = {
                        onForgetExternalLibrary()
                    }) { Text("Forget") }
                }
            }
        }
        val currentPlan = migrationPlan
        BlueprintButton(
            onClick = { onMigrateInstalledContent() },
            enabled = !migrationScanFailed && currentPlan != null && !currentPlan.isEmpty &&
                externalStorageStatus?.state == ExternalStorageState.AVAILABLE_READ_WRITE,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).semantics { contentDescription = "Migrate installed content" }
        ) {
            Text(
                when {
                    migrationScanFailed -> "Installed content scan failed"
                    currentPlan == null -> "Scanning installed content…"
                    currentPlan.isEmpty -> "No installed content to migrate"
                    else -> "Migrate " + formatBytes(currentPlan.totalBytes)
                }
            )
        }
        Text(
            "Migration verifies each installed file in the selected library, switches playback to it, then removes the matching phone copy.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontSize = 12.sp
        )
        if (migrationScanFailed) {
            Text("Check that installed files are accessible, then retry the scan.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp)
            BlueprintTextButton(onClick = onRetryMigrationScan) { Text("Retry scan") }
        }
        Spacer(Modifier.height(14.dp))
        Text("Emulator save root", fontWeight = FontWeight.Bold)
        Text(
            emulatorSaveStorageStatus?.displayName ?: emulatorSaveStorageStatus?.message ?: "Checking emulator save root…",
            modifier = Modifier.semantics {
                contentDescription = "Emulator save root: " +
                    (emulatorSaveStorageStatus?.displayName ?: emulatorSaveStorageStatus?.message ?: "Checking")
            },
            color = when (emulatorSaveStorageStatus?.state) {
                ExternalStorageState.AVAILABLE_READ_WRITE -> MaterialTheme.colorScheme.primary
                ExternalStorageState.NOT_CONFIGURED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                null -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.error
            },
        )
        Text(
            "Select the emulator's writable save folder. GameBox uses <root>/<game-id> and keeps this permission separate from the game library.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontSize = 12.sp,
        )
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BlueprintButton(
                    onClick = { onChooseEmulatorSaveRoot() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (currentSettings.emulatorSaveRootUri.isBlank()) "Choose save folder" else "Change save folder")
                }
                if (currentSettings.emulatorSaveRootUri.isNotBlank()) {
                    BlueprintOutlinedButton(
                        onClick = {
                            onForgetEmulatorSaveRoot()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Forget save root") }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BlueprintButton(onClick = { onChooseEmulatorSaveRoot() }) {
                    Text(if (currentSettings.emulatorSaveRootUri.isBlank()) "Choose save folder" else "Change save folder")
                }
                if (currentSettings.emulatorSaveRootUri.isNotBlank()) {
                    BlueprintOutlinedButton(onClick = {
                        onForgetEmulatorSaveRoot()
                    }) { Text("Forget save root") }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
}

package com.gamebox.os.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.gamebox.os.diagnostics.*
import com.gamebox.os.domain.*
import com.gamebox.os.settings.*
import kotlinx.coroutines.launch

@Composable
internal fun SettingsDeveloperDiagnosticsSection(
    currentSettings: GameBoxSettings,
    settingsRepository: SettingsRepository,
    games: List<Game>,
    downloads: List<DownloadJob>,
    runtimeStatus: RuntimeDeviceStatus,
    storageCapacity: StorageCapacitySnapshot?,
    modifier: Modifier = Modifier,
    messageState: MutableState<String?>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val diagnosticEvents = remember { DiagnosticEventCollector() }
    val diagnosticsReport = buildDiagnosticsReport(
        device = DiagnosticsDevice(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            sdk = Build.VERSION.SDK_INT,
            appVersion = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName ?: "unknown",
            usableBytes = storageCapacity?.availableBytes ?: 0L,
            totalBytes = storageCapacity?.totalBytes ?: 0L,
        ),
        settings = currentSettings,
        games = games,
        downloads = downloads,
        runtime = DiagnosticsRuntime(
            controllerCount = runtimeStatus.controllerNames.size,
            audioOutput = runtimeStatus.audioOutputLabel,
            networkTransport = runtimeStatus.networkTransportLabel,
            networkState = runtimeStatus.networkLabel,
            externalDisplayCount = runtimeStatus.externalDisplayNames.size,
            externalDisplaySummary = runtimeStatus.externalDisplayLabel,
            batteryPercent = runtimeStatus.batteryPercent,
            powerState = runtimeStatus.powerLabel,
        ),
    )
    val diagnosticsBundle = remember(diagnosticsReport, diagnosticEvents.snapshot()) {
        buildDiagnosticsRecoveryBundle(diagnosticsReport, diagnosticEvents.snapshot())
    }
    val diagnosticsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.use { it.write(diagnosticsBundle) }
                    ?: error("Unable to open diagnostics destination")
            }
            if (result.isSuccess) {
                diagnosticEvents.record(DiagnosticLevel.INFO, "diagnostics_export", "Recovery bundle exported")
            } else {
                diagnosticEvents.record(DiagnosticLevel.ERROR, "diagnostics_export", result.exceptionOrNull()?.message ?: "Export failed")
            }
            messageState.value = if (result.isSuccess) "Sanitized recovery bundle exported"
            else "Diagnostics export failed"
        }
    }
        SettingsSectionHeader("Developer and diagnostics", modifier)
        val allowDeveloperOptions = context.applicationInfo.flags and
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (allowDeveloperOptions) {
            Text("Layout simulation", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DeveloperLayoutMode.entries.forEach { mode ->
                    BlueprintFilterChip(
                        selected = currentSettings.developerLayoutMode == mode,
                        onClick = { scope.launch { settingsRepository.setDeveloperLayoutMode(mode) } },
                        label = { Text(when (mode) {
                            DeveloperLayoutMode.AUTO -> "Automatic"
                            DeveloperLayoutMode.WIDE -> "TV / DeX"
                            DeveloperLayoutMode.COMPACT -> "Phone"
                        }) },
                    )
                }
            }
            Text("Catalog state simulation", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CatalogFailureSimulation.entries.forEach { simulation ->
                    BlueprintFilterChip(
                        selected = currentSettings.catalogFailureSimulation == simulation,
                        onClick = { scope.launch { settingsRepository.setCatalogFailureSimulation(simulation) } },
                        label = { Text(when (simulation) {
                            CatalogFailureSimulation.LIVE -> "Live"
                            CatalogFailureSimulation.OFFLINE_FALLBACK -> "Offline"
                            CatalogFailureSimulation.REMOTE_FALLBACK -> "Provider unavailable"
                            CatalogFailureSimulation.ERROR -> "Error"
                        }) },
                    )
                }
            }
            Text(
                "Simulated catalog states affect presentation only; they never replace, delete, or corrupt cached provider data.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Focus diagnostics overlay")
                Text(
                    "Shows the focused Blueprint control, live bounds, and nearest directional targets.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 12.sp,
                )
            }
            BlueprintSwitch(
                checked = currentSettings.focusDebugOverlayEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { settingsRepository.setFocusDebugOverlayEnabled(enabled) }
                },
                modifier = Modifier.semantics {
                    contentDescription = "Focus diagnostics overlay"
                },
            )
            }
        }
        Text(
            "Export a sanitized recovery bundle when troubleshooting. Credentials, remote URLs, checksums, paths, and save contents are excluded.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontSize = 12.sp
        )
        BlueprintOutlinedButton(
            onClick = { diagnosticsLauncher.launch("gamebox-diagnostics.zip") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export sanitized recovery bundle", modifier = Modifier.fillMaxWidth())
        }
}

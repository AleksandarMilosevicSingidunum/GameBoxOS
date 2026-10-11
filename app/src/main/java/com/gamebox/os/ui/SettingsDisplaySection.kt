package com.gamebox.os.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.settings.GameBoxSettings
import com.gamebox.os.display.isLikelyVirtualPresentationDisplay
import kotlin.math.roundToInt

internal fun externalDisplayTaskbarGuidance(
    manufacturer: String,
    externalDisplayNames: List<String>,
): String? {
    val isHonor = manufacturer.contains("HONOR", ignoreCase = true)
    val hasPhysicalExternalDisplay = externalDisplayNames.any { name ->
        !isLikelyVirtualPresentationDisplay(name)
    }
    if (!isHonor || !hasPhysicalExternalDisplay) return null
    return "For GameBox and each emulator on HONOR Desktop Mode, open the app's Dock menu and choose Open in full screen mode to hide the Dock. This is an HONOR per-app setting; GameBox cannot change it or force third-party emulators into fullscreen. To hide the Dock globally, use Settings > Large screen features > Global Taskbar when available."
}

@Composable
internal fun SettingsDisplaySection(
    modifier: Modifier = Modifier,
    compact: Boolean,
    currentSettings: GameBoxSettings,
    runtimeStatus: RuntimeDeviceStatus,
    onSafeAreaChange: (Float) -> Unit,
    onOpenAndroidDisplaySettings: () -> Unit,
) {
        SettingsSectionHeader("Display", modifier)
        externalDisplayTaskbarGuidance(
            manufacturer = android.os.Build.MANUFACTURER,
            externalDisplayNames = runtimeStatus.externalDisplayNames,
        )?.let { guidance ->
            SettingsStatusCard(
                Icons.Rounded.Monitor,
                "HONOR Dock controls",
                guidance,
            )
        }
        SettingsStatusCard(
            Icons.Rounded.Monitor,
            "Responsive display mode",
            if (compact) "Phone layout active" else "Wide / DeX layout active",
        )
        SettingsStatusCard(
            Icons.Rounded.Cable,
            if (runtimeStatus.externalDisplayNames.isEmpty()) "External display not detected"
            else "External display detected",
            runtimeStatus.externalDisplayLabel,
        )
        SettingsStatusCard(
            Icons.Rounded.BatteryChargingFull,
            "Phone power",
            runtimeStatus.powerLabel,
        )
        Text("TV safe area", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Text(
            "Keeps the complete GameBox viewport inside a television's visible area. This setting applies only to wide TV / DeX layouts.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontSize = 12.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(top = 7.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(0f, 0.02f, 0.04f, 0.06f, 0.08f, 0.1f).forEach { percent ->
                val label = if (percent == 0f) "Off" else (percent * 100).roundToInt().toString() + "%"
                BlueprintFilterChip(
                    selected = kotlin.math.abs(currentSettings.safeAreaPercent - percent) < 0.001f,
                    onClick = { onSafeAreaChange(percent) },
                    label = { Text(label) },
                    modifier = Modifier.semantics {
                        contentDescription = "TV safe area $label"
                    }.bringFocusedControlIntoView().focusable()
                        .onBlueprintActivation { onSafeAreaChange(percent) },
                )
            }
        }
        Text(
            if (compact) "Preview this setting in TV / DeX layout."
            else "Active inset: " + (currentSettings.safeAreaPercent * 100).roundToInt() + "% on every edge.",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
        )
        SettingsActionRow("Open Android display settings", Icons.Rounded.Monitor) {
            onOpenAndroidDisplaySettings()
        }
        Spacer(Modifier.height(18.dp))
}

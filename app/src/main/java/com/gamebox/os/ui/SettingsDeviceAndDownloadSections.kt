package com.gamebox.os.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.settings.GameBoxSettings

@Composable
internal fun SettingsDeviceAndDownloadSections(
    connectedControllers: Int,
    runtimeStatus: RuntimeDeviceStatus,
    activeDownloads: Int,
    currentSettings: GameBoxSettings,
    onUnmeteredDownloadsChange: (Boolean) -> Unit,
    onEnableNotifications: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenEmulatorSettings: () -> Unit,
    modifier: Modifier = Modifier,
    downloadsModifier: Modifier = Modifier,
    emulatorsModifier: Modifier = Modifier,
) {
    SettingsSectionHeader("Controllers", modifier)
    SettingsStatusCard(
        Icons.Rounded.SportsEsports,
        if (connectedControllers == 1) "1 game controller connected" else "$connectedControllers game controllers connected",
        runtimeStatus.controllerLabel + ". Pair controllers in Android, then return to GameBox.",
    )
    SettingsActionRow("Open Bluetooth controller settings", Icons.Rounded.Bluetooth, onClick = onOpenBluetoothSettings)

    Spacer(Modifier.height(18.dp))
    SettingsSectionHeader("Downloads", downloadsModifier)
    SettingsStatusCard(
        Icons.Rounded.Downloading,
        if (activeDownloads == 1) "1 active download" else "$activeDownloads active downloads",
        "Queued transfers are durable and resume through WorkManager after an app or device restart.",
    )
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Unmetered downloads only", fontWeight = FontWeight.SemiBold)
            Text(
                if (currentSettings.downloadsUnmeteredOnly)
                    "Remote game transfers wait for Wi-Fi, Ethernet, or another unmetered connection."
                else
                    "Remote game transfers may use metered or mobile data.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp,
            )
        }
        BlueprintSwitch(
            checked = currentSettings.downloadsUnmeteredOnly,
            onCheckedChange = onUnmeteredDownloadsChange,
            modifier = Modifier.semantics { contentDescription = "Unmetered downloads only" },
        )
    }
    Spacer(Modifier.height(12.dp))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        BlueprintOutlinedButton(
            onClick = onEnableNotifications,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
            Text("Enable download notifications", modifier = Modifier.fillMaxWidth())
        }
    }

    Spacer(Modifier.height(18.dp))
    SettingsSectionHeader("Emulators", emulatorsModifier)
    SettingsStatusCard(
        Icons.Rounded.Memory,
        "Approved emulator adapters",
        "RetroArch, PPSSPP, Dolphin, DuckStation, M64Plus FZ and Flycast are detected at launch time. Game Details explains missing cores or adapters.",
    )
    SettingsActionRow("Manage installed emulator apps", Icons.Rounded.Apps, onClick = onOpenEmulatorSettings)
}

@Composable
internal fun SettingsAudioSection(
    outputLabel: String,
    modifier: Modifier = Modifier,
    onOpenAndroidSoundSettings: () -> Unit,
) {
    SettingsSectionHeader("Audio", modifier)
    SettingsStatusCard(
        Icons.AutoMirrored.Rounded.VolumeUp,
        "System audio · $outputLabel",
        "GameBox respects Android media volume and the active output route.",
    )
    SettingsActionRow("Open Android sound settings", Icons.AutoMirrored.Rounded.VolumeUp, onClick = onOpenAndroidSoundSettings)
}

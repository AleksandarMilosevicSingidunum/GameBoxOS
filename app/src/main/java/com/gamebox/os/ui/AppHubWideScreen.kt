package com.gamebox.os.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.gamebox.os.launch.MoonlightStatus
import com.gamebox.os.settings.SettingsRepository
import com.gamebox.os.settings.ShortcutLaunchRecord
import com.gamebox.os.ui.theme.GameBoxControllerBack
import com.gamebox.os.ui.theme.GameBoxControllerConfirm

@Composable
internal fun AppHubWideScreen(
    hubKind: AppHubKind,
    subtitle: String,
    shortcuts: List<AppShortcut>,
    installedPackages: Set<String>,
    moonlightStatus: MoonlightStatus,
    settingsRepository: SettingsRepository,
    recentLaunches: List<ShortcutLaunchRecord>,
    message: String?,
    onLaunch: (AppShortcut) -> Unit,
    onExternalDisplay: () -> Unit,
) {
    val isPc = hubKind == AppHubKind.PC
    val context = LocalContext.current
    val audio = context.getSystemService(android.media.AudioManager::class.java)
    val volume = audio?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 0
    val volumeMax = audio?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 1
    val runtimeStatus = LocalRuntimeDeviceStatus.current
    val networkLabel = runtimeStatus.networkLabel
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (isPc) "PC Hub" else "Your Media Hub", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            if (shortcuts.isEmpty()) {
                BlueprintPanel(Modifier.fillMaxWidth()) {
                    Text("No shortcuts available. Enable unavailable shortcuts in Settings for setup guidance.", fontSize = 12.sp)
                }
            } else if (isPc) {
                val moonlight = shortcuts.firstOrNull {
                    it.role == AppShortcutRole.FEATURED_PC_STREAMING
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (moonlight != null) {
                        BlueprintPcHero(
                            moonlight,
                            moonlight.packageName in installedPackages,
                            { onLaunch(moonlight) },
                            Modifier.weight(1.25f).height(272.dp),
                        )
                    }
                    Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        shortcuts.filterNot {
                            it.role == AppShortcutRole.FEATURED_PC_STREAMING
                        }.chunked(3).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { shortcut ->
                                    BlueprintShortcutTile(
                                        shortcut,
                                        shortcut.packageName in installedPackages,
                                        Modifier.weight(1f).height(131.dp),
                                    ) { onLaunch(shortcut) }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                Text("Connection setup", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                MoonlightHostProbePanel(settingsRepository, Modifier.fillMaxWidth())
                Text("Recent sessions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                BlueprintPanel(Modifier.fillMaxWidth()) {
                    if (moonlightStatus.recentSessions.isEmpty()) {
                        Text("No Moonlight sessions opened yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    moonlightStatus.recentSessions.take(3).forEach { Text(it, fontSize = 11.sp) }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(3.dp)) {
                    items(shortcuts, key = { it.packageName }) { shortcut ->
                        BlueprintShortcutTile(
                            shortcut,
                            shortcut.packageName in installedPackages,
                            Modifier.width(128.dp).height(
                                128.dp + 48.dp * (androidx.compose.ui.platform.LocalDensity.current.fontScale - 1f)
                                    .coerceAtLeast(0f),
                            ),
                        ) { onLaunch(shortcut) }
                    }
                }
                Text("Ready on this device", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                val ready = shortcuts.filter { it.packageName in installedPackages }
                if (ready.isEmpty()) {
                    BlueprintPanel(Modifier.fillMaxWidth()) {
                        Text("Install a media app to enable one-click launch.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(ready, key = { it.packageName }) { shortcut ->
                            ShortcutCard(shortcut, true, Modifier.width(220.dp)) { onLaunch(shortcut) }
                        }
                    }
                }
                BlueprintPanel(Modifier.fillMaxWidth()) {
                    Text("Recently opened", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (recentLaunches.isEmpty()) {
                        Text(
                            "No media apps opened yet. GameBox does not read private viewing history.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    } else {
                        val shortcutNames = shortcuts.associateBy(AppShortcut::packageName)
                        recentLaunches.take(3).forEach { launch ->
                            shortcutNames[launch.packageName]?.let { shortcut ->
                                Text(
                                    shortcut.title + " · " + formatShortcutLaunchTime(launch.launchedAtEpochMs),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
            }
            Text("Quick actions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BlueprintOutlinedButton(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) } }) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, Modifier.size(16.dp))
                    Text("Audio", Modifier.padding(start = 6.dp), fontSize = 11.sp)
                }
                BlueprintOutlinedButton(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) } }) {
                    Icon(Icons.Rounded.SportsEsports, null, Modifier.size(16.dp))
                    Text("Controllers", Modifier.padding(start = 6.dp), fontSize = 11.sp)
                }
                shortcuts.firstOrNull { it.role == AppShortcutRole.FILE_MANAGER }?.let { files ->
                    BlueprintOutlinedButton(onClick = { onLaunch(files) }) { Text("Open files", fontSize = 11.sp) }
                }
                if (isPc) {
                    Text(
                        "The system may ask you to confirm the display switch.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                    BlueprintOutlinedButton(
                        onClick = { runCatching { context.startActivity(desktopHomeIntent()) } },
                        modifier = Modifier.semantics { contentDescription = "Return to Android or DeX desktop" },
                    ) {
                        Icon(Icons.Rounded.DesktopWindows, null, Modifier.size(16.dp))
                        Text("Desktop mode", Modifier.padding(start = 6.dp), fontSize = 11.sp)
                    }
                }
            }
            message?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite },
                )
            }
        }
        Column(Modifier.width(198.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isPc) {
                BlueprintOutlinedButton(
                    onClick = onExternalDisplay,
                    modifier = Modifier.fillMaxWidth()
                        .semantics { contentDescription = "Show GameBox on external display" },
                ) {
                    Icon(Icons.Rounded.Cast, null, Modifier.size(16.dp))
                    Text("Show on TV", Modifier.padding(start = 6.dp), fontSize = 11.sp)
                }
                HomeStatusItem(Icons.Rounded.DesktopWindows, "PC streaming", if (moonlightStatus.moonlightInstalled) "Moonlight installed" else "Install Moonlight")
                HomeStatusItem(Icons.Rounded.Wifi, "Connection", networkLabel)
                HomeStatusItem(Icons.Rounded.SportsEsports, "Controller", connectedControllerLabel())
                HomeStatusItem(Icons.Rounded.Smartphone, "Android ${Build.VERSION.RELEASE}", Build.MODEL)
            } else {
                HomeStatusItem(
                    Icons.AutoMirrored.Rounded.VolumeUp,
                    "Media volume",
                    "${(volume * 100 / volumeMax.coerceAtLeast(1))}% · ${runtimeStatus.audioOutputLabel}",
                    volume.toFloat() / volumeMax.coerceAtLeast(1),
                )
                HomeStatusItem(Icons.Rounded.Wifi, "Network", networkLabel)
                HomeStatusItem(Icons.Rounded.SportsEsports, "Controller", connectedControllerLabel())
                BlueprintPanel(Modifier.fillMaxWidth()) {
                    Text("Remote & controller", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    ControllerHint("A", "Open app", GameBoxControllerConfirm)
                    ControllerHint("B", "Back", GameBoxControllerBack)
                    Text("LB / RB  Change tab", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
    }
}

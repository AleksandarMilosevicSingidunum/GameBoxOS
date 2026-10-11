package com.gamebox.os.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.gamebox.os.display.launchGameBoxOnExternalDisplay
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.launch

@Composable
internal fun AppHubScreen(
    title: String,
    subtitle: String,
    shortcuts: List<AppShortcut>,
    compact: Boolean,
    settingsRepository: SettingsRepository,
    hubKind: AppHubKind,
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val currentSettings by settingsRepository.settings.collectAsState(
        initial = com.gamebox.os.settings.GameBoxSettings()
    )
    val controllerActions = LocalControllerActions.current
    val scope = rememberCoroutineScope()
    val controllerLabels = hubControllerLabels(hubKind, currentSettings.showUnavailableShortcuts)
    DisposableEffect(controllerActions, controllerLabels) {
        val lease = controllerActions?.configure(
            xLabel = controllerLabels.xLabel,
            onX = {
                scope.launch {
                    settingsRepository.setShowUnavailableShortcuts(
                        !currentSettings.showUnavailableShortcuts
                    )
                }
            },
            yLabel = controllerLabels.yLabel,
            onY = {
                val intent = if (hubKind == AppHubKind.PC) desktopHomeIntent()
                    else Intent(Settings.ACTION_SOUND_SETTINGS)
                runCatching { context.startActivity(intent) }
            },
        )
        onDispose {
            val owner = controllerActions
            if (owner != null && lease != null) owner.clear(lease)
        }
    }
    val launchIntents = remember(shortcuts) {
        shortcuts.associate { shortcut ->
            shortcut.packageName to if (shortcut.packageName == "__android_home__") {
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                context.packageManager.getLaunchIntentForPackage(shortcut.packageName)
            }
        }
    }
    val installedPackages = launchIntents.filterValues { it != null }.keys
    val visiblePackages = visibleShortcutPackageNames(
        shortcuts.map { it.packageName },
        installedPackages,
        currentSettings.showUnavailableShortcuts
    ).toSet()
    val visibleShortcuts = shortcuts.filter {
        it.packageName in visiblePackages &&
            it.packageName !in currentSettings.hiddenShortcutPackages
    }
    var message by remember { mutableStateOf<String?>(null) }
    val shortcutsByPackage = remember(shortcuts) { shortcuts.associateBy(AppShortcut::packageName) }
    val recentLaunches = currentSettings.recentShortcutLaunches
        .filter { it.packageName in shortcutsByPackage }
    val runtimeStatus = LocalRuntimeDeviceStatus.current
    val moonlightStatus = remember(shortcuts, installedPackages, recentLaunches, runtimeStatus.networkState) {
        moonlightStatusForHub(shortcuts, installedPackages, recentLaunches, runtimeStatus.networkState)
    }
    fun openInstallListing(shortcut: AppShortcut): String {
        val encodedPackage = Uri.encode(shortcut.packageName)
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=" + encodedPackage),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(marketIntent)
            return "Opened store listing for " + shortcut.title
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + encodedPackage),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                context.startActivity(webIntent)
                "Opened install page for " + shortcut.title
            } catch (_: ActivityNotFoundException) {
                "No app store or browser is available to install " + shortcut.title
            }
        }
    }
    fun launchShortcut(shortcut: AppShortcut) {
        val launchIntent = launchIntents[shortcut.packageName]
        if (launchIntent == null) {
            message = if (shortcut.role == AppShortcutRole.FEATURED_PC_STREAMING) {
                openInstallListing(shortcut)
            } else {
                shortcut.title + " is not installed"
            }
        } else {
            try {
                context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                scope.launch {
                    settingsRepository.recordShortcutLaunch(shortcut.packageName)
                }
                message = "Opened " + shortcut.title
            } catch (_: ActivityNotFoundException) {
                message = "Unable to open " + shortcut.title
            }
        }
    }
    if (!compact) {
        AppHubWideScreen(
            hubKind = hubKind,
            subtitle = subtitle,
            shortcuts = visibleShortcuts,
            installedPackages = installedPackages,
            moonlightStatus = moonlightStatus,
            settingsRepository = settingsRepository,
            recentLaunches = recentLaunches,
            message = message,
            onLaunch = ::launchShortcut,
            onExternalDisplay = { message = activity?.let(::launchGameBoxOnExternalDisplay) ?: "External display launch is unavailable" },
        )
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(restoredScrollState("hub." + title.lowercase()))) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        if (hubKind == AppHubKind.PC) {
            MoonlightStatusPanel(moonlightStatus, compact = true)
            Spacer(Modifier.height(12.dp))
            MoonlightHostProbePanel(settingsRepository, Modifier.fillMaxWidth())
            BlueprintOutlinedButton(
                onClick = { runCatching { context.startActivity(desktopHomeIntent()) } },
                modifier = Modifier.semantics { contentDescription = "Return to Android or DeX desktop" },
            ) {
                Icon(Icons.Rounded.DesktopWindows, null, Modifier.size(16.dp))
                Text("Desktop mode", Modifier.padding(start = 6.dp))
            }
            BlueprintOutlinedButton(
                onClick = { message = activity?.let(::launchGameBoxOnExternalDisplay) ?: "External display launch is unavailable" },
                modifier = Modifier.semantics { contentDescription = "Show GameBox on external display" },
            ) {
                Icon(Icons.Rounded.Cast, null, Modifier.size(16.dp))
                Text("Show on TV", Modifier.padding(start = 6.dp))
            }
            Text(
                "The system may ask you to confirm the display switch on the TV.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(18.dp))
        } else {
            Spacer(Modifier.height(18.dp))
        }
        if (visibleShortcuts.isEmpty()) {
            Text(
                if (shortcuts.all { it.packageName in currentSettings.hiddenShortcutPackages })
                    "All shortcuts in this hub are hidden. Choose visible apps in Settings."
                else "No installed shortcuts are available. Enable unavailable shortcuts in Settings to see setup guidance.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }
        visibleShortcuts.forEach { shortcut ->
            ShortcutCard(
                shortcut = shortcut,
                installed = launchIntents[shortcut.packageName] != null,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                onClick = { launchShortcut(shortcut) },
            )
        }
        if (recentLaunches.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Recently opened", fontWeight = FontWeight.SemiBold)
            recentLaunches.take(3).forEach { launch ->
                val shortcut = shortcutsByPackage[launch.packageName]
                if (shortcut != null) {
                    Text(
                        shortcut.title + " · " + formatShortcutLaunchTime(launch.launchedAtEpochMs),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
    }
}

internal fun desktopHomeIntent(): Intent =
    Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_HOME)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

package com.gamebox.os.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.settings.GameBoxSettings
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.launch

@Composable
internal fun SettingsInterfaceSection(
    currentSettings: GameBoxSettings,
    settingsRepository: SettingsRepository,
) {
    val scope = rememberCoroutineScope()
    Text("Interface", fontWeight = FontWeight.Bold)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text("Show unavailable app shortcuts")
            Text(
                "When disabled, Media and PC Hub show only installed apps.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp
            )
        }
        BlueprintSwitch(
            checked = currentSettings.showUnavailableShortcuts,
            onCheckedChange = { show ->
                scope.launch { settingsRepository.setShowUnavailableShortcuts(show) }
            },
            modifier = Modifier.semantics {
                contentDescription = "Show unavailable app shortcuts"
            }
        )
    }
    Spacer(Modifier.height(8.dp))
    Text("Visible Media and PC apps", fontWeight = FontWeight.SemiBold)
    Text(
        "Choose the apps shown in your hubs independently of whether setup guidance is visible.",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        fontSize = 12.sp,
    )
    (mediaShortcuts + pcShortcuts).distinctBy(AppShortcut::packageName).forEach { shortcut ->
        Row(
            Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(shortcut.title, fontSize = 13.sp)
                Text(
                    if (shortcut in mediaShortcuts) "Media" else "PC Hub",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
            BlueprintSwitch(
                checked = shortcut.packageName !in currentSettings.hiddenShortcutPackages,
                onCheckedChange = { visible ->
                    scope.launch { settingsRepository.setShortcutVisible(shortcut.packageName, visible) }
                },
                modifier = Modifier.semantics {
                    contentDescription = shortcut.title + " shortcut visible"
                },
            )
        }
    }
    BlueprintOutlinedButton(
        enabled = currentSettings.hiddenShortcutPackages.isNotEmpty(),
        onClick = { scope.launch { settingsRepository.showAllShortcuts() } },
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Text("Show all Media and PC apps")
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("UI sound effects")
            Text(
                "Play Android's subtle click sound when activating GameBox controls. Device sound-effect settings still apply.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp,
            )
        }
        BlueprintSwitch(
            checked = currentSettings.uiSoundEffectsEnabled,
            onCheckedChange = { enabled ->
                scope.launch { settingsRepository.setUiSoundEffectsEnabled(enabled) }
            },
            modifier = Modifier.semantics { contentDescription = "UI sound effects" },
        )
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Reduce motion")
            Text(
                "Disables screen-entry and focus scaling animations while keeping focus borders and state colors.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp,
            )
        }
        BlueprintSwitch(
            checked = currentSettings.reducedMotion,
            onCheckedChange = { reduce ->
                scope.launch { settingsRepository.setReducedMotion(reduce) }
            },
            modifier = Modifier.semantics { contentDescription = "Reduce motion" },
        )
    }
    Spacer(Modifier.height(18.dp))
}

package com.gamebox.os.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.gamebox.os.data.GameRepository
import com.gamebox.os.display.currentDisplayId
import com.gamebox.os.domain.Game
import com.gamebox.os.launch.EmulatorCapabilityRegistry
import com.gamebox.os.launch.emulatorPackageCandidates
import com.gamebox.os.launch.EmulatorReadinessPolicy
import com.gamebox.os.launch.EmulatorReadinessState
import com.gamebox.os.launch.startIntentOnDisplay
import com.gamebox.os.settings.GameBoxSettings
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun GameSettingsPanel(
    game: Game,
    repository: GameRepository,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val registry = EmulatorCapabilityRegistry()
    val options = androidx.compose.runtime.remember(game.platform) { registry.optionsFor(game) }
    val context = LocalContext.current
    val activity = LocalActivity.current
    val packageManager = context.packageManager
    var launchError by remember(game.id) { mutableStateOf<String?>(null) }
    val settings by settingsRepository.settings.collectAsState(initial = GameBoxSettings())
    val scope = rememberCoroutineScope()
    val platformDefault = settings.platformEmulatorDefaults[
        game.platform.lowercase().filter(Char::isLetterOrDigit)
    ]?.takeIf { it in options }
    val effectivePackage = game.emulatorPackage ?: platformDefault
    val installedPackages = options
        .filter { option ->
            emulatorPackageCandidates(option).any { packageManager.getLaunchIntentForPackage(it) != null }
        }
        .toSet()
    val readiness = EmulatorReadinessPolicy.evaluate(
        approvedOptions = options,
        selectedPackage = effectivePackage,
        installedPackages = installedPackages,
        displayName = registry::displayName,
    )

    BlueprintPanel(modifier.fillMaxWidth()) {
        Text("Game settings", style = MaterialTheme.typography.titleLarge)
        Text("Choose an emulator for this game or its platform.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text("Platform default · " + game.platform, fontWeight = FontWeight.SemiBold)
        Text(
            platformDefault?.let { registry.optionDisplayName(it) } ?: "Automatic recommended option",
        )
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            BlueprintFilterChip(
                selected = platformDefault == null,
                onClick = { scope.launch { settingsRepository.setPlatformEmulatorDefault(game.platform, null) } },
                label = { Text("Automatic") },
            )
            options.forEach { pkg ->
                val installed = pkg in installedPackages
                BlueprintFilterChip(
                    selected = platformDefault == pkg,
                    onClick = { scope.launch { settingsRepository.setPlatformEmulatorDefault(game.platform, pkg) } },
                    enabled = installed,
                    label = {
                        Text(registry.optionDisplayName(pkg) + if (installed) " · Installed" else " · Not installed")
                    },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("This game", fontWeight = FontWeight.SemiBold)
        Text(if (game.emulatorPackage == null) "Uses platform default" else "Overrides platform default")
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            BlueprintFilterChip(
                selected = game.emulatorPackage == null,
                onClick = { repository.setEmulatorSettings(game.id, null, game.graphicsProfile) },
                label = { Text("Automatic") },
            )
            options.forEach { pkg ->
                val installed = pkg in installedPackages
                BlueprintFilterChip(
                    selected = game.emulatorPackage == pkg,
                    onClick = { repository.setEmulatorSettings(game.id, pkg, game.graphicsProfile) },
                    enabled = installed,
                    label = {
                        Text(registry.optionDisplayName(pkg) + if (installed) " · Installed" else " · Not installed")
                    },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(readiness.message, color = MaterialTheme.colorScheme.primary)
        if (game.platform.equals("PS2", ignoreCase = true)) {
            Text("ARMSX2 requires a BIOS dump from a PS2 you own. Add it in ARMSX2 before playing.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (readiness.state == EmulatorReadinessState.NONE_INSTALLED) {
            Text("Install the approved package, then return to GameBox to refresh readiness.")
        }
        Text("Adjust graphics and performance in the emulator's own settings.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (readiness.state == EmulatorReadinessState.READY) {
            BlueprintOutlinedButton(onClick = {
                val intent = readiness.selectedPackage?.let { selected ->
                    emulatorPackageCandidates(selected).firstNotNullOfOrNull {
                        packageManager.getLaunchIntentForPackage(it)
                    }
                }
                launchError = if (intent == null) {
                    "Emulator is unavailable. Reopen game settings and try again."
                } else {
                    runCatching {
                        startIntentOnDisplay(context, intent, activity?.currentDisplayId(), activity)
                    }.fold(
                        onSuccess = { null },
                        onFailure = { "Could not open the emulator. Please try again." },
                    )
                }
            }, modifier = Modifier.bringFocusedControlIntoView()) {
                Text("Open emulator")
            }
        }
        launchError?.let { Text(it) }
    }
}

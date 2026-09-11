package com.gamebox.os.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GraphicsProfiles
import com.gamebox.os.launch.EmulatorCapabilityRegistry
import com.gamebox.os.launch.EmulatorReadinessPolicy
import com.gamebox.os.launch.EmulatorReadinessState
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
    val packageManager = LocalContext.current.packageManager
    val settings by settingsRepository.settings.collectAsState(initial = GameBoxSettings())
    val scope = rememberCoroutineScope()
    val platformDefault = settings.platformEmulatorDefaults[
        game.platform.lowercase().filter(Char::isLetterOrDigit)
    ]?.takeIf { it in options }
    val effectivePackage = game.emulatorPackage ?: platformDefault
    val installedPackages = options
        .filter { packageManager.getLaunchIntentForPackage(it) != null }
        .toSet()
    val readiness = EmulatorReadinessPolicy.evaluate(
        approvedOptions = options,
        selectedPackage = effectivePackage,
        installedPackages = installedPackages,
        displayName = registry::displayName,
    )

    Column(modifier.fillMaxWidth().padding(16.dp)) {
        Text("Game settings")
        Text("Changes apply the next time this game launches.")
        Text("Platform default · " + game.platform)
        Text(
            platformDefault?.let { registry.optionDisplayName(it) } ?: "Automatic recommended option",
        )
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(
                selected = platformDefault == null,
                onClick = { scope.launch { settingsRepository.setPlatformEmulatorDefault(game.platform, null) } },
                label = { Text("Automatic") },
            )
            options.forEach { pkg ->
                val installed = pkg in installedPackages
                FilterChip(
                    selected = platformDefault == pkg,
                    onClick = { scope.launch { settingsRepository.setPlatformEmulatorDefault(game.platform, pkg) } },
                    enabled = installed,
                    label = {
                        Text(registry.optionDisplayName(pkg) + if (installed) " · Installed" else " · Not installed")
                    },
                )
            }
        }
        Text("This game")
        Text(if (game.emulatorPackage == null) "Uses platform default" else "Overrides platform default")
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(
                selected = game.emulatorPackage == null,
                onClick = { repository.setEmulatorSettings(game.id, null, game.graphicsProfile) },
                label = { Text("Automatic") },
            )
            options.forEach { pkg ->
                val installed = pkg in installedPackages
                FilterChip(
                    selected = game.emulatorPackage == pkg,
                    onClick = { repository.setEmulatorSettings(game.id, pkg, game.graphicsProfile) },
                    enabled = installed,
                    label = {
                        Text(registry.optionDisplayName(pkg) + if (installed) " · Installed" else " · Not installed")
                    },
                )
            }
        }
        Text(readiness.message)
        if (readiness.state == EmulatorReadinessState.NONE_INSTALLED) {
            Text("Install the approved package, then return to GameBox to refresh readiness.")
        }
        Text("Graphics profile")
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            listOf(
                GraphicsProfiles.COMPATIBILITY,
                GraphicsProfiles.BALANCED,
                GraphicsProfiles.PERFORMANCE,
            ).forEach { profile ->
                FilterChip(
                    selected = game.graphicsProfile == profile,
                    onClick = {
                        repository.setEmulatorSettings(game.id, game.emulatorPackage, profile)
                    },
                    label = { Text(profile) },
                )
            }
        }
    }
}

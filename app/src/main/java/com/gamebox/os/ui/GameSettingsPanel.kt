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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GraphicsProfiles
import com.gamebox.os.launch.EmulatorCapabilityRegistry
import com.gamebox.os.launch.EmulatorReadinessPolicy
import com.gamebox.os.launch.EmulatorReadinessState

@Composable
fun GameSettingsPanel(game: Game, repository: GameRepository, modifier: Modifier = Modifier) {
    val registry = EmulatorCapabilityRegistry()
    val options = androidx.compose.runtime.remember(game.platform) { registry.optionsFor(game) }
    val packageManager = LocalContext.current.packageManager
    val installedPackages = options
        .filter { packageManager.getLaunchIntentForPackage(it) != null }
        .toSet()
    val readiness = EmulatorReadinessPolicy.evaluate(
        approvedOptions = options,
        selectedPackage = game.emulatorPackage,
        installedPackages = installedPackages,
        displayName = registry::displayName,
    )

    Column(modifier.fillMaxWidth().padding(16.dp)) {
        Text("Game settings")
        Text("Changes apply the next time this game launches.")
        Text("Emulator")
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
                        Text(registry.displayName(pkg) + if (installed) " · Installed" else " · Not installed")
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

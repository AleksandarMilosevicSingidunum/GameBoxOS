package com.gamebox.os.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GraphicsProfiles
import com.gamebox.os.launch.EmulatorCapabilityRegistry

@Composable
fun GameSettingsPanel(game: Game, repository: GameRepository, modifier: Modifier = Modifier) {
    val options = EmulatorCapabilityRegistry().optionsFor(game)
    Column(modifier.padding(16.dp)) {
        Text("Emulator")
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(game.emulatorPackage == null, { repository.setEmulatorSettings(game.id, null, game.graphicsProfile) }, label = { Text("Automatic") })
            options.forEach { pkg -> FilterChip(game.emulatorPackage == pkg, { repository.setEmulatorSettings(game.id, pkg, game.graphicsProfile) }, label = { Text(pkg.substringAfterLast('.')) }) }
        }
        Text("Graphics profile")
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            listOf(GraphicsProfiles.COMPATIBILITY, GraphicsProfiles.BALANCED, GraphicsProfiles.PERFORMANCE).forEach { profile -> FilterChip(game.graphicsProfile == profile, { repository.setEmulatorSettings(game.id, game.emulatorPackage, profile) }, label = { Text(profile) }) }
        }
    }
}
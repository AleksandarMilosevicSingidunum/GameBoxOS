@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gamebox.os.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun GameDetailsToolsSection(
    showGameSettings: Boolean,
    metadataLabel: String,
    reducedMotion: Boolean,
    onToggleGameSettings: () -> Unit,
    onEditMetadata: () -> Unit,
    gameSettings: @Composable () -> Unit,
) {
    FlowRow(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
    ) {
        BlueprintOutlinedButton(
            onClick = onToggleGameSettings,
            modifier = Modifier.bringFocusedControlIntoView(),
        ) {
            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 7.dp))
            Text(if (showGameSettings) "Hide game settings" else "Game settings & emulator", fontSize = 12.sp)
        }
        BlueprintOutlinedButton(
            onClick = onEditMetadata,
            modifier = Modifier.bringFocusedControlIntoView().semantics {
                contentDescription = "Edit game metadata and artwork"
            },
        ) {
            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 7.dp))
            Text(metadataLabel, fontSize = 12.sp)
        }
    }

    if (reducedMotion) {
        if (showGameSettings) Column { gameSettings() }
    } else {
        AnimatedVisibility(visible = showGameSettings) {
            Column { gameSettings() }
        }
    }
}

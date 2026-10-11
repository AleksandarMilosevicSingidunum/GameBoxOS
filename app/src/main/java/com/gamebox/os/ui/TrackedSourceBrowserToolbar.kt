package com.gamebox.os.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamebox.os.ui.theme.GameBoxControllerBack
import com.gamebox.os.ui.theme.GameBoxControllerConfirm
import com.gamebox.os.ui.theme.GameBoxFocusBlue

@Composable
internal fun TrackedSourceBrowserToolbar(
    sourceLabel: String,
    backLabel: String,
    onBack: () -> Unit,
) {
    var backFocused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 32.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(Modifier.weight(1f).padding(end = 24.dp)) {
                Column {
                    Text(
                        text = "GAMEBOX  /  ${sourceLabel.uppercase()}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        text = sourceLabel,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                ControllerHint("D-pad", "Navigate", GameBoxFocusBlue)
                ControllerHint("A", "Select", GameBoxControllerConfirm)
                ControllerHint("B", "Back", GameBoxControllerBack)
                BlueprintButton(
                    onClick = onBack,
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .onFocusChanged { backFocused = it.isFocused }
                        .border(
                            width = if (backFocused) 2.dp else 0.dp,
                            color = if (backFocused) GameBoxFocusBlue else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(14.dp),
                        )
                        .testTag("tracked-source-back-to-gamebox"),
                ) {
                    Text(backLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

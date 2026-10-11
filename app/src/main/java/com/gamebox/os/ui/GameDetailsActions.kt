package com.gamebox.os.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun TvDetailsSecondaryAction(
    label: String,
    compact: Boolean,
    enabled: Boolean,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    if (compact) {
        BlueprintOutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .bringFocusedControlIntoView()
                .focusProperties { canFocus = enabled }
                .semantics {
                    this.contentDescription = contentDescription ?: label
                },
        ) {
            Text(label)
        }
        return
    }
    val actionModifier = if (enabled) {
        Modifier.bringFocusedControlIntoView().blueprintClick(onClick)
    } else {
        Modifier.semantics {
            role = Role.Button
            disabled()
        }
    }
    Box(
        modifier = actionModifier
            .heightIn(min = 40.dp)
            .then(contentDescription?.let { description ->
                Modifier.semantics { this.contentDescription = description }
            } ?: Modifier)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.Transparent,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.8f else 0.35f),
            ),
        ) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                Text(
                    label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
internal fun DetailMetric(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

package com.gamebox.os.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ControllerHint(letter: String, label: String, color: Color) {
    Row(
        modifier = Modifier.semantics { contentDescription = "$letter button: $label" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, color),
            modifier = Modifier.size(22.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(letter, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
internal fun BlueprintRail(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

@Composable
internal fun BlueprintRailItem(
    label: String,
    count: Int,
    selected: Boolean,
    icon: ImageVector,
    brandKey: String? = null,
    focusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    semanticLabel: String? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth()
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .then(rightFocusRequester?.let { requester ->
                Modifier.onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT
                    ) {
                        requester.requestFocus()
                    } else {
                        false
                    }
                }
            } ?: Modifier)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val requester = when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_UP -> upFocusRequester
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> downFocusRequester
                    else -> null
                } ?: return@onPreviewKeyEvent false
                requester.requestFocus()
            }
            .hoverable(interactionSource)
            .blueprintClick(onClick)
            .semantics {
                this.selected = selected
                semanticLabel?.let { contentDescription = it }
            },
        color = when {
            focused && selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
            focused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.44f)
            selected || hovered -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(8.dp),
        border = when {
            focused -> BorderStroke(2.dp, blueprintFocusBrush())
            selected || hovered -> BorderStroke(1.dp, blueprintFocusBrush())
            else -> null
        },
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (brandKey == null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
            } else {
                ConsoleBrandMark(brandKey, selected, Modifier.size(16.dp))
            }
            Text(
                label,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun BlueprintPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.68f)),
        tonalElevation = 3.dp,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
    }
}

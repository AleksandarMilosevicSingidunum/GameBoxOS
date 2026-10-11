package com.gamebox.os.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog

/** Search trigger for a controller-driven TV surface; it never opens a software IME. */
@Composable
internal fun ControllerSearchField(
    query: String,
    onOpenKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val activate = rememberUiClickAction(onOpenKeyboard)
    OutlinedButton(
        onClick = activate,
        modifier = modifier
            .onBlueprintActivation(activate)
            .blueprintButtonFocusRing(RoundedCornerShape(8.dp))
            .then(focusRequester?.let(Modifier::focusRequester) ?: Modifier)
            .semantics {
                contentDescription = if (query.isBlank()) {
                    "Search games. Press A to type with the controller."
                } else {
                    "Search games: $query. Press A to edit."
                }
            },
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        androidx.compose.material3.Icon(Icons.Rounded.Search, contentDescription = null)
        Text(
            query.ifBlank { "Search games" },
            Modifier.padding(start = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp,
        )
    }
}

/** Full D-pad/A/B keyboard so TV search does not depend on a physical keyboard or IME. */
@Composable
internal fun ControllerSearchKeyboardDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstKeyFocus = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val compactKeyboard = maxHeight < 600.dp
            val sidePadding = if (maxWidth < 700.dp) 12.dp else 28.dp
            val verticalPadding = if (compactKeyboard) 12.dp else 22.dp
            val rowSpacing = if (compactKeyboard) 5.dp else 12.dp
            val dialogWidthFraction = if (maxWidth < 700.dp) 0.96f else 0.90f
            val dialogMaxHeight = (maxHeight * 0.96f).coerceAtMost(740.dp)

            LaunchedEffect(Unit) {
                androidx.compose.runtime.withFrameNanos { }
                firstKeyFocus.requestFocus()
            }
            Surface(
                modifier = Modifier.widthIn(max = 900.dp)
                    .fillMaxWidth(dialogWidthFraction)
                    .heightIn(max = dialogMaxHeight)
                    .semantics { contentDescription = "Controller search keyboard dialog" }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown &&
                            event.nativeKeyEvent.keyCode in setOf(
                                AndroidKeyEvent.KEYCODE_BUTTON_B,
                                AndroidKeyEvent.KEYCODE_BACK,
                            )
                        ) {
                            onDismiss()
                            true
                        } else {
                            false
                        }
                    },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp,
                shadowElevation = 18.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = sidePadding, vertical = verticalPadding),
                    verticalArrangement = Arrangement.spacedBy(rowSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Search games", style = MaterialTheme.typography.titleLarge)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            query.ifBlank { "Use the D-pad to choose letters" },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                .semantics { contentDescription = "Search query: ${query.ifBlank { "empty" }}" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val keySpacing = if (compactKeyboard) 4.dp else 6.dp
                        val keyWidth = ((maxWidth - keySpacing * 9) / 10f).coerceIn(24.dp, 54.dp)
                        val keyHeight = if (compactKeyboard) 40.dp else 48.dp

                        Column(
                            verticalArrangement = Arrangement.spacedBy(keySpacing),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            controllerSearchKeyboardRows.forEachIndexed { rowIndex, row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(keySpacing)) {
                                    row.forEachIndexed { columnIndex, searchKey ->
                                        val first = rowIndex == 0 && columnIndex == 0
                                        val width = when (searchKey.action) {
                                            ControllerSearchKeyAction.SPACE -> (keyWidth * 3.4f).coerceAtMost(190.dp)
                                            ControllerSearchKeyAction.CLEAR,
                                            ControllerSearchKeyAction.SUBMIT,
                                            ControllerSearchKeyAction.BACKSPACE -> (keyWidth * 1.8f).coerceAtMost(94.dp)
                                            ControllerSearchKeyAction.INSERT -> keyWidth
                                        }
                                        val activateKey = {
                                            when (val result = applyControllerSearchKey(query, searchKey)) {
                                                is ControllerSearchKeyResult.QueryChanged -> onQueryChange(result.query)
                                                ControllerSearchKeyResult.Submitted -> onDismiss()
                                            }
                                        }
                                        val performKeyActivation = rememberUiClickAction(activateKey)
                                        val interactionSource = remember(searchKey) { MutableInteractionSource() }
                                        val focused = interactionSource.collectIsFocusedAsState().value
                                        val reducedMotion = LocalReducedMotion.current
                                        val focusScale = animateFloatAsState(
                                            targetValue = if (reducedMotion) 1f else if (focused) 1.06f else 1f,
                                            animationSpec = if (reducedMotion) snap() else tween(120),
                                            label = "search-key-focus-scale",
                                        )
                                        OutlinedButton(
                                            onClick = performKeyActivation,
                                            modifier = Modifier.width(width).height(keyHeight)
                                                .graphicsLayer {
                                                    scaleX = focusScale.value
                                                    scaleY = focusScale.value
                                                    transformOrigin = TransformOrigin.Center
                                                }
                                                .then(if (first) Modifier.focusRequester(firstKeyFocus) else Modifier)
                                                .bringFocusedControlIntoView()
                                                .onBlueprintActivation(performKeyActivation)
                                                .blueprintButtonFocusRing(RoundedCornerShape(8.dp))
                                                .semantics {
                                                    contentDescription = searchKey.contentDescription
                                                },
                                            interactionSource = interactionSource,
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (focused) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                },
                                                contentColor = if (focused) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                            ),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(2.dp),
                                        ) {
                                            Text(
                                                searchKey.label,
                                                fontWeight = if (focused) {
                                                    androidx.compose.ui.text.font.FontWeight.Bold
                                                } else {
                                                    androidx.compose.ui.text.font.FontWeight.Medium
                                                },
                                                fontSize = if (compactKeyboard) 11.sp else 13.sp,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!compactKeyboard) {
                        Spacer(Modifier.height(1.dp))
                        Text(
                            "D-pad move   A select   B close",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

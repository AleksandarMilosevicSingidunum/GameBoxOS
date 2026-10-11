package com.gamebox.os.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.gamebox.os.launch.MoonlightConnectivity
import com.gamebox.os.launch.MoonlightStatus

internal fun formatShortcutLaunchTime(epochMs: Long): String =
    java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.MEDIUM,
        java.text.DateFormat.SHORT,
    ).format(java.util.Date(epochMs))

@Composable
internal fun BlueprintShortcutTile(
    shortcut: AppShortcut,
    installed: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val performClick = rememberUiClickAction(onClick)
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val emphasized = focused || hovered
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (reducedMotion) 1f else if (pressed) 0.95f else if (emphasized) 1.04f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "hub-tile-scale",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (emphasized) 2.dp else 1.dp,
        animationSpec = if (reducedMotion) snap() else androidx.compose.animation.core.tween(120),
        label = "hub-tile-focus-border",
    )
    val tonalElevation by animateDpAsState(
        targetValue = if (emphasized) 10.dp else 2.dp,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "hub-tile-focus-elevation",
    )
    val accent = shortcutAccent(shortcut.title)
    Surface(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .hoverable(interactionSource).focusDebugTarget()
            .onFocusChanged { focused = it.isFocused }
            .semantics(mergeDescendants = true) {
                contentDescription = shortcut.title + ", " + if (installed) "Ready" else "Setup required"
                role = Role.Button
            }
            .blueprintActivation(interactionSource, performClick, showIndication = false),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(
            borderWidth,
            if (emphasized) blueprintFocusBrush()
            else Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline),
            ),
        ),
        tonalElevation = tonalElevation,
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.72f),
                        accent.copy(alpha = 0.26f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            ),
        ) {
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(0.35f))
                AppBrandMark(shortcut.title, Modifier.size(39.dp))
                Spacer(Modifier.weight(0.35f))
                Text(shortcut.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    if (installed) "Ready" else "Setup required",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
internal fun BlueprintPcHero(
    shortcut: AppShortcut,
    installed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = moonlightHeroContentDescription(installed)
                role = Role.Button
            }
            .blueprintClick(onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF18365B), Color(0xFF07101F), Color(0xFF040711))),
            ),
        ) {
            AppBrandMark("Moonlight", Modifier.align(Alignment.TopEnd).padding(15.dp).size(38.dp))
            Column(Modifier.fillMaxSize().padding(15.dp)) {
                Surface(color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(5.dp)) {
                    Text(
                        if (installed) "READY" else "SETUP REQUIRED",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text("YOUR PC, ON THE BIG SCREEN", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text("Moonlight", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (installed) "Moonlight is ready" else "Install Moonlight to stream",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        moonlightHeroActionLabel(installed),
                        Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MoonlightStatusPanel(status: MoonlightStatus, compact: Boolean) {
    val connectivityLabel = when (status.connectivity) {
        MoonlightConnectivity.OFFLINE -> "Offline"
        MoonlightConnectivity.LOCAL_NETWORK -> "LAN ready"
        MoonlightConnectivity.INTERNET -> "Network connected"
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).semantics {
            contentDescription = "PC streaming: " + connectivityLabel
        },
    ) {
        Column(Modifier.padding(if (compact) 14.dp else 18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PC Streaming", fontWeight = FontWeight.Bold)
                Text(
                    connectivityLabel,
                    color = if (status.connectivity == MoonlightConnectivity.OFFLINE)
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                if (status.moonlightInstalled) {
                    "Moonlight is installed. Launch it to stream from a paired PC."
                } else {
                    "Install Moonlight to enable PC game streaming."
                },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontSize = 12.sp,
            )
            if (status.recentSessions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Recent sessions", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                status.recentSessions.take(3).forEach { session ->
                    Text(session, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
                }
            }
        }
    }
}

@Composable
internal fun ShortcutCard(
    shortcut: AppShortcut,
    installed: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val accent = shortcutAccent(shortcut.title)
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val performClick = rememberUiClickAction(onClick)
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val emphasized = focused || hovered
    val reducedMotion = LocalReducedMotion.current
    val borderWidth by animateDpAsState(
        targetValue = when {
            focused -> 3.dp
            hovered -> 2.dp
            else -> 1.dp
        },
        animationSpec = if (reducedMotion) snap() else androidx.compose.animation.core.tween(120),
        label = "shortcut-focus-border",
    )
    val cardElevation by animateDpAsState(
        targetValue = if (emphasized) 10.dp else 3.dp,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "shortcut-focus-elevation",
    )
    val scale by animateFloatAsState(
        targetValue = if (reducedMotion) 1f else if (pressed) 0.97f else if (emphasized) 1.02f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "shortcut-scale",
    )
    Surface(
        modifier.height(146.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .hoverable(interactionSource)
            .semantics { contentDescription = shortcut.title + ", " + if (installed) "installed" else "not installed" }
            .focusDebugTarget()
            .onFocusChanged { focused = it.isFocused }
            .blueprintActivation(interactionSource, performClick, showIndication = false),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(
            borderWidth,
            if (emphasized) blueprintFocusBrush()
            else Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline),
            ),
        ),
        tonalElevation = cardElevation,
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(accent.copy(alpha = 0.30f), Color.Transparent, Color.Transparent)),
            ),
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(shape = RoundedCornerShape(8.dp), color = accent.copy(alpha = 0.92f)) {
                        Box(Modifier.padding(10.dp).size(25.dp)) {
                            AppBrandMark(shortcut.title, Modifier.fillMaxSize())
                        }
                    }
                    Column {
                        Text(shortcut.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(
                            shortcut.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (installed) "READY" else "SETUP REQUIRED",
                        color = if (installed) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun shortcutAccent(title: String): Color = when (title) {
    "YouTube", "Netflix" -> Color(0xFFE53935)
    "Kodi" -> Color(0xFF168AD7)
    "Jellyfin" -> Color(0xFF6D48D7)
    "Plex" -> Color(0xFFE5A11A)
    "Spotify" -> Color(0xFF1DB954)
    "VLC" -> Color(0xFFF28C28)
    "Twitch" -> Color(0xFF9146FF)
    "Moonlight" -> Color(0xFF6C63FF)
    "Winlator" -> Color(0xFF2475D5)
    "Termux" -> Color(0xFF2E8B57)
    "Files" -> Color(0xFFE6A928)
    "Chrome" -> Color(0xFF4285F4)
    else -> Color(0xFF5C7CFA)
}

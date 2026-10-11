package com.gamebox.os.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.importer.BrowserDownloadStatus

internal enum class Destination(val title: String) {
    HOME("Home"), LIBRARY("Library"), STORE("Store"), DOWNLOADS("Downloads"),
    MEDIA("Media"), PC("PC"), SETTINGS("Settings")
}

/** Keep Download Manager in the main tab order so controller users can always reach it. */
internal val primaryDestinations: List<Destination> = Destination.entries

internal fun downloadActivityCount(
    remoteStatuses: Iterable<DownloadStatus>,
    browserStatuses: Iterable<BrowserDownloadStatus>,
): Int = remoteStatuses.count {
    it in setOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)
} + browserStatuses.count { it != BrowserDownloadStatus.IMPORTED }

internal fun destinationAfterPrimaryTabMove(current: Destination, offset: Int): Destination {
    val currentIndex = primaryDestinations.indexOf(current).coerceAtLeast(0)
    val nextIndex = Math.floorMod(currentIndex + offset, primaryDestinations.size)
    return primaryDestinations[nextIndex]
}

@Composable
internal fun TopNav(
    selected: Destination,
    compact: Boolean,
    downloadActivityCount: Int = 0,
    selectedFocusRequester: FocusRequester? = null,
    onSelect: (Destination) -> Unit,
    onSearch: () -> Unit = { onSelect(Destination.STORE) },
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(30_000)
        }
    }
    if (compact) {
        Column {
            val openDownloads = rememberUiClickAction { onSelect(Destination.DOWNLOADS) }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameBoxLogo(selected)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = openDownloads,
                        modifier = Modifier.size(36.dp).blueprintButtonFocusRing(CircleShape)
                            .onBlueprintActivation(openDownloads).semantics {
                            contentDescription = downloadActivityDescription(downloadActivityCount)
                        },
                    ) {
                        BadgedBox(badge = { if (downloadActivityCount > 0) Badge { Text(downloadActivityCount.coerceAtMost(99).toString()) } }) {
                            Icon(Icons.Rounded.Downloading, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        now.format(DateTimeFormatter.ofPattern("HH:mm")),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                primaryDestinations.forEach {
                    NavButton(it, selected, if (it == selected) selectedFocusRequester else null, onSelect)
                }
            }
        }
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBoxLogo(selected)
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                primaryDestinations.forEach {
                    NavButton(it, selected, if (it == selected) selectedFocusRequester else null, onSelect)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val openSearch = rememberUiClickAction(onSearch)
                IconButton(
                    onClick = openSearch,
                    modifier = Modifier.size(36.dp).blueprintButtonFocusRing(CircleShape)
                        .onBlueprintActivation(openSearch),
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = "Browse and search games", modifier = Modifier.size(18.dp))
                }
                val openDownloads = rememberUiClickAction { onSelect(Destination.DOWNLOADS) }
                IconButton(
                    onClick = openDownloads,
                    modifier = Modifier.size(36.dp).blueprintButtonFocusRing(CircleShape)
                        .onBlueprintActivation(openDownloads).semantics {
                        contentDescription = downloadActivityDescription(downloadActivityCount)
                    },
                ) {
                    BadgedBox(badge = { if (downloadActivityCount > 0) Badge { Text(downloadActivityCount.coerceAtMost(99).toString()) } }) {
                        Icon(Icons.Rounded.Downloading, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(now.format(DateTimeFormatter.ofPattern("h:mm a")), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        now.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                        modifier = Modifier.testTag("wide-nav-date"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

internal fun downloadActivityDescription(count: Int): String = when (count) {
    0 -> "Open download activity"
    1 -> "Open download activity, 1 item active or needing attention"
    else -> "Open download activity, $count items active or needing attention"
}

@Composable
private fun GameBoxLogo(selected: Destination) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GameBoxBrandMark(Modifier.size(30.dp).semantics { contentDescription = "GameBox logo" })
        Spacer(Modifier.width(9.dp))
        Column {
            Text("GameBox", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text(
                "GAMEBOX OS / ${selected.title.uppercase()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
internal fun NavButton(
    item: Destination,
    selected: Destination,
    focusRequester: FocusRequester? = null,
    onSelect: (Destination) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val performClick = rememberUiClickAction { onSelect(item) }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val emphasized = item == selected || hovered || focused
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (reducedMotion) 1f else if (pressed) 0.96f else if (hovered || focused) 1.025f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "nav-scale",
    )
    val border by animateColorAsState(
        if (emphasized) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "nav-border",
    )
    val fill by animateColorAsState(
        if (emphasized) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "nav-fill",
    )
    Surface(
        color = fill,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (emphasized) blueprintFocusBrush() else Brush.linearGradient(listOf(border, border))),
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .hoverable(interactionSource)
            .focusDebugTarget()
            .then(focusRequester?.let(Modifier::focusRequester) ?: Modifier)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .semantics {
                contentDescription = item.title + " tab"
                role = Role.Tab
                this.selected = item == selected
                onClick(label = "Select ${item.title}") {
                    performClick()
                    true
                }
            }
            .blueprintActivation(interactionSource, performClick, showIndication = false),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                item.title,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                maxLines = 1,
                fontSize = 13.sp,
                fontWeight = if (item == selected) FontWeight.Bold else FontWeight.Medium,
                color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item == selected) {
                Box(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp)
                        .width(26.dp).height(3.dp).clip(RoundedCornerShape(50)).background(blueprintFocusBrush()),
                )
            }
        }
    }
}

internal fun destinationIcon(item: Destination): ImageVector = when (item) {
    Destination.HOME -> Icons.Rounded.Home
    Destination.LIBRARY -> Icons.Rounded.VideoLibrary
    Destination.STORE -> Icons.Rounded.Storefront
    Destination.DOWNLOADS -> Icons.Rounded.Download
    Destination.MEDIA -> Icons.Rounded.Movie
    Destination.PC -> Icons.Rounded.DesktopWindows
    Destination.SETTINGS -> Icons.Rounded.Settings
}

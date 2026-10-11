package com.gamebox.os.ui

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SettingsSectionStrip(selected: SettingsSection, onSelect: (SettingsSection) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 10.dp),
    ) {
        items(SettingsSection.entries, key = { it.name }) { section ->
            BlueprintFilterChip(
                selected = section == selected,
                onClick = { onSelect(section) },
                label = { Text(section.label) },
                leadingIcon = { Icon(settingsSectionIcon(section), null, Modifier.size(16.dp)) },
                modifier = Modifier.semantics {
                    contentDescription = "${section.label} settings section"
                },
            )
        }
    }
}

@Composable
internal fun SettingsWideSectionRail(
    selected: SettingsSection,
    storageFocusRequester: FocusRequester,
    onSelect: (SettingsSection) -> Unit,
    initialFocusSection: SettingsSection = SettingsSection.STORAGE,
) {
    BlueprintRail(Modifier.width(246.dp)) {
        Text(
            "SETTINGS",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("wide-settings-heading"),
        )
        SettingsSection.entries.forEach { section ->
            val isSelected = section == selected
            val interactionSource = remember(section) { MutableInteractionSource() }
            val hovered by interactionSource.collectIsHoveredAsState()
            var focused by remember(section) { mutableStateOf(false) }
            val emphasized = isSelected || hovered || focused
            val reducedMotion = LocalReducedMotion.current
            val containerColor by animateColorAsState(
                targetValue = when {
                    focused && isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
                    focused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
                    isSelected || hovered -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                    else -> Color.Transparent
                },
                animationSpec = if (reducedMotion) snap() else spring(),
                label = "settings-section-container",
            )
            val borderWidth by animateDpAsState(
                targetValue = when {
                    focused -> 2.dp
                    isSelected || hovered -> 1.dp
                    else -> 0.dp
                },
                animationSpec = if (reducedMotion) snap() else spring(),
                label = "settings-section-border-width",
            )
            Surface(
                modifier = Modifier.fillMaxWidth()
                    .then(if (section == initialFocusSection) Modifier.focusRequester(storageFocusRequester) else Modifier)
                    .hoverable(interactionSource)
                    .blueprintClick { onSelect(section) }
                    .onFocusChanged { focused = it.isFocused }
                    .semantics {
                        this.selected = isSelected
                        contentDescription = "${section.label} settings section"
                    },
                color = containerColor,
                shape = RoundedCornerShape(8.dp),
                border = when {
                    borderWidth <= 0.1.dp -> null
                    focused -> BorderStroke(borderWidth, blueprintFocusBrush())
                    else -> BorderStroke(borderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                },
            ) {
                Row(
                    Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        settingsSectionIcon(section),
                        contentDescription = null,
                        tint = if (emphasized) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        section.label,
                        modifier = Modifier.weight(1f).padding(start = 9.dp).testTag("wide-settings-label-${section.name}"),
                        fontSize = 13.sp,
                    )
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

internal fun settingsSectionIcon(section: SettingsSection): ImageVector = when (section) {
    SettingsSection.STORAGE -> Icons.Rounded.Storage
    SettingsSection.CONTROLLERS -> Icons.Rounded.SportsEsports
    SettingsSection.DOWNLOADS -> Icons.Rounded.Downloading
    SettingsSection.EMULATORS -> Icons.Rounded.Memory
    SettingsSection.DISPLAY -> Icons.Rounded.Monitor
    SettingsSection.AUDIO -> Icons.AutoMirrored.Rounded.VolumeUp
    SettingsSection.NETWORK -> Icons.Rounded.Wifi
    SettingsSection.SAVES_CLOUD -> Icons.Rounded.CloudSync
    SettingsSection.SYSTEM -> Icons.Rounded.Settings
    SettingsSection.SOURCES -> Icons.Rounded.Search
    SettingsSection.DEVELOPER -> Icons.Rounded.BugReport
}

@Composable
internal fun SettingsStatusCard(icon: ImageVector, title: String, detail: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
internal fun SettingsActionRow(
    title: String,
    icon: ImageVector,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val emphasized = hovered || focused
    val reducedMotion = LocalReducedMotion.current
    val border by animateColorAsState(
        if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "settings-row-border",
    )
    val container by animateColorAsState(
        targetValue = if (emphasized) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "settings-row-container",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (focused) 2.dp else 1.dp,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "settings-row-border-width",
    )
    val focusModifier = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
    Surface(
        modifier = modifier.then(focusModifier)
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .blueprintClick(onClick)
            .hoverable(interactionSource)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = title },
        color = container,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(borderWidth, border),
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp)
            .semantics { contentDescription = "Settings section: " + title },
    )
}

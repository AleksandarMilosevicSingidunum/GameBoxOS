package com.gamebox.os.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId

/** Presentation boundary; platform state and launcher intents stay in [HomeScreen]. */
@Composable
internal fun HomeDashboardContent(
    games: List<Game>,
    presentation: HomePresentationState?,
    compact: Boolean,
    hostWindowHasFocus: Boolean = true,
    showDefaultHomeSetupBanner: Boolean,
    networkLabel: String,
    storagePercent: Int?,
    deviceModel: String,
    controllerLabel: String,
    onChooseHome: () -> Unit,
    emptyHomeSetupFocusRequester: FocusRequester,
    openPc: () -> Unit,
    openStore: () -> Unit,
    onFocused: (GameId) -> Unit,
    open: (Game) -> Unit,
    openStorageSettings: () -> Unit = {},
    openControllerSettings: () -> Unit = {},
) {
    if (presentation == null) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(restoredScrollState("home-empty")),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showDefaultHomeSetupBanner) {
                DefaultHomeSetupBanner(onChooseHome, emptyHomeSetupFocusRequester)
            }
            BlueprintPanel(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        Modifier.weight(if (compact) 1f else 1.35f),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Text(
                            "Welcome to GameBox",
                            fontSize = if (compact) 22.sp else 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Discover a title, import a copy you own, and keep your living-room library together.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                        BlueprintButton(
                            onClick = openStore,
                            modifier = Modifier.focusRequester(emptyHomeSetupFocusRequester)
                                .bringFocusedControlIntoView(),
                        ) {
                            Icon(Icons.Rounded.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                "Open Vimm Lair in GameBox",
                                Modifier.padding(start = 8.dp).semantics {
                                    contentDescription = "Open Vimm Lair in GameBox"
                                },
                            )
                        }
                    }
                    if (!compact) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            HomeStatusItem(Icons.Rounded.SportsEsports, "Controller", controllerLabel)
                            HomeStatusItem(Icons.Rounded.Wifi, "Network", networkLabel)
                            HomeStatusItem(
                                Icons.Rounded.Storage,
                                "Storage",
                                storagePercent?.let { "$it% used" } ?: "Checking…",
                                storagePercent?.div(100f),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Getting started", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                val steps = listOf(
                    "01" to ("Choose storage" to "Pick where GameBox keeps your imported library, organized by console."),
                    "02" to ("Pair a controller" to "Connect a Bluetooth gamepad for comfortable couch navigation and play."),
                    "03" to ("Discover" to "Browse console rows and Vimm Lair. A listing is discovery information, not an install file; supported source links can start a tracked download."),
                    "04" to ("Download" to "Open Vimm Lair in GameBox to find a title and start a tracked download. GameBox verifies and imports supported downloads automatically; use manual import for files you already have."),
                    "05" to ("Get ready to play" to "Install a compatible emulator and any required firmware before launching a game."),
                )
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        steps.forEach { (number, step) ->
                            HomeQuickStartStep(
                                number,
                                step.first,
                                step.second,
                                icon = homeQuickStartIcon(number),
                                compact = true,
                                modifier = Modifier.fillMaxWidth(),
                                actionLabel = when (number) {
                                    "01" -> "Choose folder"
                                    "02" -> "Controller settings"
                                    else -> null
                                },
                                onAction = when (number) {
                                    "01" -> openStorageSettings
                                    "02" -> openControllerSettings
                                    else -> null
                                },
                            )
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        steps.forEach { (number, step) ->
                            HomeQuickStartStep(
                                number,
                                step.first,
                                step.second,
                                icon = homeQuickStartIcon(number),
                                compact = false,
                                modifier = Modifier.weight(1f),
                                actionLabel = when (number) {
                                    "01" -> "Choose folder"
                                    "02" -> "Controller settings"
                                    else -> null
                                },
                                onAction = when (number) {
                                    "01" -> openStorageSettings
                                    "02" -> openControllerSettings
                                    else -> null
                                },
                            )
                        }
                    }
                }
            }
        }
        return
    }

    val installed = presentation.installedGames
    val history = presentation.recentlyPlayed
    val hero = presentation.hero
    val focusTarget = presentation.focusTarget
    val secondarySection = homeSecondarySection(games, history, hero)
    val collectionFocusRequester = remember { FocusRequester() }
    val desktopFocusRequester = remember { FocusRequester() }

    Column(
        Modifier.fillMaxWidth().verticalScroll(restoredScrollState("home")),
        // Keep the recent-game rail clear of the persistent controller footer.
        verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 4.dp),
    ) {
        if (showDefaultHomeSetupBanner) DefaultHomeSetupBanner(onChooseHome)
        Text(
            if (history.isEmpty()) "Ready to play" else "Continue Playing",
            modifier = Modifier.semantics { heading() },
            fontSize = if (compact) 22.sp else 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GameCard(
                hero,
                Modifier.weight(1f).height(if (compact) 224.dp else 184.dp),
                hero = true,
                restoreFocus = focusTarget == hero.id,
                hostWindowHasFocus = hostWindowHasFocus,
                focusDownRequester = if (compact) null else if (installed.isEmpty()) {
                    desktopFocusRequester
                } else collectionFocusRequester,
                onFocused = onFocused,
            ) { open(hero) }
            if (!compact) {
                            HomeStatusPanel(
                                networkLabel = networkLabel,
                                storagePercent = storagePercent,
                                deviceModel = deviceModel,
                                controllerLabel = controllerLabel,
                                openSettings = openControllerSettings,
                            )
            }
        }
        if (compact) {
            HomeGameSection(
                "In your library", installed, focusTarget.takeIf { it != hero.id }, onFocused, true, open,
            )
            HomeQuickLaunchRow(openPc)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(Modifier.weight(1.65f)) {
                    Text("Your collection", Modifier.semantics { heading() }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (installed.isEmpty()) {
                        Text(
                            "Install a title from Store to start your collection.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(3.dp),
                        ) {
                            items(installed.take(8), key = { it.id.value }) { game ->
                                GameCard(
                                    game,
                                    Modifier.width(116.dp).height(128.dp),
                                    poster = true,
                                    restoreFocus = focusTarget == game.id && game.id != hero.id,
                                    hostWindowHasFocus = hostWindowHasFocus,
                                    focusRequesterOverride = collectionFocusRequester.takeIf {
                                        game.id == installed.firstOrNull()?.id
                                    },
                                    onFocused = onFocused,
                                ) { open(game) }
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    HomeQuickLaunchRow(
                        openPc,
                        compact = false,
                        desktopFocusRequester = desktopFocusRequester,
                    )
                }
            }
        }
        secondarySection?.let { section ->
            HomeGameSection(
                section.title,
                section.games,
                focusTarget.takeIf { id -> id != hero.id && section.games.none { it.id == id } },
                onFocused,
                compact,
                open,
                openStore = if (compact) null else openStore,
            )
        }
    }
}

@Composable
private fun HomeQuickStartStep(
    number: String,
    title: String,
    description: String,
    icon: ImageVector,
    compact: Boolean,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.heightIn(min = if (compact) 0.dp else 184.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .68f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                ) {
                    Text(
                        number,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            if (actionLabel != null && onAction != null) {
                BlueprintOutlinedButton(onClick = onAction) {
                    Text(actionLabel, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun homeQuickStartIcon(number: String): ImageVector = when (number) {
    "01" -> Icons.Rounded.Storage
    "02" -> Icons.Rounded.SportsEsports
    "03" -> Icons.Rounded.Search
    "04" -> Icons.Rounded.Download
    else -> Icons.Rounded.PlayArrow
}

@Composable
internal fun HomeGameSection(
    title: String,
    games: List<Game>,
    focusTarget: GameId?,
    onFocused: (GameId) -> Unit,
    compact: Boolean,
    open: (Game) -> Unit,
    openStore: (() -> Unit)? = null,
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.semantics { heading() }, fontSize = if (compact) 18.sp else 15.sp, fontWeight = FontWeight.SemiBold)
            if (openStore != null) {
                BlueprintTextButton(
                    onClick = openStore,
                    modifier = Modifier.semantics {
                        contentDescription = "Open Vimm Lair in GameBox"
                    },
                ) {
                    Icon(Icons.Rounded.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Open Vimm Lair in GameBox", Modifier.padding(start = 6.dp), fontSize = 13.sp)
                }
            } else {
                Text(quantityLabel(games.size, "game"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(if (compact) 8.dp else 4.dp))
        if (games.isEmpty()) {
            Text("Your games will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        } else {
            GameRow(games, focusTarget, onFocused, compact, open)
        }
    }
}

@Composable
internal fun HomeQuickLaunchRow(
    openPc: () -> Unit,
    compact: Boolean = true,
    desktopFocusRequester: FocusRequester? = null,
) {
    Text("Quick launch", Modifier.semantics { heading() }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(11.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "Desktop" to Color(0xFF6159D7),
            "Steam" to Color(0xFF18558E),
            "Xbox" to Color(0xFF16722E),
            "Epic Games" to Color(0xFF252B37),
        ).forEachIndexed { index, (label, accent) ->
            Surface(
                color = accent,
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .12f)),
                modifier = Modifier.weight(1f).heightIn(min = if (compact) 140.dp else 100.dp)
                    .then(
                        if (index == 0 && desktopFocusRequester != null) {
                            Modifier.focusRequester(desktopFocusRequester)
                        } else Modifier
                    )
                    .blueprintClick(openPc)
                    .semantics { contentDescription = "Quick launch $label; opens PC Hub" },
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .40f))))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AppBrandMark(label, Modifier.size(32.dp))
                    Spacer(Modifier.height(if (compact) 18.dp else 8.dp))
                    Text(
                        label,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = if (compact) 2 else 1,
                        minLines = if (compact) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        fontSize = if (compact) 12.sp else 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "PC Hub",
                        color = Color.White.copy(alpha = .78f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeStatusPanel(
    networkLabel: String,
    storagePercent: Int?,
    deviceModel: String,
    controllerLabel: String,
    openSettings: () -> Unit,
) {
    Column(Modifier.width(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HomeStatusItem(
            Icons.Rounded.Storage,
            "Storage",
            storagePercent?.let { "$it% used" } ?: "Checking…",
            storagePercent?.div(100f),
        )
        HomeStatusItem(Icons.Rounded.Wifi, "Network", networkLabel)
        HomeStatusItem(
            Icons.Rounded.SportsEsports,
            "Controller",
            controllerLabel,
            onClick = openSettings,
        )
        HomeStatusItem(Icons.Rounded.Smartphone, "Device", deviceModel)
    }
}

@Composable
internal fun HomeStatusItem(
    icon: ImageVector,
    label: String,
    value: String,
    progress: Float? = null,
    onClick: (() -> Unit)? = null,
) {
    val reducedMotion = LocalReducedMotion.current
    val animatedProgress = animateFloatAsState(
        targetValue = progress?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = if (reducedMotion) snap() else spring(stiffness = Spring.StiffnessLow),
        label = "home-status-progress",
    ).value
    val statusTone = homeStatusTone(label, value)
    val statusIndicatorColor = when (statusTone) {
        HomeStatusTone.HEALTHY -> Color(0xFF42D6A0)
        HomeStatusTone.ATTENTION -> Color(0xFFFFB454)
        null -> MaterialTheme.colorScheme.outlineVariant
    }
    val animatedStatusIndicatorColor by animateColorAsState(
        targetValue = statusIndicatorColor,
        animationSpec = if (reducedMotion) snap() else tween(180),
        label = "home-status-indicator",
    )
    val interactionModifier = onClick?.let { action ->
        Modifier.blueprintClick(action).semantics(mergeDescendants = true) {
            role = Role.Button
            contentDescription = "$label: $value. Open Settings to configure."
        }
    } ?: Modifier
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = .80f),
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().then(interactionModifier).testTag("home-status-item-$label"),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, fontSize = 11.sp, lineHeight = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            value,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (label == "Network" || label == "Controller") {
                            Box(
                                Modifier.size(7.dp)
                                    .clip(CircleShape)
                                    .background(animatedStatusIndicatorColor)
                                    .testTag("home-status-indicator-$label"),
                            )
                        }
                    }
                }
            }
            if (progress != null) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).semantics {
                        contentDescription = "$label usage: $value"
                    },
                )
            }
        }
    }
}

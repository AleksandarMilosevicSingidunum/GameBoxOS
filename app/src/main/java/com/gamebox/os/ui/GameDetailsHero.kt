package com.gamebox.os.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.InstallState

@Composable
internal fun GameArtworkHeroFrame(
    artworkUrl: String?,
    fallbackKey: String,
    modifier: Modifier,
    borderColor: Color,
    tonalElevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = tonalElevation,
    ) {
        Box(Modifier.fillMaxSize()) {
            RemoteArtwork(
                artworkUrl,
                Modifier.fillMaxSize().blur(14.dp),
                fallbackKey = fallbackKey,
                showFallbackMonogram = false,
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(Color(0xF4050812), Color(0xA1050812), Color(0x22050812)),
                    ),
                ),
            )
            content()
        }
    }
}

@Composable
internal fun GameDetailsHero(
    game: Game,
    modifier: Modifier = Modifier,
    detail: GameDetailPresentation,
    compact: Boolean,
    authorizedFixture: Boolean,
    workerActive: Boolean,
    hasRemoteSource: Boolean,
    hasApprovedEmulator: Boolean = true,
    primaryActionFocusRequester: FocusRequester,
    discoveryActionFocusRequester: FocusRequester? = null,
    onPlay: () -> Unit,
    onInstallAuthorized: () -> Unit,
    onInstallRemote: () -> Unit,
) {
    // Expose the primary action as a direct TV focus target. Material Button is
    // focusable in normal traversal, but explicit focus is needed by host restore.
    val primaryActionModifier = Modifier
        .fillMaxWidth()
        .focusRequester(primaryActionFocusRequester)
        .focusProperties { discoveryActionFocusRequester?.let { down = it } }
        .bringFocusedControlIntoView()
    GameArtworkHeroFrame(
        artworkUrl = detail.heroUrl,
        fallbackKey = detail.title,
        modifier = modifier.fillMaxWidth().height(if (compact) 252.dp else 292.dp),
        borderColor = MaterialTheme.colorScheme.outline,
        tonalElevation = 8.dp,
    ) {
        Row(
            Modifier.fillMaxSize().padding(if (compact) 18.dp else 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                if (compact) Modifier.fillMaxWidth(0.92f) else Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Row(
                        Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Rounded.SportsEsports, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(
                            detail.platform.uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    detail.title,
                    modifier = Modifier.semantics { heading() },
                    fontSize = if (compact) 29.sp else 39.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                )
                Text(detail.genre ?: "Imported", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    detail.releaseLabel?.let { releaseYear ->
                        DetailMetric(Icons.Rounded.CalendarMonth, releaseYear)
                    }
                    DetailMetric(Icons.Rounded.Groups, detail.playersLabel ?: "1 player")
                    DetailMetric(Icons.Rounded.Storage, game.sizeMb.toString() + " MB")
                }
            }
            if (!compact) {
                Spacer(Modifier.width(18.dp))
                Box(Modifier.width(238.dp).height(220.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xD9151C2C),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
                    ) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "INSTALLATION",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Status", fontSize = 13.sp)
                                Text(
                                    game.state.displayName(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Game size", fontSize = 13.sp)
                                Text(game.sizeMb.toString() + " MB", fontSize = 13.sp)
                            }
                            when {
                                game.state in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE) -> {
                                    BlueprintButton(
                                        onClick = onPlay,
                                        enabled = hasApprovedEmulator,
                                        modifier = primaryActionModifier,
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(
                                            if (hasApprovedEmulator) "Play Now" else "No approved emulator",
                                            modifier = Modifier.padding(start = 6.dp),
                                            fontSize = 14.sp,
                                        )
                                    }
                                    if (!hasApprovedEmulator) {
                                        Text(
                                            "GameBox cannot launch ${detail.platform} titles yet.",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 10.sp,
                                        )
                                    }
                                }
                                authorizedFixture -> {
                                    BlueprintButton(
                                        onClick = onInstallAuthorized,
                                        enabled = !workerActive,
                                        modifier = primaryActionModifier,
                                    ) {
                                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(
                                            if (workerActive) "Installing…" else "Install game",
                                            modifier = Modifier.padding(start = 6.dp),
                                            fontSize = 14.sp,
                                        )
                                    }
                                }
                                game.state in setOf(
                                    InstallState.NOT_INSTALLED,
                                    InstallState.FAILED,
                                    InstallState.MISSING_FILES,
                                ) && hasRemoteSource -> {
                                    BlueprintButton(onClick = onInstallRemote, modifier = primaryActionModifier) {
                                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Install game", modifier = Modifier.padding(start = 6.dp), fontSize = 14.sp)
                                    }
                                }
                                else -> Text(
                                    if (!authorizedFixture && !hasRemoteSource && game.state in setOf(
                                            InstallState.NOT_INSTALLED,
                                            InstallState.FAILED,
                                            InstallState.MISSING_FILES,
                                        )
                                    ) "No verified installer is configured. Use Import your copy below."
                                    else "Install this authorized title below to make it ready to play.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

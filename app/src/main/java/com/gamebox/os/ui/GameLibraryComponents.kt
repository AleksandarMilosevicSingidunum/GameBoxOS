package com.gamebox.os.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.ui.theme.GameBoxCardOutlineEnd
import com.gamebox.os.ui.theme.GameBoxCardOutlineStart

@Composable
internal fun BlueprintGameTile(
    game: Game,
    restoreFocus: Boolean,
    onFocused: (GameId) -> Unit,
    open: (Game) -> Unit,
) {
    GameCard(
        game = game,
        modifier = Modifier.width(124.dp).height(168.dp),
        poster = true,
        restoreFocus = restoreFocus,
        onFocused = onFocused,
        onClick = { open(game) },
    )
}

@Composable
internal fun GameRow(
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
    compact: Boolean,
    open: (Game) -> Unit,
) {
    val listState = rememberLazyListState()
    val restoreIndex = games.indexOfFirst { it.id == restoreGameId }
    LaunchedEffect(restoreIndex) {
        if (restoreIndex >= 0) listState.scrollToItem(restoreIndex)
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(games, key = { it.id.value }) { game ->
            GameCard(
                game = game,
                modifier = Modifier.width(if (compact) 184.dp else 198.dp)
                    .height(if (compact) 126.dp else 105.dp),
                restoreFocus = restoreGameId == game.id,
                onFocused = onFocused,
                onClick = { open(game) },
            )
        }
    }
}

@Composable
internal fun GameCard(
    game: Game,
    modifier: Modifier,
    hero: Boolean = false,
    restoreFocus: Boolean = false,
    hostWindowHasFocus: Boolean = true,
    focusRequesterOverride: FocusRequester? = null,
    focusDownRequester: FocusRequester? = null,
    onFocused: (GameId) -> Unit = {},
    poster: Boolean = false,
    onClick: () -> Unit,
) {
    val internalFocusRequester = remember { FocusRequester() }
    val focusRequester = focusRequesterOverride ?: internalFocusRequester
    val performClick = rememberUiClickAction(onClick)
    var focused by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val emphasized = focused || hovered
    val reducedMotion = LocalReducedMotion.current
    val focusBloom by animateFloatAsState(
        targetValue = if (hero && emphasized && !reducedMotion) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(420),
        label = "hero-focus-bloom",
    )
    val scale by animateFloatAsState(
        targetValue = if (reducedMotion) 1f else if (pressed) .975f else if (emphasized) 1.025f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "game-scale",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (emphasized) 2.dp else 1.dp,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "game-border-width",
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (emphasized) 12.dp else 2.dp,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "game-shadow",
    )
    LaunchedEffect(restoreFocus, hostWindowHasFocus) {
        if (restoreFocus && hostWindowHasFocus) focusRequester.requestFocus()
    }
    Surface(
        modifier.focusProperties {
            focusDownRequester?.let { down = it }
        }.focusRequester(focusRequester)
            .semantics {
                contentDescription = GameBoxSemantics.gameCardDescription(game, hero)
                role = Role.Button
            }
            .focusDebugTarget()
            .bringFocusedControlIntoView()
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused(game.id)
            }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .hoverable(source)
            .focusable()
            .blueprintActivation(source, performClick, showIndication = false),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            borderWidth,
            if (emphasized) blueprintFocusBrush()
            else Brush.linearGradient(listOf(GameBoxCardOutlineStart, GameBoxCardOutlineEnd)),
        ),
        shadowElevation = shadowElevation,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (poster && !hero) {
                PosterGameCardContent(game, maxWidth)
            } else {
                StandardGameCardContent(
                    game = game,
                    hero = hero,
                    focusBloom = focusBloom,
                    cardWidth = maxWidth,
                    cardHeight = maxHeight,
                )
            }
        }
    }
}

@Composable
private fun PosterGameCardContent(game: Game, cardWidth: Dp) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().testTag("game-card-poster-artwork")) {
            RemoteArtwork(
                game.artworkUrl,
                Modifier.fillMaxSize(),
                fallbackKey = gameArtworkFallbackTitle(game),
                contentScale = gameCardArtworkScale(hero = false, poster = true),
            )
            if (game.favorite) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(14.dp),
                )
            }
        }
        Column(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .testTag("game-card-poster-info")
                .padding(horizontal = 9.dp, vertical = 7.dp),
        ) {
            Text(
                game.title,
                fontSize = if (cardWidth >= 150.dp) 16.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = gameCardTitleMaxLines(cardWidth, poster = true),
                lineHeight = if (cardWidth >= 150.dp) 21.sp else 17.sp,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                gameCardPosterSubtitle(game),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (cardWidth >= 150.dp) 12.sp else 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StandardGameCardContent(
    game: Game,
    hero: Boolean,
    focusBloom: Float,
    cardWidth: Dp,
    cardHeight: Dp,
) {
    Box(Modifier.fillMaxSize()) {
        RemoteArtwork(
            game.artworkUrl,
            Modifier.fillMaxSize(),
            fallbackKey = gameArtworkFallbackTitle(game),
            contentScale = gameCardArtworkScale(hero, poster = false),
            showFallbackMonogram = false,
        )
        Box(
            Modifier.fillMaxSize().background(
                if (hero) {
                    Brush.horizontalGradient(
                        listOf(Color(0xF0060B12), Color(0xB0060B12), Color.Transparent),
                    )
                } else {
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        .30f to Color.Transparent,
                        .48f to Color(0xD4040911),
                        1f to Color(0xFF040911),
                    )
                },
            ),
        )
        if (focusBloom > 0.001f) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF428BFF).copy(alpha = focusBloom * 0.11f),
                            Color.Transparent,
                            Color(0xFFAB5CFF).copy(alpha = focusBloom * 0.09f),
                        ),
                    ),
                ),
            )
        }
        val coverRailWidth = if (hero) 0.dp else gameCardCoverRailWidth(cardHeight)
        if (coverRailWidth > 0.dp) {
            Surface(
                modifier = Modifier.align(Alignment.CenterStart)
                    .width(coverRailWidth).fillMaxHeight().padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black,
                border = BorderStroke(1.dp, Color.White.copy(alpha = .16f)),
                shadowElevation = 5.dp,
            ) {
                RemoteArtwork(
                    game.artworkUrl,
                    Modifier.fillMaxSize(),
                    fallbackKey = gameArtworkFallbackTitle(game),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        if (hero) {
            HeroGameCardContent(game, cardWidth)
        } else {
            RegularGameCardContent(game, cardWidth, coverRailWidth)
        }
    }
}

@Composable
private fun HeroGameCardContent(game: Game, cardWidth: Dp) {
    // TV OEM fonts can measure taller than the nominal sp size. Keep enough
    // bottom-aligned slack that the play action remains fully legible in the
    // fixed-height Home hero on external displays.
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            game.platform.uppercase(),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            game.title,
            fontSize = if (cardWidth < 450.dp) 28.sp else 38.sp,
            lineHeight = if (cardWidth < 450.dp) 32.sp else 44.sp,
            fontWeight = FontWeight.Bold,
            maxLines = if (cardWidth < 450.dp) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(.84f),
        )
        Text(
            game.description ?: game.genre,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 380.dp).padding(top = 6.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(6.dp)) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(15.dp))
                    Text(
                        "View game",
                        Modifier.padding(start = 5.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                game.state.displayName(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.RegularGameCardContent(
    game: Game,
    cardWidth: Dp,
    coverRailWidth: Dp,
) {
    if (game.favorite) {
        Icon(
            Icons.Rounded.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(14.dp),
        )
    }
    Column(
        Modifier.align(Alignment.BottomStart).fillMaxWidth()
            .padding(start = coverRailWidth + 11.dp, end = 11.dp, top = 8.dp, bottom = 10.dp),
    ) {
        Text(
            game.title,
            fontSize = when {
                cardWidth < 170.dp -> 12.sp
                cardWidth < 250.dp -> 13.sp
                else -> 16.sp
            },
            fontWeight = FontWeight.SemiBold,
            maxLines = gameCardTitleMaxLines(cardWidth, poster = false),
            lineHeight = if (cardWidth >= 250.dp) 20.sp else 16.sp,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            gameCardSubtitle(game),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (cardWidth >= 250.dp) 12.sp else 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun gameCardArtworkScale(hero: Boolean, poster: Boolean): ContentScale = when {
    hero -> ContentScale.Crop
    poster -> ContentScale.Fit
    else -> ContentScale.Crop
}

/** Wide TV cards get one extra title line; small square cards retain more artwork area. */
internal fun gameCardTitleMaxLines(cardWidth: Dp, poster: Boolean): Int = when {
    poster && cardWidth >= 110.dp && cardWidth < 150.dp -> 3
    !poster && cardWidth >= 170.dp && cardWidth < 250.dp -> 3
    else -> 2
}

internal fun gameCardConsoleLabel(platformName: String): String =
    storeConsoles.firstOrNull { storeConsoleMatches(it, platformName) }?.cardLabel ?: platformName

internal fun gameCardPosterSubtitle(game: Game): String = buildString {
    append(gameCardConsoleLabel(game.platform))
    game.minutesPlayed.coerceAtLeast(0).takeIf { it > 0 }?.let { minutes ->
        append(" · ")
        append(formatCompactPlaytime(minutes))
    }
}

internal fun gameCardSubtitle(game: Game): String = buildList {
    add(gameCardConsoleLabel(game.platform))
    add(game.state.displayName())
    game.minutesPlayed.coerceAtLeast(0).takeIf { it > 0 }?.let { add(formatCompactPlaytime(it)) }
}.joinToString(" · ")

internal fun formatCompactPlaytime(minutesPlayed: Int): String {
    val minutes = minutesPlayed.coerceAtLeast(0)
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours == 0 -> "${remainingMinutes}m"
        remainingMinutes == 0 -> "${hours}h"
        else -> "${hours}h ${remainingMinutes}m"
    }
}

internal fun libraryPosterTileWidth(availableWidth: Dp): Dp =
    ((availableWidth - 32.dp) / 4f).coerceIn(108.dp, 190.dp)

internal fun libraryPosterTileHeight(tileWidth: Dp): Dp = tileWidth

internal fun gameCardCoverRailWidth(cardHeight: Dp): Dp =
    (cardHeight * .62f).coerceIn(54.dp, 72.dp)

@Composable
internal fun GameScreenshotGallery(detail: GameDetailPresentation, compact: Boolean) {
    var selectedScreenshot by remember(detail.id, detail.screenshots) { mutableStateOf<String?>(null) }
    selectedScreenshot?.let { screenshot ->
        AlertDialog(
            onDismissRequest = { selectedScreenshot = null },
            title = { Text(detail.title, maxLines = 2) },
            text = {
                RemoteArtwork(
                    screenshot,
                    Modifier.fillMaxWidth().height(300.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                )
            },
            confirmButton = { BlueprintTextButton(onClick = { selectedScreenshot = null }) { Text("Close") } },
        )
    }
    if (detail.screenshots.isNotEmpty()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Screenshots", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Select to enlarge · ${detail.screenshots.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(3.dp)) {
                items(detail.screenshots.distinct(), key = { it }) { screenshot ->
                    Surface(
                        Modifier.width(if (compact) 236.dp else 260.dp)
                            .height(146.dp)
                            .blueprintClick { selectedScreenshot = screenshot }
                            .semantics { contentDescription = "Enlarge screenshot of ${detail.title}" },
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        RemoteArtwork(screenshot, Modifier.fillMaxSize(), fallbackKey = detail.title)
                    }
                }
            }
        }
    }
}

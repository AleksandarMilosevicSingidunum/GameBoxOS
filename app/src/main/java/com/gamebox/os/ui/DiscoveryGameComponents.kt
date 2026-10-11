package com.gamebox.os.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.catalog.LibretroThumbnailClient
import com.gamebox.os.source.DiscoverySourceGame
import com.gamebox.os.ui.theme.GameBoxCardOutlineEnd
import com.gamebox.os.ui.theme.GameBoxCardOutlineStart

@Composable
internal fun ConfiguredSourceResultCard(
    game: DiscoverySourceGame,
    modifier: Modifier = Modifier,
    presentation: DiscoveryGame = configuredSourceResultToDiscoveryGame(game),
    thumbnailClient: LibretroThumbnailClient? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val performClick = rememberUiClickAction(onClick)
    val hovered by source.collectIsHoveredAsState()
    val reducedMotion = LocalReducedMotion.current
    val emphasized = focused || hovered
    val scale by animateFloatAsState(
        targetValue = when {
            reducedMotion -> 1f
            focused -> 1.025f
            hovered -> 1.012f
            else -> 1f
        },
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "configured-result-focus-scale",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (emphasized) 2.dp else 1.dp,
        animationSpec = if (reducedMotion) snap() else androidx.compose.animation.core.tween(120),
        label = "configured-result-focus-border",
    )
    var discoveredCover by remember(game.sourceId, game.externalId) {
        mutableStateOf(game.coverUrl ?: presentation.coverUrl)
    }
    var artworkLookupFinished by remember(game.sourceId, game.externalId) {
        mutableStateOf(discoveredCover != null || thumbnailClient == null)
    }
    LaunchedEffect(game.sourceId, game.externalId, discoveredCover, thumbnailClient) {
        if (discoveredCover == null && thumbnailClient != null) {
            discoveredCover = runCatching {
                thumbnailClient.lookupBoxart(game.platform, game.title, game.region)
            }.getOrNull()
            artworkLookupFinished = true
        }
    }
    val coverUrl = discoveredCover ?: presentation.coverUrl
    val editionLabel = configuredSourceResultEditionLabel(game)
    val platformLabel = configuredSourceResultPlatformLabel(game.platform)
    Surface(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .hoverable(source)
            .focusDebugTarget()
            .onFocusChanged { focused = it.isFocused }
            .blueprintActivation(source, performClick, showIndication = false)
            .semantics {
                contentDescription = listOfNotNull(
                    game.title,
                    game.platform,
                    editionLabel,
                    "external discovery result",
                ).joinToString(", ")
                role = Role.Button
            },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            borderWidth,
            if (emphasized) blueprintFocusBrush()
            else Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant),
            ),
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            RemoteArtwork(coverUrl, Modifier.fillMaxSize(), fallbackKey = presentation.title)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color(0x33030810),
                        .45f to Color(0x77030810),
                        1f to Color(0xF0030810),
                    ),
                ),
            )
            Text(
                platformLabel.uppercase(),
                modifier = Modifier.align(Alignment.TopStart).padding(9.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f),
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color(0xEE030810))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    game.title,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                editionLabel?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    when {
                        coverUrl != null -> "Box art · View details"
                        artworkLookupFinished -> "Details / import"
                        else -> "Finding box art…"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun DiscoveryGameRow(games: List<DiscoveryGame>, compact: Boolean, open: (DiscoveryGame) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(games, key = { it.id.value }) { game ->
            DiscoveryGameCard(
                game = game,
                modifier = Modifier.width(if (compact) 190.dp else 230.dp).height(if (compact) 190.dp else 220.dp),
                onClick = { open(game) },
            )
        }
    }
}

@Composable
internal fun DiscoveryGameCard(game: DiscoveryGame, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val performClick = rememberUiClickAction(onClick)
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val emphasized = focused || hovered
    val scale by animateFloatAsState(
        targetValue = if (reducedMotion) 1f else if (pressed) .97f else if (emphasized) 1.018f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(),
        label = "discovery-scale",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (emphasized) 2.dp else 1.dp,
        animationSpec = if (reducedMotion) snap() else androidx.compose.animation.core.tween(120),
        label = "discovery-focus-border",
    )
    Surface(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .focusDebugTarget()
            .onFocusChanged { focused = it.isFocused }
            .hoverable(source)
            .blueprintActivation(source, performClick, showIndication = false)
            .semantics {
                contentDescription = game.title + ", " + game.platformId + ", discover only, import an authorized copy to play"
                role = Role.Button
            },
        shape = RoundedCornerShape(7.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            borderWidth,
            if (emphasized) blueprintFocusBrush()
            else Brush.linearGradient(listOf(GameBoxCardOutlineStart, GameBoxCardOutlineEnd)),
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            RemoteArtwork(game.coverUrl, Modifier.fillMaxSize(), fallbackKey = game.title)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.Transparent, .48f to Color.Transparent, 1f to Color(0xFF040810)),
                ),
            )
            if (game.favorite) {
                Icon(Icons.Rounded.Favorite, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(14.dp))
            }
            Column(Modifier.align(Alignment.BottomStart).padding(9.dp)) {
                Text(game.title, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "Discover · ${game.releaseDate?.take(4) ?: "Import your copy"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

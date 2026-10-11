package com.gamebox.os.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.Game

@Composable
internal fun CatalogFeaturedHero(
    discoveryGame: DiscoveryGame?,
    authorizedGame: Game?,
    modifier: Modifier = Modifier,
    onOpenDiscovery: (DiscoveryGame) -> Unit,
    onOpenAuthorized: (Game) -> Unit,
    fallbackKey: String = discoveryGame?.title ?: authorizedGame?.title ?: "Game Box",
) {
    val coverUrl = discoveryGame?.coverUrl ?: authorizedGame?.artworkUrl
    Surface(
        modifier = modifier.height(204.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val showCover = maxWidth >= 760.dp && !coverUrl.isNullOrBlank()
            RemoteArtwork(
                discoveryGame?.backgroundUrl ?: coverUrl ?: authorizedGame?.artworkUrl,
                Modifier.fillMaxSize(),
                fallbackKey = fallbackKey,
                showFallbackMonogram = false,
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(Color(0xF2040711), Color(0xB0040711), Color(0x18040711)),
                    ),
                ),
            )
            Column(
                Modifier.fillMaxSize().padding(18.dp)
                    .padding(end = if (showCover) 148.dp else 0.dp),
            ) {
                Text("FEATURED", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    discoveryGame?.title ?: authorizedGame?.title ?: "Find your next adventure",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    discoveryGame?.description?.take(110)
                        ?: authorizedGame?.description?.take(110)
                        ?: "Browse local-first metadata and authorized sources.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 430.dp),
                )
                Spacer(Modifier.height(10.dp))
                BlueprintButton(
                    enabled = discoveryGame != null || authorizedGame != null,
                    onClick = {
                        when {
                            discoveryGame != null -> onOpenDiscovery(discoveryGame)
                            authorizedGame != null -> onOpenAuthorized(authorizedGame)
                        }
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "View featured game details"
                    },
                ) {
                    Text("View Details  ›", fontSize = 12.sp)
                }
            }
            if (showCover) {
                GameCoverPoster(
                    title = discoveryGame?.title ?: authorizedGame?.title ?: fallbackKey,
                    artworkUrl = coverUrl,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)
                        .width(108.dp).height(164.dp),
                )
            }
        }
    }
}

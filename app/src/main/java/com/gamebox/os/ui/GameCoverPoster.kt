package com.gamebox.os.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Reusable, aspect-correct cover treatment for wide featured and game-detail heroes. */
@Composable
internal fun GameCoverPoster(
    title: String,
    artworkUrl: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics { contentDescription = "$title cover art" },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.26f)),
        shadowElevation = 12.dp,
    ) {
        RemoteArtwork(artworkUrl, Modifier.fillMaxSize(), fallbackKey = title)
    }
}

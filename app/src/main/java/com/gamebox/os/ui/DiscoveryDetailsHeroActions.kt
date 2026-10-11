@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gamebox.os.ui

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay

/** The discovery hero's import/favorite/back actions share one controller-aware button path. */
@Composable
internal fun DiscoveryDetailsHeroActions(
    compact: Boolean,
    primaryActionFocusRequester: FocusRequester? = null,
    importing: Boolean,
    downloadAvailable: Boolean = false,
    downloadWaiting: Boolean = false,
    downloadFailed: Boolean = false,
    downloadImported: Boolean = false,
    favoriteEnabled: Boolean,
    favorited: Boolean,
    onImport: () -> Unit,
    onDownload: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onFavorite: () -> Unit,
    onBack: () -> Unit,
) {
    var primaryActionFocusClaimed by remember(primaryActionFocusRequester) { mutableStateOf(false) }
    var primaryActionLaidOut by remember(primaryActionFocusRequester) { mutableStateOf(false) }
    LaunchedEffect(primaryActionFocusRequester, primaryActionLaidOut) {
        val requester = primaryActionFocusRequester ?: return@LaunchedEffect
        if (!primaryActionLaidOut || primaryActionFocusClaimed) return@LaunchedEffect
        withFrameNanos { }
        delay(400)
        primaryActionFocusClaimed = runCatching { requester.requestFocus() }.getOrDefault(false)
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val showDownloadManager = downloadWaiting || downloadFailed || downloadImported
        BlueprintButton(
            onClick = when {
                showDownloadManager -> onOpenDownloads
                downloadAvailable -> onDownload
                else -> onImport
            },
            enabled = !importing,
            modifier = (if (compact) Modifier.fillMaxWidth() else Modifier)
                .then(primaryActionFocusRequester?.let(Modifier::focusRequester) ?: Modifier)
                .onGloballyPositioned { primaryActionLaidOut = true }
                .bringFocusedControlIntoView(),
        ) {
            Icon(
                when {
                    showDownloadManager -> Icons.Rounded.Downloading
                    downloadAvailable -> Icons.Rounded.Download
                    else -> Icons.Rounded.FileOpen
                },
                null,
                Modifier.size(16.dp),
            )
            Text(
                when {
                    importing -> "Importing…"
                    downloadWaiting || downloadFailed -> "View Download"
                    downloadImported -> "View Download Manager"
                    downloadAvailable -> "Download"
                    else -> "Import your copy"
                },
                modifier = Modifier.padding(start = 6.dp),
                fontSize = 12.sp,
            )
        }
        if (favoriteEnabled) {
            BlueprintOutlinedButton(
                onClick = onFavorite,
            ) {
                Icon(if (favorited) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, Modifier.size(16.dp))
                Text(if (favorited) "Favorited" else "Favorite", Modifier.padding(start = 6.dp), fontSize = 12.sp)
            }
        }
        BlueprintTextButton(onClick = onBack) {
            Text("Back", fontSize = 12.sp)
        }
    }
}

package com.gamebox.os.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Promotes the common discovery path; local-copy importing remains available in Library. */
@Composable
internal fun GameDetailsDiscoveryCard(
    compact: Boolean,
    onOpenStore: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    BlueprintPanel(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                ),
            ) {
                Icon(
                    Icons.Rounded.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    "VIMM LAIR",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text("Discover and download", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Browse Vimm Lair in GameBox. Supported downloads are tracked and imported automatically.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
            if (!compact) {
                DiscoveryDownloadButton(onOpenStore, focusRequester = focusRequester)
            }
        }
        if (compact) {
            DiscoveryDownloadButton(
                onOpenStore,
                Modifier.fillMaxWidth().padding(top = 4.dp),
                focusRequester,
            )
        }
    }
}

@Composable
private fun DiscoveryDownloadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    BlueprintButton(
        onClick = onClick,
        modifier = modifier.then(focusRequester?.let(Modifier::focusRequester) ?: Modifier),
    ) {
        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
        Text("Download", Modifier.padding(start = 6.dp), fontSize = 12.sp)
    }
}

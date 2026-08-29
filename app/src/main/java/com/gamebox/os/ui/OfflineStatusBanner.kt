package com.gamebox.os.ui

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import com.gamebox.os.data.isNetworkAvailable

@Composable
fun OfflineStatusBanner(context: Context) {
    val online = remember { isNetworkAvailable(context) }
    if (!online) {
        Surface(
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = "Offline mode: cached catalog and installed games remain available"
                liveRegion = LiveRegionMode.Polite
            },
            color = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Text(
                "Offline mode - cached catalog and installed games remain available",
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

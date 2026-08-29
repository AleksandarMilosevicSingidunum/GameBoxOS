package com.gamebox.os.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import com.gamebox.os.data.isNetworkAvailable

@Composable
fun OfflineStatusBanner(context: Context) {
    val manager = remember(context) { context.getSystemService(ConnectivityManager::class.java) }
    var online by remember(context) { mutableStateOf(isNetworkAvailable(context)) }

    DisposableEffect(manager) {
        if (manager == null) return@DisposableEffect onDispose { }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                online = isNetworkAvailable(context)
            }
            override fun onLost(network: Network) {
                online = isNetworkAvailable(context)
            }
            override fun onCapabilitiesChanged(network: Network, capabilities: android.net.NetworkCapabilities) {
                online = isNetworkAvailable(context)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { manager.unregisterNetworkCallback(callback) }
    }

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

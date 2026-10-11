package com.gamebox.os.ui

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.gamebox.os.ui.theme.GameBoxControllerBack
import com.gamebox.os.ui.theme.GameBoxControllerConfirm

@Composable
internal fun QuickActionsDialog(
    destination: Destination,
    xLabel: String,
    yLabel: String,
    onX: () -> Unit,
    onY: () -> Unit,
    onHome: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
    autoFocusFirstAction: Boolean = true,
) {
    val actionFocusRequesters = remember { List(5) { FocusRequester() } }
    LaunchedEffect(autoFocusFirstAction) {
        if (autoFocusFirstAction) {
            androidx.compose.runtime.withFrameNanos { }
            runCatching { actionFocusRequesters.first().requestFocus() }
        }
    }
    fun actionFocus(index: Int): Modifier = Modifier.focusRequester(actionFocusRequesters[index])
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Gamepad, contentDescription = null) },
        title = { Text(destination.title + " quick actions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsActionRow("Downloads", Icons.Rounded.Downloading, modifier = actionFocus(0)) { onDownloads() }
                SettingsActionRow("Home", Icons.Rounded.Home, modifier = actionFocus(1)) { onHome() }
                SettingsActionRow(
                    "X  " + xLabel,
                    Icons.Rounded.Close,
                    modifier = actionFocus(2),
                    onClick = onX,
                )
                SettingsActionRow("Y  " + yLabel, Icons.Rounded.ChangeCircle, modifier = actionFocus(3)) { onY() }
                SettingsActionRow("Settings", Icons.Rounded.Settings, modifier = actionFocus(4)) { onSettings() }
            }
        },
        confirmButton = {},
        dismissButton = { BlueprintTextButton(onClick = onDismiss) { Text("Close") } },
        modifier = Modifier.semantics { contentDescription = "Controller quick actions" },
    )
}

@Composable
internal fun SystemStatusDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val networkService = context.getSystemService(ConnectivityManager::class.java)
    val network = networkService?.getNetworkCapabilities(networkService.activeNetwork)
    val networkLabel = if (network?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) {
        "Connected"
    } else {
        "Offline"
    }
    val storageCapacity = rememberStorageCapacitySnapshot(context)
    val storageSummary = storageCapacity?.let {
        "${formatBytes(it.availableBytes)} free of ${formatBytes(it.totalBytes)}"
    } ?: "Checking…"
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
        title = { Text("System status") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsStatusCard(Icons.Rounded.Storage, "Storage", storageSummary)
                SettingsStatusCard(Icons.Rounded.Wifi, "Network", networkLabel)
                SettingsStatusCard(Icons.Rounded.SportsEsports, "Controller", connectedControllerLabel())
                SettingsStatusCard(Icons.Rounded.Smartphone, "Device", Build.MODEL)
            }
        },
        confirmButton = { BlueprintTextButton(onClick = onDismiss) { Text("Done") } },
        modifier = Modifier.semantics { contentDescription = "System status overlay" },
    )
}

@Composable
internal fun ProfileSwitcherDialog(
    activeProfile: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.SwitchAccount, contentDescription = null) },
        title = { Text("Switch profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Local player", "Guest").forEach { profile ->
                    val isSelected = profile == activeProfile
                    Surface(
                        modifier = Modifier.fillMaxWidth().blueprintClick { onSelect(profile) }
                            .semantics {
                                contentDescription = "$profile profile"
                                selected = isSelected
                            },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                if (profile == "Guest") Icons.Rounded.PersonOutline else Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(profile, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            if (isSelected) Icon(Icons.Rounded.Check, contentDescription = "Active profile")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { BlueprintTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun ControllerFooter(xLabel: String, yLabel: String, profileName: String) {
    val controllerLabel = connectedControllerLabel()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp)
            .semantics { contentDescription = "Controller footer" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ControllerHint("A", "Select", GameBoxControllerConfirm)
            ControllerHint("B", "Back", GameBoxControllerBack)
            ControllerHint("X", xLabel, Color(0xFF3C8DFF))
            ControllerHint("Y", yLabel, Color(0xFFFFC43D))
            Text("LB/RB  Change tab", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text("Menu  Quick actions", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text("View  Status", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                Icons.Rounded.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(25.dp),
            )
            Text(profileName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                controllerLabel,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = "Controller status: $controllerLabel"
                },
            )
        }
    }
}

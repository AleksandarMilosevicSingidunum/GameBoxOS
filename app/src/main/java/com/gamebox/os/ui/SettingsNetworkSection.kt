package com.gamebox.os.ui

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.gamebox.os.settings.GameBoxSettings
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun SettingsNetworkSection(
    modifier: Modifier = Modifier,
    currentSettings: GameBoxSettings,
    settingsRepository: SettingsRepository,
    runtimeStatus: RuntimeDeviceStatus,
    onOpenAndroidNetworkSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var companionSecret by remember { mutableStateOf<String?>(null) }
    var companionTlsFingerprint by remember { mutableStateOf<String?>(null) }
    var companionBusy by remember { mutableStateOf(false) }
    var companionMessage by remember { mutableStateOf<String?>(null) }
    var companionMessageIsError by remember { mutableStateOf(false) }
    LaunchedEffect(settingsRepository) {
        runSettingsUiAction(
            failureMessage = "Pairing secret could not be loaded",
            onBusyChanged = { companionBusy = it },
            onFailure = {
                companionMessage = it
                companionMessageIsError = true
            },
        ) {
            companionSecret = settingsRepository.companionPairingSecret()
            companionTlsFingerprint = withContext(Dispatchers.IO) {
                com.gamebox.os.companion.CompanionTlsIdentity.fingerprint()
            }
        }
    }
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val networkCapabilities = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
    val networkReady = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    SettingsSectionHeader("Network", modifier)
    SettingsStatusCard(
        Icons.Rounded.Wifi,
        runtimeStatus.networkTransportLabel + " · " + runtimeStatus.networkLabel,
        if (networkReady) "Catalog refresh, metadata, downloads and cloud saves can use the current network."
        else "Bundled catalog and installed games remain available.",
    )
    SettingsActionRow("Open Android network settings", Icons.Rounded.Wifi) {
        onOpenAndroidNetworkSettings()
    }
    Text("Windows Companion", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
    Text("Enable a paired local-network status connection. The pairing secret is encrypted on this device.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 12.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(if (currentSettings.companionEnabled) "Companion active on port ${currentSettings.companionPort}" else "Companion disabled")
        BlueprintSwitch(
            checked = currentSettings.companionEnabled,
            enabled = !companionBusy,
            onCheckedChange = { enabled ->
            scope.launch {
                runSettingsUiAction(
                    failureMessage = "Companion configuration could not be updated",
                    onBusyChanged = { companionBusy = it },
                    onFailure = {
                        companionMessage = it
                        companionMessageIsError = true
                    },
                ) {
                    if (enabled && companionSecret == null) {
                        companionSecret = settingsRepository.rotateCompanionPairingSecret()
                    }
                    settingsRepository.setCompanionConfiguration(enabled, currentSettings.companionPort)
                    if (enabled) {
                        try {
                            com.gamebox.os.companion.CompanionEndpointService.start(context)
                        } catch (error: Exception) {
                            settingsRepository.setCompanionConfiguration(false, currentSettings.companionPort)
                            throw error
                        }
                        companionMessage = "Companion enabled"
                    } else {
                        com.gamebox.os.companion.CompanionEndpointService.stop(context)
                        companionMessage = "Companion disabled"
                    }
                    companionMessageIsError = false
                }
            }
        })
    }
    BlueprintOutlinedButton(
        enabled = !companionBusy,
        onClick = {
            scope.launch {
                runSettingsUiAction(
                    failureMessage = "Pairing secret could not be rotated",
                    onBusyChanged = { companionBusy = it },
                    onFailure = {
                        companionMessage = it
                        companionMessageIsError = true
                    },
                ) {
                    companionSecret = settingsRepository.rotateCompanionPairingSecret()
                    companionMessage = "Pairing secret rotated"
                    companionMessageIsError = false
                }
            }
        },
        modifier = Modifier.padding(top = 6.dp),
    ) { Text(if (companionBusy) "Updating…" else "Rotate pairing secret") }
    if (companionBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
    companionMessage?.let {
        Text(
            it,
            color = if (companionMessageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
        )
    }
    companionSecret?.let { Text("Pairing secret: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary) }
    companionTlsFingerprint?.let {
        SelectionContainer {
            Text("TLS SHA-256 fingerprint: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
    Spacer(Modifier.height(18.dp))
}

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
import com.gamebox.os.companion.ActiveCompanionRemotePairing
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
    var showCompanionSecret by remember { mutableStateOf(false) }
    var companionTlsFingerprint by remember { mutableStateOf<String?>(null) }
    var companionBusy by remember { mutableStateOf(false) }
    var companionMessage by remember { mutableStateOf<String?>(null) }
    var companionMessageIsError by remember { mutableStateOf(false) }
    var remotePairingCode by remember { mutableStateOf<String?>(null) }
    var remotePairingSeconds by remember { mutableLongStateOf(0L) }
    var remotePaired by remember { mutableStateOf(false) }
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
            remotePaired = settingsRepository.remoteControlToken() != null
            companionTlsFingerprint = withContext(Dispatchers.IO) {
                com.gamebox.os.companion.CompanionTlsIdentity.fingerprint()
            }
        }
    }
    LaunchedEffect(settingsRepository) {
        while (true) {
            remotePaired = settingsRepository.remoteControlToken() != null
            kotlinx.coroutines.delay(2_000)
        }
    }
    LaunchedEffect(remotePairingCode) {
        if (remotePairingCode == null) return@LaunchedEffect
        while (ActiveCompanionRemotePairing.remainingSeconds() > 0) {
            remotePairingSeconds = ActiveCompanionRemotePairing.remainingSeconds()
            kotlinx.coroutines.delay(1_000)
        }
        ActiveCompanionRemotePairing.close()
        remotePairingCode = null
        remotePairingSeconds = 0
    }
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val networkCapabilities = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
    val networkReady = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val localHostAddress = remember(connectivityManager?.activeNetwork) {
        connectivityManager?.activeNetwork?.let(connectivityManager::getLinkProperties)
            ?.linkAddresses.orEmpty().asSequence().map { it.address }
            .filterIsInstance<java.net.Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress && !it.isLoopbackAddress }?.hostAddress
    }
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
    Text("GameBox Companion", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
    Text("Enable local-network access for GameBox Remote and the Windows companion. Remote controls use a separate HTTP port on your Wi-Fi; the Windows companion keeps TLS.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 12.sp)
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
    if (currentSettings.companionEnabled) {
        Text(
            "Same Wi-Fi: ${localHostAddress ?: "No local IPv4 address found"} · Remote ${com.gamebox.os.companion.CompanionEndpointService.remoteControlPort(currentSettings.companionPort)} · Windows ${currentSettings.companionPort}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("Install GameBox Remote on your other phone. Pair it once with the six-digit code below; it remembers this HONOR and reconnects automatically. Windows companion credentials remain separate.", fontSize = 12.sp)
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
                    if (currentSettings.companionEnabled) {
                        com.gamebox.os.companion.CompanionEndpointService.restart(context)
                    }
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
    companionSecret?.let {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionContainer(Modifier.weight(1f)) {
                Text(
                    "Windows companion key: ${if (showCompanionSecret) it else "••••••••"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(onClick = { showCompanionSecret = !showCompanionSecret }) {
                Text(if (showCompanionSecret) "Hide" else "Show")
            }
        }
    }
    companionTlsFingerprint?.let {
        SelectionContainer {
            Text("Windows companion TLS SHA-256 fingerprint: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
    Text("GameBox Remote pairing", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
    Text(
        "Start a short pairing window once. Pairing a new phone replaces the current remote. The companion stores the pairing on your other phone and reconnects automatically on this Wi-Fi. Revoke it here at any time.",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 12.sp,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        BlueprintOutlinedButton(
            enabled = currentSettings.companionEnabled && !companionBusy,
            onClick = {
                remotePairingCode = ActiveCompanionRemotePairing.open()
                remotePairingSeconds = ActiveCompanionRemotePairing.remainingSeconds()
                companionMessage = "Pairing window opened for two minutes"
                companionMessageIsError = false
            },
        ) { Text("Start remote pairing") }
        BlueprintOutlinedButton(
            enabled = remotePaired && !companionBusy,
            onClick = {
                scope.launch {
                    runSettingsUiAction(
                        failureMessage = "Remote pairing could not be revoked",
                        onBusyChanged = { companionBusy = it },
                        onFailure = { companionMessage = it; companionMessageIsError = true },
                    ) {
                        settingsRepository.revokeRemoteControlToken()
                        remotePaired = false
                        companionMessage = "Remote pairing revoked"
                        companionMessageIsError = false
                    }
                }
            },
        ) { Text("Revoke remote") }
    }
    Text(if (remotePaired) "A remote is paired" else "No remote paired", fontSize = 12.sp)
    remotePairingCode?.let { code ->
        SelectionContainer {
            Text("Pairing code: $code · expires in ${remotePairingSeconds}s", fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
    Spacer(Modifier.height(18.dp))
}

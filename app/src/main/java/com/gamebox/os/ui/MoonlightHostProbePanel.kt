package com.gamebox.os.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gamebox.os.launch.HostReachability
import com.gamebox.os.launch.HostReachabilityResult
import com.gamebox.os.launch.probeMoonlightHost
import com.gamebox.os.settings.GameBoxSettings
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun MoonlightHostProbePanel(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val settings by settingsRepository.settings.collectAsState(initial = GameBoxSettings())
    var host by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("47984") }
    var initialized by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<HostReachabilityResult?>(null) }
    var configurationMessage by remember { mutableStateOf<String?>(null) }
    var probing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(settings.moonlightHost, settings.moonlightPort) {
        if (!initialized) {
            host = settings.moonlightHost
            portText = settings.moonlightPort.toString()
            initialized = true
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PC host reachability")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                host, { host = it.take(253); configurationMessage = null },
                Modifier.weight(1f), label = { Text("Host or IP") }, singleLine = true,
            )
            OutlinedTextField(
                portText, { portText = it.filter(Char::isDigit).take(5); configurationMessage = null },
                Modifier.weight(0.45f), label = { Text("Port") }, singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val port = portText.toIntOrNull() ?: 0
                probing = true
                scope.launch {
                    try {
                        settingsRepository.setMoonlightHost(host, port)
                        configurationMessage = "PC host saved on this device."
                        result = probeMoonlightHost(host, port)
                    } catch (error: IllegalArgumentException) {
                        configurationMessage = error.message ?: "PC host configuration is invalid."
                        result = null
                    } finally {
                        probing = false
                    }
                }
            }, enabled = !probing && host.isNotBlank(),
                modifier = Modifier.semantics { contentDescription = "Check and save PC host" }) {
                if (probing) CircularProgressIndicator() else Text("Check and save host")
            }
            TextButton(
                enabled = !probing && (host.isNotBlank() || settings.moonlightHost.isNotBlank()),
                onClick = {
                    scope.launch {
                        settingsRepository.clearMoonlightHost()
                        host = ""
                        portText = "47984"
                        result = null
                        configurationMessage = "Saved PC host cleared."
                    }
                },
            ) { Text("Clear saved host") }
        }
        configurationMessage?.let { Text(it) }
        result?.let { status ->
            Text(
                status.message,
                modifier = Modifier.semantics {
                    contentDescription = "Host reachability: " + status.message
                },
            )
            if (status.state == HostReachability.UNREACHABLE) {
                Text("The address remains saved. Check that the PC is awake and on the same network, then retry.")
            }
        }
    }
}

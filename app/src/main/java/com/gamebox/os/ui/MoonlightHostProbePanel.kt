package com.gamebox.os.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.launch

@Composable
fun MoonlightHostProbePanel(modifier: Modifier = Modifier) {
    var host by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("47984") }
    var result by remember { mutableStateOf<HostReachabilityResult?>(null) }
    var probing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PC host reachability")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(host, { host = it.take(253) }, Modifier.weight(1f), label = { Text("Host or IP") }, singleLine = true)
            OutlinedTextField(portText, { portText = it.filter(Char::isDigit).take(5) }, Modifier.weight(0.45f), label = { Text("Port") }, singleLine = true)
        }
        Button(onClick = {
            val port = portText.toIntOrNull() ?: 0
            probing = true
            scope.launch { result = probeMoonlightHost(host, port); probing = false }
        }, enabled = !probing, modifier = Modifier.semantics { contentDescription = "Probe PC host" }) {
            if (probing) CircularProgressIndicator() else Text("Check host")
        }
        result?.let { status ->
            Text(status.message, modifier = Modifier.semantics { contentDescription = "Host reachability: " + status.message })
            if (status.state == HostReachability.UNREACHABLE) Text("Check that the PC is awake and on the same network.")
        }
    }
}

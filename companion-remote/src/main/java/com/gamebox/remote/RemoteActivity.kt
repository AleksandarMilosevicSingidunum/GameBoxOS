package com.gamebox.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RemoteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF829BFF), onPrimary = Color.White,
                background = Color(0xFF07101D), onBackground = Color.White,
                surface = Color(0xFF111D2B), onSurface = Color(0xFFEAF1FF),
                surfaceVariant = Color(0xFF26364A), onSurfaceVariant = Color(0xFFC2D0E1),
            )) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { RemoteScreen() }
            }
        }
    }

    @Composable
    private fun RemoteScreen() {
        val store = remember { RemotePairingStore(applicationContext) }
        val requestQueue = remember { RemoteCommandQueue() }
        val scope = rememberCoroutineScope()
        val saved = remember { store.load() }
        var host by remember { mutableStateOf(saved?.host.orEmpty()) }
        var port by remember { mutableStateOf(saved?.port?.toString() ?: "49501") }
        var remoteToken by remember { mutableStateOf(saved?.token.orEmpty()) }
        var pairingCode by remember { mutableStateOf("") }
        var typedText by remember { mutableStateOf("") }
        var status by remember { mutableStateOf(if (saved == null) "Find GameBox and pair this phone once on your Wi-Fi." else "Remembered GameBox found. Reconnecting…") }
        var pendingRequests by remember { mutableIntStateOf(0) }
        val busy = pendingRequests > 0
        var discovering by remember { mutableStateOf(false) }
        var discoveredName by remember { mutableStateOf<String?>(null) }
        var connected by remember { mutableStateOf(false) }

        fun runRequest(action: String, text: String? = null, onSuccess: (() -> Unit)? = null) {
            val pairing = runCatching {
                RemotePairing(host.trim(), port.toInt(), remoteToken.trim()).also {
                    require(it.host.matches(Regex("[A-Za-z0-9.:-]{1,253}"))) { "Enter the host’s local IP address or hostname" }
                    require(it.port in 10_240..65_535)
                    require(it.token.matches(Regex("[0-9a-fA-F]{64}"))) { "Pair this phone with GameBox first" }
                }
            }.getOrElse { status = it.message ?: "Connection details are invalid"; return }
            pendingRequests++
            scope.launch {
                try {
                    requestQueue.execute {
                        runCatching { withContext(Dispatchers.IO) { RemoteClient(pairing).send(action, text) } }
                            .onSuccess {
                                connected = true
                                store.save(pairing)
                                status = if (action == "status") "Connected to GameBox on local Wi-Fi" else "Sent"
                                onSuccess?.invoke()
                            }
                            .onFailure { failure ->
                                when (failure) {
                                    is RemoteUnauthorizedException -> {
                                        connected = false
                                        store.clear()
                                        remoteToken = ""
                                        status = "Saved pairing is no longer valid. Start pairing in GameBox to reconnect."
                                    }
                                    is RemoteSearchUnavailableException -> status = failure.message.orEmpty()
                                    else -> {
                                        connected = false
                                        status = failure.message ?: "GameBox connection failed"
                                    }
                                }
                            }
                    }
                } finally {
                    pendingRequests--
                }
            }
        }

        LaunchedEffect(saved?.token) {
            if (saved != null) {
                pendingRequests++
                try {
                    runCatching { withContext(Dispatchers.IO) { reconnectRemembered(saved) } }
                        .onSuccess {
                            host = it.host
                            port = it.port.toString()
                            store.save(it)
                            connected = true
                            status = "Connected to GameBox on local Wi-Fi"
                        }
                        .onFailure { failure ->
                            connected = false
                            if (failure is RemoteUnauthorizedException) {
                                store.clear()
                                remoteToken = ""
                                status = "Saved pairing is no longer valid. Start pairing in GameBox to reconnect."
                            } else status = failure.message ?: "GameBox connection failed"
                        }
                } finally {
                    pendingRequests--
                    }
            }
        }

        fun pairRemote() {
            val pending = runCatching {
                require(pairingCode.matches(Regex("[0-9]{6}"))) { "Enter the six-digit code shown in GameBox" }
                RemotePairing(host.trim(), port.toInt(), "").also {
                    require(it.host.matches(Regex("[A-Za-z0-9.:-]{1,253}"))) { "Find GameBox or enter its local address" }
                    require(it.port in 10_240..65_535)
                }
            }.getOrElse { status = it.message ?: "Pairing details are invalid"; return }
            pendingRequests++
            scope.launch {
                try {
                    requestQueue.execute {
                        runCatching { withContext(Dispatchers.IO) { RemoteClient(pending).pair(pairingCode) } }
                            .onSuccess {
                                remoteToken = it.token
                                store.save(it)
                                pairingCode = ""
                                status = "Paired. Connect to start using the remote; this phone will remember GameBox."
                            }
                            .onFailure { status = it.message ?: "Pairing failed" }
                    }
                } finally {
                    pendingRequests--
                }
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("GameBox Remote", style = MaterialTheme.typography.headlineMedium)
            Text("Connect to your HONOR host on the same Wi-Fi. Pair this phone once with the short code shown in GameBox. Its encrypted saved pairing reconnects automatically next time.")
            OutlinedButton(
                enabled = !discovering,
                onClick = {
                    scope.launch {
                        discovering = true
                        runCatching { withContext(Dispatchers.IO) { discoverGameBox() } }
                            .onSuccess {
                                host = it.host
                                port = it.port.toString()
                                discoveredName = it.name
                                status = if (remoteToken.isNotEmpty()) "Found ${it.name}. Reconnecting…" else "Found ${it.name}. Enter the pairing code shown in GameBox."
                                if (remoteToken.isNotEmpty()) runRequest("status")
                            }
                            .onFailure { status = it.message ?: "GameBox was not found; enter the host manually." }
                        discovering = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (discovering) "Searching local network…" else "Find GameBox on Wi-Fi") }
            discoveredName?.let { Text("Found: $it", color = MaterialTheme.colorScheme.primary) }
            OutlinedTextField(host, { host = it }, Modifier.fillMaxWidth(), label = { Text("GameBox host IP / name") }, singleLine = true, placeholder = { Text("192.168.1.20") })
            OutlinedTextField(port, { port = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth(), label = { Text("Port") }, singleLine = true)
            if (remoteToken.isEmpty()) {
                OutlinedTextField(pairingCode, { pairingCode = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), label = { Text("Six-digit pairing code") }, singleLine = true)
                Button(enabled = !busy && pairingCode.length == 6, onClick = { pairRemote() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "Pairing…" else "Pair this phone")
                }
            }
            if (remoteToken.isNotEmpty()) {
                Button(enabled = !busy, onClick = { runRequest("status") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "Connecting…" else if (connected) "Reconnect" else "Connect")
                }
                if (!connected) {
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            store.clear()
                            remoteToken = ""
                            status = "Saved pairing removed from this phone. Start pairing in GameBox to pair again."
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) { Text("Forget saved pairing") }
                    Text(
                        "This removes the saved credential from this phone. It does not revoke access on GameBox.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(status, color = if (status.contains("failed", true) || status.contains("invalid", true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            if (connected) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("TV controls", style = MaterialTheme.typography.titleLarge)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RemoteButton("▲") { runRequest("up") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RemoteButton("◀") { runRequest("left") }
                        RemoteButton("OK", emphasized = true) { runRequest("select") }
                        RemoteButton("▶") { runRequest("right") }
                    }
                    RemoteButton("▼") { runRequest("down") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { runRequest("back") }, modifier = Modifier.weight(1f)) { Text("Back") }
                    OutlinedButton(onClick = { runRequest("home") }, modifier = Modifier.weight(1f)) { Text("TV Home") }
                    OutlinedButton(onClick = { runRequest("gamebox") }, modifier = Modifier.weight(1f)) { Text("GameBox") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { runRequest("play_pause") }, modifier = Modifier.weight(1f)) { Text("Play / pause") }
                    OutlinedButton(onClick = { runRequest("backspace") }, modifier = Modifier.weight(1f)) { Text("⌫") }
                }
                OutlinedTextField(typedText, { typedText = it.take(128) }, Modifier.fillMaxWidth(), label = { Text("Type into GameBox search") }, singleLine = true)
                Button(enabled = typedText.isNotEmpty(), onClick = { runRequest("text", typedText) { typedText = "" } }, modifier = Modifier.fillMaxWidth()) {
                    Text("Send text")
                }
                TextButton(
                    enabled = !busy,
                    onClick = {
                        val pairing = RemotePairing(host.trim(), port.toIntOrNull() ?: -1, remoteToken)
                        pendingRequests++
                        scope.launch {
                            try {
                                requestQueue.execute {
                                    runCatching { withContext(Dispatchers.IO) { RemoteClient(pairing).revoke() } }
                                        .onSuccess {
                                            store.clear(); remoteToken = ""; connected = false
                                            status = "Disconnected and pairing revoked on GameBox."
                                        }
                                        .onFailure { status = it.message ?: "GameBox could not revoke this remote" }
                                }
                            } finally {
                                pendingRequests--
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Disconnect and revoke pairing") }
                }
            }
        }

    @Composable
    private fun RemoteButton(label: String, emphasized: Boolean = false, onClick: () -> Unit) {
        Button(onClick = onClick, modifier = Modifier.size(if (emphasized) 84.dp else 64.dp),
            colors = if (emphasized) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()) {
            Text(label, style = MaterialTheme.typography.titleLarge)
        }
    }
}

internal class RemoteCommandQueue {
    private val mutex = Mutex()

    suspend fun <T> execute(request: suspend () -> T): T = mutex.withLock { request() }
}

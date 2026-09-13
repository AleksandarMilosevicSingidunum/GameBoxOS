package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.gamebox.os.domain.Game
import com.gamebox.os.storage.ContentRemovalPreview
import com.gamebox.os.storage.CloudBackupAcknowledgementRequired
import com.gamebox.os.storage.CloudBackupPreflight
import com.gamebox.os.storage.SaveSafetyController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ContentRemovalDialog(game: Game, controller: SaveSafetyController, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var preview by remember(game.id) { mutableStateOf<ContentRemovalPreview?>(null) }
    var message by remember(game.id) { mutableStateOf<String?>(null) }
    var busy by remember(game.id) { mutableStateOf(false) }
    var finished by remember(game.id) { mutableStateOf(false) }
    var cloudPreflight by remember(game.id) { mutableStateOf<CloudBackupPreflight?>(null) }
    var cloudFailureAfterPreflight by remember(game.id) { mutableStateOf(false) }
    var acknowledgeLocalOnly by remember(game.id) { mutableStateOf(false) }
    LaunchedEffect(game.id) {
        try {
            preview = withContext(Dispatchers.IO) { controller.contentRemovalPreview(game) }
            cloudPreflight = controller.cloudBackupPreflight(game)
        }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { message = "Cannot verify this game's owned content and backup readiness. No files were removed." }
    }
    val cloudAcknowledgementRequired = cloudFailureAfterPreflight ||
        cloudPreflight?.requiresAcknowledgement == true
    AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        title = { Text("Uninstall ${game.title}?") },
        text = {
            Column {
                Text(preview?.let { "${it.files} content file(s), ${it.bytes} bytes" } ?: "Checking owned content…")
                Text("Saves, backups, metadata, favorites and history are retained. External emulator files are not touched.")
                cloudPreflight?.let { Text(it.message) }
                if (cloudAcknowledgementRequired && !finished) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = acknowledgeLocalOnly,
                            enabled = !busy,
                            onCheckedChange = { acknowledgeLocalOnly = it },
                        )
                        Text(
                            "I understand cloud backup is unavailable and want to continue using the verified local backup.",
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
                message?.let { Text(it) }
                if (busy) LinearProgressIndicator()
            }
        },
        confirmButton = {
            Button(enabled = !busy && (finished ||
                (preview != null && cloudPreflight != null &&
                    (!cloudAcknowledgementRequired || acknowledgeLocalOnly))), onClick = {
                if (finished) onClose() else {
                    busy = true
                    scope.launch {
                        try {
                            message = controller.uninstallContent(game, acknowledgeLocalOnly)
                            finished = true
                        }
                        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (required: CloudBackupAcknowledgementRequired) {
                            cloudFailureAfterPreflight = true
                            acknowledgeLocalOnly = false
                            message = required.message
                        }
                        catch (error: Exception) { message = error.message ?: "Removal failed; saves retained" }
                        finally { busy = false }
                    }
                }
            }) { Text(if (finished) "Close" else "Uninstall content") }
        },
        dismissButton = { if (!finished) OutlinedButton(enabled = !busy, onClick = onClose) { Text("Cancel") } },
    )
}

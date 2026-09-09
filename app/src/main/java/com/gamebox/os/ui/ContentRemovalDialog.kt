package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.gamebox.os.domain.Game
import com.gamebox.os.storage.ContentRemovalPreview
import com.gamebox.os.storage.SaveSafetyController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ContentRemovalDialog(game: Game, controller: SaveSafetyController, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var preview by remember(game.id) { mutableStateOf<ContentRemovalPreview?>(null) }
    var message by remember(game.id) { mutableStateOf<String?>(null) }
    var busy by remember(game.id) { mutableStateOf(false) }
    var finished by remember(game.id) { mutableStateOf(false) }
    LaunchedEffect(game.id) {
        try { preview = withContext(Dispatchers.IO) { controller.contentRemovalPreview(game) } }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { message = "Cannot verify this game's owned content. No files were removed." }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        title = { Text("Uninstall ${game.title}?") },
        text = {
            Column {
                Text(preview?.let { "${it.files} content file(s), ${it.bytes} bytes" } ?: "Checking owned content…")
                Text("Saves, backups, metadata, favorites and history are retained. External emulator files are not touched.")
                message?.let { Text(it) }
                if (busy) LinearProgressIndicator()
            }
        },
        confirmButton = {
            Button(enabled = !busy && (finished || preview != null), onClick = {
                if (finished) onClose() else {
                    busy = true
                    scope.launch {
                        try { message = controller.uninstallContent(game); finished = true }
                        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (error: Exception) { message = error.message ?: "Removal failed; saves retained" }
                        finally { busy = false }
                    }
                }
            }) { Text(if (finished) "Close" else "Uninstall content") }
        },
        dismissButton = { if (!finished) OutlinedButton(enabled = !busy, onClick = onClose) { Text("Cancel") } },
    )
}

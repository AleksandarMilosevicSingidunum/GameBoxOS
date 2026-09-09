package com.gamebox.os.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gamebox.os.domain.Game
import com.gamebox.os.storage.SaveSafetyController

/** Managed copies only. This does not write into an emulator's private save folder. */
@Composable
internal fun PerGameSaveCard(game: Game, controller: SaveSafetyController, enabled: Boolean) {
    val busy by controller.observeBusy().collectAsState()
    val actionsEnabled = enabled && !busy
    val state by controller.observeState().collectAsState()
    var confirmRestore by remember(game.id) { mutableStateOf(false) }
    val importSave = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uri -> uri?.let(controller::importInitialSave)
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        uri -> uri?.let(controller::exportBackup)
    }
    Surface(Modifier.fillMaxWidth().padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(16.dp)) {
            Text("Save copy — ${game.title}", style = MaterialTheme.typography.titleMedium)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Manage a single-file copy in GameBox. These actions do not change your emulator's live saves. Export the copy and use the emulator's import process when supported.")
            if (state.saveRecordPresent) {
                Text("${state.sizeBytes} bytes • ${state.relativePath}")
                Button(onClick = controller::backupSave, enabled = actionsEnabled) { Text("Back up save copy") }
                OutlinedButton(onClick = { confirmRestore = true }, enabled = actionsEnabled && state.backupPresent) {
                    Text("Restore save copy")
                }
                OutlinedButton(onClick = { export.launch("${game.id.value}-save-backup.dat") },
                    enabled = actionsEnabled && state.backupPresent) { Text("Export save backup") }
            } else {
                Text("No managed save copy. Select a save you exported from this game's emulator.")
                Button(onClick = { importSave.launch(arrayOf("*/*")) }, enabled = actionsEnabled) { Text("Import save copy") }
            }
            state.operationMessage?.let { Text(it,
                color = if (state.operationSuccessful) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error) }
        }
    }
    if (confirmRestore) AlertDialog(
        onDismissRequest = { confirmRestore = false },
        title = { Text("Replace this save copy?") },
        text = { Text("The verified backup will replace GameBox's current save copy for ${game.title}. Your emulator's live save is not changed.") },
        confirmButton = { Button(enabled = actionsEnabled && state.backupPresent, onClick = {
            confirmRestore = false
            controller.restoreSave()
        }) { Text("Replace save copy") } },
        dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } }
    )
}


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
    var confirmImport by remember(game.id) { mutableStateOf(false) }
    // Keep the launch target, rather than routing a delayed picker result to a new game.
    var pickerTarget by remember { mutableStateOf<SaveSafetyController?>(null) }
    val importSave = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uri ->
        val target = pickerTarget
        pickerTarget = null
        if (uri != null && target === controller && actionsEnabled) target?.importInitialSave(uri)
    }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uri ->
        val target = pickerTarget
        pickerTarget = null
        if (uri != null && target === controller && actionsEnabled) target?.importBackup(uri)
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        uri ->
        val target = pickerTarget
        pickerTarget = null
        if (uri != null && target === controller && actionsEnabled) target?.exportBackup(uri)
    }
    Surface(Modifier.fillMaxWidth().padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(16.dp)) {
            Text("Save copy — ${game.title}", style = MaterialTheme.typography.titleMedium)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Manage a single-file copy in GameBox. These actions do not change your emulator's live saves. Export the copy and use the emulator's import process when supported.")
            if (state.saveRecordPresent) {
                Text("${state.sizeBytes} bytes • ${state.relativePath}")
                Button(onClick = controller::backupSave, enabled = actionsEnabled) { Text("Back up save copy") }
                OutlinedButton(onClick = { confirmImport = true }, enabled = actionsEnabled) {
                    Text("Import save backup")
                }
                OutlinedButton(onClick = { confirmRestore = true }, enabled = actionsEnabled && state.backupPresent) {
                    Text("Restore save copy")
                }
                OutlinedButton(onClick = { pickerTarget = controller; export.launch("${game.id.value}-save-backup.dat") },
                    enabled = actionsEnabled && state.backupPresent) { Text("Export save backup") }
            } else {
                Text("No managed save copy. Select a save you exported from this game's emulator.")
                Button(onClick = { pickerTarget = controller; importSave.launch(arrayOf("*/*")) }, enabled = actionsEnabled) { Text("Import save copy") }
            }
            state.operationMessage?.let { Text(it,
                color = if (state.operationSuccessful) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error) }
        }
    }
    if (confirmImport) AlertDialog(
        onDismissRequest = { confirmImport = false },
        title = { Text("Import a backup for ${game.title}?") },
        text = { Text("Choose a single-file save exported for this game, up to 16 MiB. It will replace the stored backup, not the current save copy or your emulator's live save. Use Restore save copy afterward to apply it. GameBox cannot verify that a selected file belongs to this game.") },
        confirmButton = { Button(enabled = actionsEnabled, onClick = {
            confirmImport = false
            pickerTarget = controller
            importBackup.launch(arrayOf("*/*"))
        }) { Text("Choose backup file") } },
        dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Cancel") } }
    )
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

package com.gamebox.os.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gamebox.os.domain.Game
import com.gamebox.os.storage.SaveSafetyController

/** Managed copies only. This does not write into an emulator's private save folder. */
@Composable
internal fun PerGameSaveCard(game: Game, controller: SaveSafetyController, compact: Boolean, enabled: Boolean) {
    val context = LocalContext.current
    val documentPickerOptions = remember(context) {
        com.gamebox.os.display.externalDisplayActivityResultOptions(context)
    }
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
        if (uri != null && target === controller && actionsEnabled) target.importInitialSave(uri)
    }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uri ->
        val target = pickerTarget
        pickerTarget = null
        if (uri != null && target === controller && actionsEnabled) target.importBackup(uri)
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        uri ->
        val target = pickerTarget
        pickerTarget = null
        if (uri != null && target === controller && actionsEnabled) target.exportBackup(uri)
    }
    val localSaveContent: @Composable ColumnScope.() -> Unit = {
        if (state.saveRecordPresent) {
            Text("${state.sizeBytes} bytes • ${state.relativePath}", style = MaterialTheme.typography.bodySmall)
            SaveCardAction("Back up save copy", compact, actionsEnabled, filled = true, onClick = controller::backupSave)
            SaveCardAction("Import save backup", compact, actionsEnabled, onClick = { confirmImport = true })
            SaveCardAction("Restore save copy", compact, actionsEnabled && state.backupPresent,
                onClick = { confirmRestore = true })
            SaveCardAction("Export save backup", compact, actionsEnabled && state.backupPresent,
                onClick = { pickerTarget = controller; export.launch("${game.id.value}-save-backup.dat", documentPickerOptions) })
        } else {
            Text("No managed save copy. Select a save you exported from this game's emulator.")
            SaveCardAction("Import save copy", compact, actionsEnabled, filled = true,
                onClick = { pickerTarget = controller; importSave.launch(arrayOf("*/*"), documentPickerOptions) })
        }
    }
    val cloudSaveContent: @Composable ColumnScope.() -> Unit = {
        SaveCardAction("Upload cloud copy", compact, actionsEnabled && state.saveRecordPresent,
            filled = true, onClick = controller::uploadCloudSave)
        SaveCardAction("Restore cloud copy", compact, actionsEnabled, onClick = controller::downloadCloudSave)
    }
    if (compact) {
        Surface(Modifier.fillMaxWidth().padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Save copy — ${game.title}", style = MaterialTheme.typography.titleMedium)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Manage a single-file copy in GameBox. These actions do not change your emulator's live saves. Export the copy and use the emulator's import process when supported.")
                localSaveContent()
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Encrypted cloud copy", style = MaterialTheme.typography.titleSmall)
                Text("Uses the WebDAV or S3-compatible provider configured in Settings. A different local copy is preserved before cloud restore.")
                cloudSaveContent()
                state.operationMessage?.let { Text(it,
                    color = if (state.operationSuccessful) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error) }
            }
        }
    } else {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BlueprintPanel(Modifier.weight(1f)) {
                    Text("Save copy", style = MaterialTheme.typography.titleMedium)
                    Text(game.title, color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Text(
                        "GameBox manages a separate single-file copy; it never changes the emulator's live save. Export it and use the emulator's import process when supported.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    localSaveContent()
                }
                BlueprintPanel(Modifier.weight(1f)) {
                    Text("Encrypted cloud copy", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Uses the WebDAV or S3-compatible provider configured in Settings. A different local copy is preserved before cloud restore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    cloudSaveContent()
                }
            }
            state.operationMessage?.let { Text(it,
                color = if (state.operationSuccessful) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error) }
        }
    }
    if (confirmImport) AlertDialog(
        onDismissRequest = { confirmImport = false },
        title = { Text("Import a backup for ${game.title}?") },
        text = { Text("Choose a single-file save exported for this game, up to 16 MiB. It will replace the stored backup, not the current save copy or your emulator's live save. Use Restore save copy afterward to apply it. GameBox cannot verify that a selected file belongs to this game.") },
        confirmButton = { BlueprintButton(enabled = actionsEnabled, onClick = {
            confirmImport = false
            pickerTarget = controller
            importBackup.launch(arrayOf("*/*"), documentPickerOptions)
        }) { Text("Choose backup file") } },
        dismissButton = { BlueprintTextButton(onClick = { confirmImport = false }) { Text("Cancel") } }
    )
    if (confirmRestore) AlertDialog(
        onDismissRequest = { confirmRestore = false },
        title = { Text("Replace this save copy?") },
        text = { Text("The verified backup will replace GameBox's current save copy for ${game.title}. Your emulator's live save is not changed.") },
        confirmButton = { BlueprintButton(enabled = actionsEnabled && state.backupPresent, onClick = {
            confirmRestore = false
            controller.restoreSave()
        }) { Text("Replace save copy") } },
        dismissButton = { BlueprintTextButton(onClick = { confirmRestore = false }) { Text("Cancel") } }
    )
}

@Composable
private fun SaveCardAction(
    label: String,
    compact: Boolean,
    enabled: Boolean,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    if (compact) {
        if (filled) {
            BlueprintButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.bringFocusedControlIntoView()
                    .semantics { contentDescription = label },
            ) { Text(label) }
        } else {
            BlueprintOutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.bringFocusedControlIntoView()
                    .semantics { contentDescription = label },
            ) { Text(label) }
        }
        return
    }
    val actionModifier = if (enabled) {
        Modifier.bringFocusedControlIntoView().blueprintClick(onClick)
    } else {
        Modifier.semantics {
            role = Role.Button
            disabled()
        }
    }
    Box(
        modifier = actionModifier
            .heightIn(min = 40.dp)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = if (filled && enabled) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(
                1.dp,
                if (enabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            ),
        ) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                Text(
                    label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

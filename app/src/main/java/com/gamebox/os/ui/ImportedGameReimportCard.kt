package com.gamebox.os.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gamebox.os.data.GameRepository
import com.gamebox.os.content.GameMutationGate
import com.gamebox.os.data.ImportedGameRegistration
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.LocalContentFile
import com.gamebox.os.domain.canReimportContent
import com.gamebox.os.importer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ImportedGameReimportCard(game: Game, importer: AuthorizedRomImporter,
    repository: GameRepository, enabled: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember(game.id) { mutableStateOf(false) }
    var message by remember(game.id) { mutableStateOf<String?>(null) }
    var confirmForget by remember(game.id) { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) message = "No files selected" else {
            busy = true
            scope.launch {
                try {
                    // Once confirmed, finish registration even if navigation removes this card.
                    message = GameMutationGate.withGameLock(game.id.value) {
                        withContext(Dispatchers.IO + NonCancellable) {
                        val current = requireNotNull(repository.game(game.id)) { "Game is no longer in the library" }
                        require(current.canReimportContent()) { "Game state changed; reopen its details" }
                        val sources = uris.map { uri ->
                            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME),
                                null, null, null)?.use { cursor ->
                                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
                            } ?: error("Selected file has no display name")
                            RomImportSource(uri, name)
                        }
                        val original = LocalContentFile(requireNotNull(current.localContentRelativePath),
                            requireNotNull(current.localContentSha256) { "Original checksum is missing; use a new import" },
                            current.localContentMimeType ?: "application/octet-stream")
                        val expected = listOf(original) + current.localContentFiles.filterNot {
                            it.relativePath == original.relativePath
                        }
                        when (val result = importer.importSet(current.id, sources, current.platform, expected)) {
                            is RomImportSetResult.Imported -> {
                                val primary = result.launchFile
                                try {
                                    repository.registerImportedGame(ImportedGameRegistration(
                                        current.id, current.title, current.platform, current.year,
                                        result.files.sumOf { it.hashes.sizeBytes },
                                        RomImportPolicy.importRootRelativePath(current.id, primary.relativePath),
                                        primary.hashes.sha256, primary.mimeType,
                                        favorite = current.favorite, artworkUrl = current.artworkUrl,
                                        description = current.description, players = current.players, region = current.region,
                                        additionalFiles = result.files.filter { it != primary }.map {
                                            LocalContentFile(RomImportPolicy.importRootRelativePath(current.id, it.relativePath),
                                                it.hashes.sha256, it.mimeType)
                                        }))
                                } catch (error: Exception) {
                                    runCatching { importer.rollbackRegistration(current.id, result.transactionId) }
                                    throw error
                                }
                                val cleanupDeferred = runCatching {
                                    importer.confirmRegistration(current.id, result.transactionId)
                                }.isFailure
                                if (cleanupDeferred) {
                                    "Content verified and restored. Saves and history retained; transaction cleanup will finish after restart."
                                } else {
                                    "Content verified and restored. Existing saves and history retained."
                                }
                            }
                            RomImportSetResult.SourceUnavailable -> "A selected file could not be opened; select the files again."
                            is RomImportSetResult.Rejected -> "Reimport rejected: ${result.reason}"
                            is RomImportSetResult.Failed -> "Reimport failed: ${result.reason}"
                        }
                        }
                    }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    message = "Reimport did not complete: " + (error.message?.take(160) ?: "check storage and retry")
                } finally { busy = false }
            }
        }
    }
    if (confirmForget) {
        AlertDialog(
            onDismissRequest = { confirmForget = false },
            title = { Text("Forget missing game files?") },
            text = {
                Text("GameBox will remove only its stale file references. Saves, backups, artwork, favorites, metadata and play history are retained. You can import or install the game again later.")
            },
            confirmButton = {
                Button(onClick = {
                    confirmForget = false
                    busy = true
                    scope.launch {
                        message = try {
                            val forgotten = GameMutationGate.withGameLock(game.id.value) {
                                withContext(Dispatchers.IO + NonCancellable) {
                                    repository.forgetMissingImportedContent(game.id)
                                }
                            }
                            if (forgotten) "Missing file references forgotten. Saves and library metadata retained."
                            else "Nothing changed because the game state changed; reopen Details and retry."
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            "Could not forget missing files: " +
                                (error.message?.take(160) ?: "check storage and retry")
                        } finally { busy = false }
                    }
                }) { Text("Forget file references") }
            },
            dismissButton = { TextButton(onClick = { confirmForget = false }) { Text("Cancel") } },
        )
    }
    if (game.canReimportContent() || busy || message != null) {
        Surface(Modifier.fillMaxWidth().padding(bottom = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (game.canReimportContent()) "Repair missing game files" else "Game file repair",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Select the original game file, or the descriptor and all tracks with their original filenames. Checksums must match. You can instead forget stale file references. Saves and history are always kept.")
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = enabled && !busy && game.canReimportContent(),
                        onClick = { picker.launch(arrayOf("*/*")) }) { Text("Locate files") }
                    TextButton(
                        enabled = enabled && !busy && game.canReimportContent(),
                        onClick = { confirmForget = true },
                    ) { Text("Forget") }
                }
                message?.let { Text(it) }
            }
        }
    }
}


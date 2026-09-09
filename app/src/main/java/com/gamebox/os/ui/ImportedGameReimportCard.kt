package com.gamebox.os.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gamebox.os.data.GameRepository
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
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) message = "No files selected" else {
            busy = true
            scope.launch {
                try {
                    // Once confirmed, finish registration even if navigation removes this card.
                    message = withContext(Dispatchers.IO + NonCancellable) {
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
                        when (val result = importer.importSet(current.id, sources, current.platform)) {
                            is RomImportSetResult.Imported -> {
                                val primary = result.launchFile
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
                                "Content verified and restored. Existing saves and history retained."
                            }
                            RomImportSetResult.SourceUnavailable -> "A selected file could not be opened; select the files again."
                            is RomImportSetResult.Rejected -> "Reimport rejected: ${result.reason}"
                            is RomImportSetResult.Failed -> "Reimport failed: ${result.reason}"
                        }
                    }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    message = "Reimport did not complete: " + (error.message?.take(160) ?: "check storage and retry")
                } finally { busy = false }
            }
        }
    }
    if (game.canReimportContent() || busy || message != null) {
        Surface(Modifier.fillMaxWidth().padding(bottom = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(16.dp)) {
                Text("Restore imported game", style = MaterialTheme.typography.titleMedium)
                Text("Select your own game file, or the descriptor and all tracks for a disc set. Saves and history are kept.")
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Button(enabled = enabled && !busy && game.canReimportContent(),
                    onClick = { picker.launch(arrayOf("*/*")) }) { Text("Select game files to reimport") }
                message?.let { Text(it) }
            }
        }
    }
}


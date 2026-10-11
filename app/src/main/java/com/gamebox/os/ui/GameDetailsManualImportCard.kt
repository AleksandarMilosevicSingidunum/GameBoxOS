package com.gamebox.os.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.data.GameRepository
import com.gamebox.os.display.externalDisplayActivityResultOptions
import com.gamebox.os.domain.Game
import com.gamebox.os.importer.AuthorizedRomImporter
import com.gamebox.os.importer.RomImportPolicy
import com.gamebox.os.importer.RomImportProgress
import com.gamebox.os.importer.RomImportSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal fun shouldRemoveImportedArchive(displayName: String): Boolean =
    displayName.substringAfterLast('.', "").lowercase() in setOf("zip", "7z")

@Composable
internal fun GameDetailsManualImportCard(
    game: Game,
    importer: AuthorizedRomImporter,
    repository: GameRepository,
    enabled: Boolean,
) {
    if (!shouldShowAuthorizedImportCard(game.state, authorizedFixture = game.id.value == "galaxy-patrol")) return

    val context = LocalContext.current
    val pickerOptions = remember(context) { externalDisplayActivityResultOptions(context) }
    val scope = rememberCoroutineScope()
    val allowMultiple = RomImportPolicy.supportsMultiFile(game.platform)
    var importJob by remember(game.id) { mutableStateOf<Job?>(null) }
    var progress by remember(game.id) { mutableStateOf<RomImportProgress?>(null) }
    var message by remember(game.id) { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(OpenGameFilesContract(allowMultiple)) { uris ->
        if (uris.isEmpty()) {
            message = "No files selected"
        } else {
            message = null
            importJob = scope.launch {
                val discovery = game.asDiscoveryGame()
                val workflow = DiscoveryImportWorkflow(context, importer, repository, discovery, game.platform)
                try {
                    message = if (uris.size > 1) {
                        workflow.importDiscSet(uris.map { uri ->
                            RomImportSource(uri, workflow.displayName(uri))
                        }) { progress = it }
                    } else {
                        val uri = uris.single()
                        val displayName = workflow.displayName(uri)
                        workflow.importSingle(
                            uri = uri,
                            displayName = displayName,
                            removeSourceAfterSuccess = shouldRemoveImportedArchive(displayName),
                            onProgress = { progress = it },
                        ).message
                    }
                } catch (cancelled: CancellationException) {
                    message = "Import cancelled; no incomplete game was added."
                    throw cancelled
                } catch (error: Exception) {
                    message = "Import failed: " + (error.message?.take(160) ?: "check the selected file and storage")
                } finally {
                    progress = null
                    importJob = null
                }
            }
        }
    }

    DiscoveryImportSection(
        platformLabel = game.platform,
        acceptedFormats = RomImportPolicy.supportedExtensionsLabel(game.platform),
        importing = importJob?.isActive == true,
        progress = progress,
        message = message,
        supportsDiscSet = allowMultiple,
        showCopyImport = true,
        actionsEnabled = enabled,
        onCancel = {
            message = "Cancelling import…"
            importJob?.cancel()
        },
        onImportCopy = { picker.launch(arrayOf("*/*"), pickerOptions) },
        onImportDiscSet = { picker.launch(arrayOf("*/*"), pickerOptions) },
    )
}

private fun Game.asDiscoveryGame() = DiscoveryGame(
    id = id,
    title = title,
    platformId = platform.lowercase(),
    region = region,
    releaseDate = year.takeIf { it > 0 }?.toString(),
    description = description,
    players = players,
    rating = null,
    coverUrl = artworkUrl,
    backgroundUrl = metadataOverrides.artworkUrl ?: artworkUrl,
    logoUrl = null,
    screenshots = emptyList(),
    favorite = favorite,
)

package com.gamebox.os.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.data.GameRepository
import com.gamebox.os.display.externalDisplayActivityResultOptions
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.InstallState
import com.gamebox.os.importer.BrowserDownloadRecord
import com.gamebox.os.importer.BrowserDownloadStatus
import com.gamebox.os.importer.AuthorizedRomImporter
import com.gamebox.os.importer.cancelManualRomImport
import com.gamebox.os.importer.enqueueManualRomImport
import com.gamebox.os.importer.RomImportPolicy
import com.gamebox.os.importer.RomImportProgress
import com.gamebox.os.importer.RomImportStage
import com.gamebox.os.importer.DiscoveryImportWorkflow
import com.gamebox.os.importer.shouldRemoveImportedArchive

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
    val allowMultiple = RomImportPolicy.supportsMultiFile(game.platform)
    var message by remember(game.id) { mutableStateOf<String?>(null) }
    val app = remember(context) { context.applicationContext as GameBoxApplication }
    val store = app.browserDownloadStore
    val records by store.records.collectAsState()
    val activeImport = remember(records, game.id) {
        records.filter { it.manualImport && it.gameId == game.id.value }
            .maxByOrNull(BrowserDownloadRecord::createdAtMillis)
            ?.takeIf { it.status in setOf(BrowserDownloadStatus.QUEUED, BrowserDownloadStatus.IMPORTING) }
    }
    val latestImport = remember(records, game.id) {
        records.filter { it.manualImport && it.gameId == game.id.value }
            .maxByOrNull(BrowserDownloadRecord::createdAtMillis)
    }
    val picker = rememberLauncherForActivityResult(OpenGameFilesContract(allowMultiple)) { selection ->
        val uris = selection.uris
        if (uris.isEmpty()) {
            message = "No files selected"
        } else {
            try {
                val workflow = DiscoveryImportWorkflow(context, importer, repository, game.asDiscoveryGame(), game.platform)
                val names = uris.map(workflow::displayName)
                enqueueManualRomImport(context, store, game.asDiscoveryGame(), game.platform, uris, names, selection.grantFlags)
                message = "Import queued. Progress is available in Download Manager."
            } catch (error: Exception) {
                message = "Could not keep access to the selected file: ${error.message?.take(120) ?: "choose it again"}"
            }
        }
    }

    DiscoveryImportSection(
        platformLabel = game.platform,
        acceptedFormats = RomImportPolicy.supportedExtensionsLabel(game.platform),
        importing = activeImport != null,
        progress = activeImport?.let(::manualImportProgress),
        message = message ?: latestImport?.let { record ->
            manualImportOutcome(record, game.state)
        },
        supportsDiscSet = allowMultiple,
        showCopyImport = true,
        actionsEnabled = enabled,
        onCancel = {
            activeImport?.let { cancelManualRomImport(context, store, it) }
            message = "Cancelling import…"
        },
        onImportCopy = { picker.launch(arrayOf("*/*"), pickerOptions) },
        onImportDiscSet = { picker.launch(arrayOf("*/*"), pickerOptions) },
    )
}

internal fun manualImportOutcome(record: BrowserDownloadRecord, gameState: InstallState): String? =
    when {
        record.status == BrowserDownloadStatus.IMPORTED &&
            gameState in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE) ->
            "Import complete. The game is ready in your Library."
        record.status in setOf(BrowserDownloadStatus.FAILED, BrowserDownloadStatus.CANCELLED) -> record.error
        else -> null
    }

internal fun manualImportProgress(record: BrowserDownloadRecord) = record.importStage?.let { label ->
    val stage = RomImportStage.entries.firstOrNull { it.label == label } ?: return@let null
    RomImportProgress(stage, record.importBytesProcessed, record.importTotalBytes, record.importFileIndex, record.importFileCount)
}

internal fun Game.asDiscoveryGame() = DiscoveryGame(
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

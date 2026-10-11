package com.gamebox.os.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.gamebox.os.content.GameMutationGate
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.LocalContentFile
import com.gamebox.os.importer.AuthorizedRomImporter
import com.gamebox.os.importer.RomImportResult
import com.gamebox.os.importer.RomImportSetResult
import com.gamebox.os.importer.RomImportSource
import com.gamebox.os.importer.RomImportPolicy
import com.gamebox.os.importer.RomImportProgress
import com.gamebox.os.importer.deleteImportedSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal data class DiscoveryImportOutcome(val message: String, val succeeded: Boolean)

/** Owns verified import, Library registration, rollback, and source cleanup for a discovery title. */
internal class DiscoveryImportWorkflow(
    private val context: Context,
    private val importer: AuthorizedRomImporter,
    private val repository: GameRepository,
    private val game: DiscoveryGame,
    private val platformName: String,
) {
    fun displayName(uri: Uri): String = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull() ?: uri.lastPathSegment ?: "game.rom"

    suspend fun importSingle(
        uri: Uri,
        displayName: String,
        removeSourceAfterSuccess: Boolean,
        onProgress: (RomImportProgress) -> Unit = {},
    ): DiscoveryImportOutcome = GameMutationGate.withGameLock(game.id.value) {
        val resolvedPlatform = RomImportPolicy.resolvedPlatform(platformName, displayName)
        when (val result = importer.import(game.id, uri, displayName, platformName, onProgress)) {
            is RomImportResult.Imported -> completeRegistration(
                subject = "imported files",
                rollback = { importer.rollbackRegistration(game.id, result.transactionId) },
            ) {
                val importedFile = LocalContentFile(
                    relativePath = RomImportPolicy.importRootRelativePath(game.id, result.relativePath),
                    sha256 = result.hashes.sha256,
                    mimeType = result.mimeType,
                )
                repository.registerImportedGame(
                    importedGameRegistration(
                        game = game,
                        platform = resolvedPlatform,
                        launchFile = importedFile,
                        files = listOf(importedFile),
                        sizeBytes = result.hashes.sizeBytes,
                    ),
                )
                val cleanupDeferred = runCatching {
                    importer.confirmRegistration(game.id, result.transactionId)
                }.isFailure
                val sourceRemoved = removeSourceAfterSuccess && deleteImportedSource(context, uri)
                singleImportSuccessMessage(
                    platform = resolvedPlatform,
                    hash = result.hashes.sha256,
                    cleanupDeferred = cleanupDeferred,
                    sourceRemoved = sourceRemoved,
                    sourceRemovalRequested = removeSourceAfterSuccess,
                )
            }
            RomImportResult.SourceUnavailable -> DiscoveryImportOutcome(
                "The selected file is unavailable or its access expired. Choose it again.",
                succeeded = false,
            )
            is RomImportResult.Rejected -> DiscoveryImportOutcome("Import rejected: ${result.reason}", succeeded = false)
            is RomImportResult.Failed -> DiscoveryImportOutcome("Import failed: ${result.reason}", succeeded = false)
        }
    }

    suspend fun importDiscSet(
        sources: List<RomImportSource>,
        onProgress: (RomImportProgress) -> Unit = {},
    ): String =
        GameMutationGate.withGameLock(game.id.value) {
            when (val result = importer.importSet(game.id, sources, platformName, onProgress = onProgress)) {
                is RomImportSetResult.Imported -> completeRegistration(
                    subject = "disc-set files",
                    rollback = { importer.rollbackRegistration(game.id, result.transactionId) },
                ) {
                    val resolvedPlatform = RomImportPolicy.resolvedPlatform(
                        platformName,
                        result.launchFile.relativePath.substringAfterLast('/'),
                    )
                    val importedFiles = result.files.map { file ->
                        LocalContentFile(
                            relativePath = RomImportPolicy.importRootRelativePath(game.id, file.relativePath),
                            sha256 = file.hashes.sha256,
                            mimeType = file.mimeType,
                        )
                    }
                    val launchPath = RomImportPolicy.importRootRelativePath(
                        game.id,
                        result.launchFile.relativePath,
                    )
                    val launchFile = requireNotNull(importedFiles.firstOrNull {
                        it.relativePath == launchPath
                    })
                    repository.registerImportedGame(
                        importedGameRegistration(
                            game = game,
                            platform = resolvedPlatform,
                            launchFile = launchFile,
                            files = importedFiles,
                            sizeBytes = result.files.sumOf { it.hashes.sizeBytes },
                        ),
                    )
                    val cleanupDeferred = runCatching {
                        importer.confirmRegistration(game.id, result.transactionId)
                    }.isFailure
                    val sourcesRemoved = sources.count { deleteImportedSource(context, it.uri) }
                    buildString {
                        append("${result.files.size}-file disc set verified and added to Library; ")
                        append("$sourcesRemoved source archive/file(s) removed")
                        if (cleanupDeferred) append(". Transaction cleanup will finish after restart.")
                    }
                }.message
                RomImportSetResult.SourceUnavailable -> "A selected file is unavailable or its access expired. Choose the files again."
                is RomImportSetResult.Rejected -> "Import rejected: ${result.reason}"
                is RomImportSetResult.Failed -> "Import failed: ${result.reason}"
            }
        }

    private suspend fun completeRegistration(
        subject: String,
        rollback: suspend () -> Unit,
        operation: suspend () -> String,
    ): DiscoveryImportOutcome = try {
        DiscoveryImportOutcome(operation(), succeeded = true)
    } catch (cancelled: CancellationException) {
        withContext(NonCancellable) { runCatching { rollback() } }
        throw cancelled
    } catch (error: Exception) {
        registrationFailureMessage(subject, error, rollback)
    }

    private suspend fun registrationFailureMessage(
        subject: String,
        error: Throwable,
        rollback: suspend () -> Unit,
    ): DiscoveryImportOutcome {
        val rolledBack = withContext(NonCancellable) { runCatching { rollback() }.isSuccess }
        val detail = error.message?.take(140) ?: "unknown error"
        val message = if (rolledBack) {
            "Library registration failed; $subject were rolled back safely: $detail"
        } else {
            "Library registration failed and recovery is pending; restart GameBox: $detail"
        }
        return DiscoveryImportOutcome(message, succeeded = false)
    }

    private fun singleImportSuccessMessage(
        platform: String,
        hash: String,
        cleanupDeferred: Boolean,
        sourceRemoved: Boolean,
        sourceRemovalRequested: Boolean,
    ): String = when {
        cleanupDeferred && sourceRemoved ->
            "$platform download verified, moved to Library, and source removed. Transaction cleanup will finish after restart."
        cleanupDeferred ->
            "$platform copy verified and added to Library. The browser source was retained because Android denied deletion."
        sourceRemoved ->
            "$platform download verified, moved to Library, and source removed. SHA-256 ${hash.take(12)}…"
        sourceRemovalRequested ->
            "$platform copy verified and added to Library, but Android denied deletion of the browser source. SHA-256 ${hash.take(12)}…"
        else -> "$platform copy verified and added to Library. SHA-256 ${hash.take(12)}…"
    }
}

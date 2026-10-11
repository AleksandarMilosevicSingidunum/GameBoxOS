package com.gamebox.os.importer

import android.content.Context
import android.net.Uri
import com.gamebox.os.domain.GameId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class RomImportSource(val uri: Uri, val displayName: String)

data class ImportedRomFile(
    val relativePath: String,
    val hashes: RomHashes,
    val mimeType: String,
)

sealed interface RomImportSetResult {
    data class Imported(
        val launchFile: ImportedRomFile,
        val files: List<ImportedRomFile>,
        val transactionId: String,
    ) : RomImportSetResult
    data object SourceUnavailable : RomImportSetResult
    data class Rejected(val reason: String) : RomImportSetResult
    data class Failed(val reason: String) : RomImportSetResult
}

sealed interface RomImportResult {
    data class Imported(
        val relativePath: String,
        val hashes: RomHashes,
        val transactionId: String,
        val mimeType: String = "application/octet-stream",
    ) : RomImportResult
    data object SourceUnavailable : RomImportResult
    data class Rejected(val reason: String) : RomImportResult
    data class Failed(val reason: String) : RomImportResult
}

private val importTransactionMutex = Mutex()

class AuthorizedRomImporter(
    context: Context,
    private val maxBytes: Long = 64L * 1024 * 1024 * 1024,
) {
    private val applicationContext = context.applicationContext
    private val transactionRecovery = ImportTransactionRecovery(applicationContext.filesDir)
    private val registrationJournal = ImportRegistrationJournal(applicationContext.filesDir)
    private val registrationRecovery = ImportRegistrationRecovery(registrationJournal)
    @Volatile private var startupRecoveryFailed = false

    init {
        require(maxBytes > 0)
        startupRecoveryFailed = transactionRecovery.recover().failures > 0
    }

    private fun recoverBeforeMutation(gameId: GameId): String? {
        val report = transactionRecovery.recover()
        startupRecoveryFailed = report.failures > 0
        if (startupRecoveryFailed) {
            return "Interrupted import recovery could not complete; check app storage before retrying"
        }
        val pending = runCatching { registrationJournal.pendingFor(gameId) }.getOrElse {
            return "Pending import registration recovery could not be read; restart GameBox and retry"
        }
        return if (pending.isNotEmpty()) {
            "A previous import for this game is still being reconciled; restart GameBox or retry shortly"
        } else null
    }

    suspend fun import(
        gameId: GameId,
        source: Uri,
        displayName: String,
        platform: String? = null,
        onProgress: (RomImportProgress) -> Unit = {},
    ): RomImportResult {
        return when (
            val result = importSet(
                gameId = gameId,
                sources = listOf(RomImportSource(source, displayName)),
                platform = platform,
                onProgress = onProgress,
            )
        ) {
            is RomImportSetResult.Imported -> RomImportResult.Imported(
                relativePath = result.launchFile.relativePath,
                hashes = result.launchFile.hashes,
                transactionId = result.transactionId,
                mimeType = result.launchFile.mimeType,
            )
            RomImportSetResult.SourceUnavailable -> RomImportResult.SourceUnavailable
            is RomImportSetResult.Rejected -> RomImportResult.Rejected(result.reason)
            is RomImportSetResult.Failed -> RomImportResult.Failed(result.reason)
        }
    }

    suspend fun importSet(
        gameId: GameId,
        sources: List<RomImportSource>,
        platform: String? = null,
        expectedFiles: List<com.gamebox.os.domain.LocalContentFile>? = null,
        onProgress: (RomImportProgress) -> Unit = {},
    ): RomImportSetResult = importTransactionMutex.withLock {
        withContext(Dispatchers.IO) {
        val importCoroutineContext = currentCoroutineContext()
        importCoroutineContext.ensureActive()
        recoverBeforeMutation(gameId)?.let { return@withContext RomImportSetResult.Failed(it) }
        if (sources.isEmpty()) return@withContext RomImportSetResult.Rejected("Select at least one game file")
        if (sources.size > MAX_DISC_SET_FILES) {
            return@withContext RomImportSetResult.Rejected("A disc set may contain at most $MAX_DISC_SET_FILES files")
        }
        val safeNames = runCatching {
            sources.map { RomImportPolicy.safeFileName(it.displayName, platform) }
        }.getOrElse { return@withContext RomImportSetResult.Rejected(it.message ?: "Invalid game file") }
        if (safeNames.map(String::lowercase).distinct().size != safeNames.size) {
            return@withContext RomImportSetResult.Rejected("The selected disc set contains duplicate filenames")
        }
        val transactionId = UUID.randomUUID().toString()
        val workspace = try {
            ImportTransactionWorkspace.create(
                filesDirectory = applicationContext.filesDir,
                gameId = gameId,
                platform = platform,
                transactionId = transactionId,
            )
        } catch (error: IllegalArgumentException) {
            return@withContext RomImportSetResult.Rejected(error.message ?: "Invalid import path")
        } catch (error: Exception) {
            return@withContext RomImportSetResult.Failed(
                error.message?.take(200) ?: "Import storage is unavailable",
            )
        }
        // The promoted directory matches the recorded imports/<console>/<id>
        // path so launch, re-import, transfer, and removal share one location.
        val expectedPrefix = workspace.expectedRelativePrefix
        val targetDirectory = workspace.targetDirectory
        val staging = workspace.stagingDirectory
        val backup = workspace.backupDirectory
        var pendingRegistration: PendingImportTransaction? = null
        try {
            workspace.prepareDirectories()
            val sourceSizes = sources.map { sourceSizeBytes(it.uri) }
            importCoroutineContext.ensureActive()
            val progressReporter = RomImportProgressReporter(
                coroutineContext = importCoroutineContext,
                observer = onProgress,
            )
            val sourceStager = RomImportSourceStager(
                maxBytes = maxBytes,
                progress = progressReporter,
            )
            var totalBytes = 0L
            val stagedFiles = mutableListOf<ImportedRomFile>()
            sources.zip(safeNames).forEachIndexed { index, (source, safeName) ->
                importCoroutineContext.ensureActive()
                val importedFiles = sourceStager.stage(
                    openInputStream = { applicationContext.contentResolver.openInputStream(source.uri) },
                    safeName = safeName,
                    sourceSize = sourceSizes[index],
                    sourceIndex = index + 1,
                    sourceCount = sources.size,
                    stagedFileCount = stagedFiles.size,
                    bytesAlreadyStaged = totalBytes,
                    platform = platform,
                    staging = staging,
                    expectedPrefix = expectedPrefix,
                ) ?: return@withContext RomImportSetResult.SourceUnavailable
                stagedFiles += importedFiles
                totalBytes += importedFiles.sumOf { it.hashes.sizeBytes }
                require(totalBytes <= maxBytes) { "Disc set exceeds the configured import limit" }
            }
            val stagedNames = stagedFiles.map { it.relativePath.substringAfterLast('/') }
            val launchName = DiscSetPolicy.selectLaunchFile(staging, stagedNames)
            val launchFile = requireNotNull(stagedFiles.firstOrNull {
                it.relativePath.substringAfterLast('/').equals(launchName, ignoreCase = true)
            })
            expectedFiles?.let { expected ->
                verifyReimportIdentity(expected, stagedFiles.map {
                    com.gamebox.os.domain.LocalContentFile(
                        RomImportPolicy.importRootRelativePath(gameId, it.relativePath),
                        it.hashes.sha256, it.mimeType)
                })
            }
            pendingRegistration = registrationJournal.prepare(
                gameId = gameId,
                transactionId = transactionId,
                hadExistingTarget = targetDirectory.exists(),
                files = stagedFiles.map {
                    PendingImportFile(
                        relativePath = RomImportPolicy.importRootRelativePath(gameId, it.relativePath),
                        sha256 = it.hashes.sha256,
                    )
                },
            )
            ImportDirectoryPublisher.replaceAtomically(targetDirectory, staging, backup)
            val ordered = listOf(launchFile) + stagedFiles.filterNot { it === launchFile }
            RomImportSetResult.Imported(launchFile, ordered, transactionId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val rollbackFailure = pendingRegistration?.let { pending ->
                runCatching { registrationJournal.rollback(pending) }.exceptionOrNull()
            }
            if (rollbackFailure != null) {
                RomImportSetResult.Failed("Import failed and rollback could not complete; restart GameBox before retrying")
            } else if (error is IllegalArgumentException) {
                RomImportSetResult.Rejected(error.message ?: "Disc set rejected")
            } else {
                RomImportSetResult.Failed(error.message?.take(200) ?: "Disc set import failed")
            }
        } finally {
            if (pendingRegistration == null && staging.exists()) staging.deleteRecursively()
        }
        }
    }

    private fun sourceSizeBytes(uri: Uri): Long? = runCatching {
        applicationContext.contentResolver.openAssetFileDescriptor(uri, "r")
            ?.use { descriptor -> descriptor.length.takeIf { it > 0L } }
    }.getOrNull()

    /** Completes the Room/filesystem commit without hashing game files on the caller dispatcher. */
    suspend fun confirmRegistration(gameId: GameId, transactionId: String) {
        withContext(NonCancellable + Dispatchers.IO) {
            importTransactionMutex.withLock {
                val pending = registrationJournal.pendingFor(gameId)
                    .singleOrNull { it.transactionId == transactionId }
                    ?: throw IllegalStateException("Pending import registration was not found")
                require(registrationJournal.targetMatchesPending(pending)) {
                    "Imported content changed before registration cleanup"
                }
                registrationJournal.confirm(pending)
            }
        }
    }

    /** Finishes rollback even when its caller is cancelling, preserving a consistent file set. */
    suspend fun rollbackRegistration(gameId: GameId, transactionId: String) {
        withContext(NonCancellable + Dispatchers.IO) {
            importTransactionMutex.withLock {
                val pending = registrationJournal.pendingFor(gameId)
                    .singleOrNull { it.transactionId == transactionId }
                    ?: throw IllegalStateException("Pending import registration was not found")
                registrationJournal.rollback(pending)
            }
        }
    }

    suspend fun reconcilePendingRegistrations(
        createdBeforeMillis: Long = Long.MAX_VALUE,
        gameLookup: suspend (GameId) -> com.gamebox.os.domain.Game?,
    ): ImportRegistrationRecoveryReport =
        importTransactionMutex.withLock {
            registrationRecovery.reconcile(createdBeforeMillis, gameLookup)
        }

}


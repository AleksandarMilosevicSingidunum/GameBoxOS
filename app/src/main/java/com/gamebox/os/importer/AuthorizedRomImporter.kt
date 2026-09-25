package com.gamebox.os.importer

import android.content.Context
import android.net.Uri
import com.gamebox.os.domain.GameId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
    ): RomImportResult {
        return when (
            val result = importSet(
                gameId = gameId,
                sources = listOf(RomImportSource(source, displayName)),
                platform = platform,
            )
        ) {
            is RomImportSetResult.Imported -> RomImportResult.Imported(
                relativePath = result.launchFile.relativePath,
                hashes = result.launchFile.hashes,
                transactionId = result.transactionId,
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
    ): RomImportSetResult = importTransactionMutex.withLock {
        withContext(Dispatchers.IO) {
        recoverBeforeMutation(gameId)?.let { return@withContext RomImportSetResult.Failed(it) }
        if (sources.isEmpty()) return@withContext RomImportSetResult.Rejected("Select at least one game file")
        if (sources.size > 64) return@withContext RomImportSetResult.Rejected("A disc set may contain at most 64 files")
        val safeNames = runCatching {
            sources.map { RomImportPolicy.safeFileName(it.displayName, platform) }
        }.getOrElse { return@withContext RomImportSetResult.Rejected(it.message ?: "Invalid game file") }
        if (safeNames.map(String::lowercase).distinct().size != safeNames.size) {
            return@withContext RomImportSetResult.Rejected("The selected disc set contains duplicate filenames")
        }
        val id = gameId.value
        if (!id.matches(Regex("[a-z0-9][a-z0-9-]{0,95}"))) {
            return@withContext RomImportSetResult.Rejected("Invalid game id")
        }
        val expectedPrefix = "imports/$id/"
        val root = applicationContext.filesDir.resolve("imports").canonicalFile
        val targetDirectory = root.resolve(id).canonicalFile
        val rootPrefix = root.path + File.separator
        if (!targetDirectory.path.startsWith(rootPrefix)) {
            return@withContext RomImportSetResult.Rejected("Import path escaped app storage")
        }
        root.mkdirs()
        val transactionId = UUID.randomUUID().toString()
        val staging = root.resolve(".staging-$id-$transactionId").canonicalFile
        val backup = root.resolve(".backup-$id-$transactionId").canonicalFile
        if (!staging.path.startsWith(rootPrefix) || !backup.path.startsWith(rootPrefix)) {
            return@withContext RomImportSetResult.Rejected("Import transaction path escaped app storage")
        }
        staging.mkdirs()
        var pendingRegistration: PendingImportTransaction? = null
        try {
            var totalBytes = 0L
            val stagedFiles = mutableListOf<ImportedRomFile>()
            sources.zip(safeNames).forEach { (source, safeName) ->
                val input = applicationContext.contentResolver.openInputStream(source.uri)
                    ?: return@withContext RomImportSetResult.SourceUnavailable
                val stagedFile = staging.resolve(safeName)
                val remaining = (maxBytes - totalBytes).coerceAtLeast(1L)
                val hashes = input.use { stream ->
                    stagedFile.outputStream().buffered().use { output ->
                        RomHasher.hash(stream, remaining) { buffer, count ->
                            output.write(buffer, 0, count)
                        }
                    }
                }
                totalBytes += hashes.sizeBytes
                require(totalBytes <= maxBytes) { "Disc set exceeds the configured import limit" }
                stagedFiles += ImportedRomFile(
                    relativePath = expectedPrefix + safeName,
                    hashes = hashes,
                    mimeType = RomImportPolicy.mimeType(safeName),
                )
            }
            val launchName = DiscSetPolicy.selectLaunchFile(staging, safeNames)
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
            replaceDirectoryAtomically(targetDirectory, staging, backup)
            val ordered = listOf(launchFile) + stagedFiles.filterNot { it === launchFile }
            RomImportSetResult.Imported(launchFile, ordered, transactionId)
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

    fun confirmRegistration(gameId: GameId, transactionId: String) {
        val pending = registrationJournal.pendingFor(gameId)
            .singleOrNull { it.transactionId == transactionId }
            ?: throw IllegalStateException("Pending import registration was not found")
        registrationJournal.confirm(pending)
    }

    fun rollbackRegistration(gameId: GameId, transactionId: String) {
        val pending = registrationJournal.pendingFor(gameId)
            .singleOrNull { it.transactionId == transactionId }
            ?: throw IllegalStateException("Pending import registration was not found")
        registrationJournal.rollback(pending)
    }

    suspend fun reconcilePendingRegistrations(
        createdBeforeMillis: Long = Long.MAX_VALUE,
        gameLookup: suspend (GameId) -> com.gamebox.os.domain.Game?,
    ): ImportRegistrationRecoveryReport =
        importTransactionMutex.withLock {
            registrationJournal.reconcile(createdBeforeMillis, gameLookup)
        }

    private fun replaceDirectoryAtomically(target: File, staging: File, backup: File) {
        var previousMoved = false
        try {
            if (target.exists()) {
                moveReplacing(target, backup)
                previousMoved = true
            }
            moveReplacing(staging, target)
        } catch (error: Exception) {
            if (previousMoved && backup.exists()) {
                if (target.exists()) target.deleteRecursively()
                runCatching { moveReplacing(backup, target) }
            }
            throw error
        }
    }

    private fun moveReplacing(source: File, target: File) {
        runCatching {
            Files.move(
                source.toPath(), target.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING,
            )
        }.getOrElse {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

}


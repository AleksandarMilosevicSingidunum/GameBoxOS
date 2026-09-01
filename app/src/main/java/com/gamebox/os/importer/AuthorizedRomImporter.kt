package com.gamebox.os.importer

import android.content.Context
import android.net.Uri
import com.gamebox.os.domain.GameId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    ) : RomImportSetResult
    data object SourceUnavailable : RomImportSetResult
    data class Rejected(val reason: String) : RomImportSetResult
    data class Failed(val reason: String) : RomImportSetResult
}

sealed interface RomImportResult {
    data class Imported(
        val relativePath: String,
        val hashes: RomHashes,
    ) : RomImportResult
    data object SourceUnavailable : RomImportResult
    data class Rejected(val reason: String) : RomImportResult
    data class Failed(val reason: String) : RomImportResult
}

class AuthorizedRomImporter(
    context: Context,
    private val maxBytes: Long = 64L * 1024 * 1024 * 1024,
) {
    private val applicationContext = context.applicationContext

    init {
        require(maxBytes > 0)
    }

    suspend fun import(
        gameId: GameId,
        source: Uri,
        displayName: String,
        platform: String? = null,
    ): RomImportResult = withContext(Dispatchers.IO) {
        val relativePath = runCatching {
            RomImportPolicy.relativePath(gameId, displayName, platform)
        }.getOrElse { return@withContext RomImportResult.Rejected(it.message ?: "Invalid game file") }
        val root = applicationContext.filesDir.resolve("imports").canonicalFile
        val target = File(applicationContext.filesDir, relativePath).canonicalFile
        val rootPrefix = root.path + File.separator
        if (!target.path.startsWith(rootPrefix)) {
            return@withContext RomImportResult.Rejected("Import path escaped app storage")
        }
        val input = applicationContext.contentResolver.openInputStream(source)
            ?: return@withContext RomImportResult.SourceUnavailable
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, target.name + ".partial")
        partial.delete()
        runCatching {
            val hashes = input.use { stream ->
                partial.outputStream().buffered().use { output ->
                    RomHasher.hash(stream, maxBytes) { buffer, count ->
                        output.write(buffer, 0, count)
                    }
                }
            }
            runCatching {
                Files.move(
                    partial.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }.getOrElse {
                Files.move(
                    partial.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
            RomImportResult.Imported(relativePath, hashes)
        }.getOrElse { error ->
            partial.delete()
            if (error is IllegalArgumentException) {
                RomImportResult.Rejected(error.message ?: "Import rejected")
            } else {
                RomImportResult.Failed(error.message?.take(200) ?: "Import failed")
            }
        }
    }

    suspend fun importSet(
        gameId: GameId,
        sources: List<RomImportSource>,
        platform: String? = null,
    ): RomImportSetResult = withContext(Dispatchers.IO) {
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
            replaceDirectoryAtomically(targetDirectory, staging, backup)
            val ordered = listOf(launchFile) + stagedFiles.filterNot { it === launchFile }
            RomImportSetResult.Imported(launchFile, ordered)
        } catch (error: Exception) {
            if (error is IllegalArgumentException) {
                RomImportSetResult.Rejected(error.message ?: "Disc set rejected")
            } else {
                RomImportSetResult.Failed(error.message?.take(200) ?: "Disc set import failed")
            }
        } finally {
            if (staging.exists()) staging.deleteRecursively()
            if (backup.exists() && targetDirectory.exists()) backup.deleteRecursively()
        }
    }

    private fun replaceDirectoryAtomically(target: File, staging: File, backup: File) {
        var previousMoved = false
        try {
            if (target.exists()) {
                moveReplacing(target, backup)
                previousMoved = true
            }
            moveReplacing(staging, target)
            if (backup.exists()) backup.deleteRecursively()
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

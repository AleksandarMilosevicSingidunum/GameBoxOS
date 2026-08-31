package com.gamebox.os.importer

import android.content.Context
import android.net.Uri
import com.gamebox.os.domain.GameId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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
    ): RomImportResult = withContext(Dispatchers.IO) {
        val relativePath = runCatching {
            RomImportPolicy.relativePath(gameId, displayName)
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
}

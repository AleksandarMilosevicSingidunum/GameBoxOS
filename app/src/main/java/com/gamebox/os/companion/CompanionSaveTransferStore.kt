package com.gamebox.os.companion

import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.GameId
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

data class CompanionSavePayload(
    val gameId: String,
    val updatedAtMillis: Long,
    val sha256: String,
    val bytes: ByteArray,
)

data class CompanionSaveImportResult(
    val gameId: String,
    val updatedAtMillis: Long,
    val sha256: String,
    val conflictPreserved: Boolean,
)

class CompanionSaveTransferStore(
    filesDirectory: File,
    private val games: GameRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val savesRoot = filesDirectory.resolve("saves").canonicalFile
    private val conflictsRoot = filesDirectory.resolve("save-conflicts").canonicalFile

    fun export(gameId: String): CompanionSavePayload? {
        requireGameId(gameId)
        requireNotNull(games.game(GameId(gameId))) { "Game is not in the library" }
        val save = saveFile(gameId)
        if (!save.isFile) return null
        require(save.length() in 1..MAX_SAVE_BYTES) { "Managed save exceeds the companion limit" }
        val bytes = save.readBytes()
        return CompanionSavePayload(gameId, save.lastModified().coerceAtLeast(0L), sha256(bytes), bytes)
    }

    suspend fun import(gameId: String, bytes: ByteArray): CompanionSaveImportResult {
        requireGameId(gameId)
        requireNotNull(games.game(GameId(gameId))) { "Game is not in the library" }
        require(bytes.size in 1..MAX_SAVE_BYTES.toInt()) { "Save must contain 1 byte to 16 MiB" }
        val destination = saveFile(gameId)
        check(destination.parentFile.mkdirs() || destination.parentFile.isDirectory)
        val incomingHash = sha256(bytes)
        val conflictPreserved = destination.takeIf(File::isFile)?.let { existing ->
            if (existing.length() > MAX_SAVE_BYTES) throw IllegalArgumentException("Existing save exceeds the companion limit")
            val previous = existing.readBytes()
            if (sha256(previous) == incomingHash) false else {
                preserveConflict(gameId, previous)
                true
            }
        } ?: false
        val partial = destination.parentFile.resolve(destination.name + ".companion-partial")
        try {
            partial.writeBytes(bytes)
            check(partial.length() == bytes.size.toLong() && sha256(partial.readBytes()) == incomingHash) {
                "Staged save verification failed"
            }
            atomicReplace(partial, destination)
        } finally {
            partial.delete()
        }
        val updated = nowMillis().coerceAtLeast(0L)
        destination.setLastModified(updated)
        games.registerManagedSave(GameId(gameId), gameId + "/save.dat", updated, bytes.size.toLong())
        return CompanionSaveImportResult(gameId, updated, incomingHash, conflictPreserved)
    }

    private fun preserveConflict(gameId: String, bytes: ByteArray) {
        val directory = conflictsRoot.resolve(gameId).canonicalFile
        require(directory.path.startsWith(conflictsRoot.path + File.separator))
        check(directory.mkdirs() || directory.isDirectory)
        val destination = directory.resolve("windows-" + nowMillis().coerceAtLeast(0L) + ".save")
        val partial = directory.resolve(destination.name + ".partial")
        try {
            partial.writeBytes(bytes)
            atomicReplace(partial, destination)
        } finally {
            partial.delete()
        }
    }

    private fun saveFile(gameId: String): File {
        val result = savesRoot.resolve(gameId + "/save.dat").canonicalFile
        require(result.path.startsWith(savesRoot.path + File.separator)) { "Save path escapes storage" }
        return result
    }

    private fun atomicReplace(source: File, destination: File) {
        runCatching {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }.getOrElse {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        const val MAX_SAVE_BYTES = 16L * 1024L * 1024L
        fun requireGameId(gameId: String) {
            require(gameId.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,95}"))) { "Invalid game ID" }
        }
        fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}

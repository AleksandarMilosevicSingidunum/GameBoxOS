package com.gamebox.os.storage

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64

data class SaveSnapshotManifest(
    val gameId: String,
    val createdAtMillis: Long,
    val relativePaths: List<String>,
)

class SaveSnapshotManifestStore(directory: File, private val maxPaths: Int = 1_000) {
    private val root = directory.canonicalFile

    init {
        require(maxPaths > 0) { "maxPaths must be positive" }
        root.mkdirs()
        require(root.isDirectory) { "Snapshot manifest directory is unavailable" }
    }

    fun save(manifest: SaveSnapshotManifest) {
        validateGameId(manifest.gameId)
        require(manifest.createdAtMillis >= 0) { "Snapshot timestamp cannot be negative" }
        val paths = manifest.relativePaths.distinct()
        require(paths.size <= maxPaths) { "Snapshot contains too many paths" }
        paths.forEach(::validateRelativePath)

        val destination = fileFor(manifest.gameId)
        val staged = File(root, destination.name + ".part")
        val encoder = Base64.getUrlEncoder().withoutPadding()
        staged.bufferedWriter().use { writer ->
            writer.appendLine("GAMEBOX_SAVE_MANIFEST_1")
            writer.appendLine(manifest.createdAtMillis.toString())
            paths.forEach { path ->
                writer.appendLine(encoder.encodeToString(path.toByteArray(Charsets.UTF_8)))
            }
        }
        promote(staged, destination)
    }

    fun load(gameId: String): SaveSnapshotManifest? {
        validateGameId(gameId)
        val source = fileFor(gameId)
        if (!source.isFile) return null
        val lines = source.readLines()
        require(lines.size >= 2 && lines.first() == "GAMEBOX_SAVE_MANIFEST_1") { "Invalid snapshot manifest" }
        val createdAt = lines[1].toLongOrNull() ?: throw IllegalArgumentException("Invalid snapshot timestamp")
        val decoder = Base64.getUrlDecoder()
        val paths = lines.drop(2).map { encoded ->
            String(decoder.decode(encoded), Charsets.UTF_8).also(::validateRelativePath)
        }
        require(paths.size <= maxPaths) { "Snapshot contains too many paths" }
        return SaveSnapshotManifest(gameId, createdAt, paths.distinct())
    }

    private fun fileFor(gameId: String): File = File(root, gameId + ".manifest")

    private fun validateGameId(gameId: String) {
        require(gameId.matches(Regex("[A-Za-z0-9._-]+"))) { "gameId contains unsupported characters" }
    }

    private fun validateRelativePath(path: String) {
        require(path.isNotBlank() && !File(path).isAbsolute) { "Snapshot path must be relative" }
        require(path.split('/', '\\').none { it == ".." }) { "Snapshot path contains traversal" }
    }

    private fun promote(staged: File, destination: File) {
        try {
            Files.move(staged.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(staged.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

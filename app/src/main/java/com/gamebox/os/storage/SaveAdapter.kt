package com.gamebox.os.storage

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class SaveDiscoveryLimitExceededException(
    val limit: Int
) : IllegalStateException("Save discovery exceeded the $limit artifact limit")

data class SaveArtifact(
    val gameId: String,
    val relativePath: String,
    val sizeBytes: Long,
    val modifiedAtMillis: Long,
)

fun interface SaveAdapter {
    fun discover(gameId: String): List<SaveArtifact>
}

/** A save adapter that can move emulator-owned bytes in both directions. */
interface BidirectionalSaveAdapter : SaveAdapter {
    fun openArtifact(artifact: SaveArtifact): InputStream
    fun restoreArtifact(artifact: SaveArtifact, source: InputStream)
}

class DirectorySaveAdapter(
    rootDirectory: File,
    private val maxArtifacts: Int = 1_000,
) : BidirectionalSaveAdapter {
    private val root = rootDirectory.canonicalFile

    init {
        require(maxArtifacts > 0) { "maxArtifacts must be positive" }
    }

    override fun discover(gameId: String): List<SaveArtifact> {
        require(gameId.matches(Regex("[A-Za-z0-9._-]+"))) { "gameId contains unsupported characters" }
        val requestedRoot = File(root, gameId)
        require(!Files.isSymbolicLink(requestedRoot.toPath())) { "Linked game save directories are not supported" }
        val gameRoot = requestedRoot.canonicalFile
        require(gameRoot.path.startsWith(root.path + File.separator)) { "game save path escapes configured root" }
        if (!gameRoot.isDirectory) return emptyList()

        val discovered = gameRoot.walkTopDown()
            .onEnter { directory ->
                !Files.isSymbolicLink(directory.toPath()) &&
                    (directory == gameRoot || directory.canonicalPath.startsWith(gameRoot.path + File.separator))
            }
            .onFail { _, error -> throw error }
            .filter {
                it.isFile && !Files.isSymbolicLink(it.toPath()) &&
                    it.canonicalPath.startsWith(gameRoot.path + File.separator) &&
                    !it.name.endsWith(".restore.part") &&
                    !(it.name.startsWith("save-import-") && it.name.endsWith(".part"))
            }
            .take(maxArtifacts + 1)
            .toList()
        if (discovered.size > maxArtifacts) throw SaveDiscoveryLimitExceededException(maxArtifacts)

        return discovered
            .asSequence()
            .map { file ->
                SaveArtifact(
                    gameId = gameId,
                    relativePath = file.relativeTo(root).invariantSeparatorsPath,
                    sizeBytes = file.length(),
                    modifiedAtMillis = file.lastModified(),
                )
            }
            .sortedBy { it.relativePath }
            .toList()
    }

    override fun openArtifact(artifact: SaveArtifact): InputStream {
        val file = resolveArtifact(artifact)
        require(file.isFile) { "Save artifact is unavailable" }
        return file.inputStream()
    }

    override fun restoreArtifact(artifact: SaveArtifact, source: InputStream) {
        val destination = resolveArtifact(artifact)
        check(destination.parentFile?.mkdirs() != false || destination.parentFile?.isDirectory == true)
        val staged = File(destination.parentFile, destination.name + ".restore.part")
        try {
            staged.outputStream().use { output -> source.copyTo(output) }
            Files.move(
                staged.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } finally {
            staged.delete()
        }
    }

    private fun resolveArtifact(artifact: SaveArtifact): File {
        require(artifact.gameId.matches(Regex("[A-Za-z0-9._-]+"))) {
            "gameId contains unsupported characters"
        }
        require(artifact.relativePath.isNotBlank() &&
            artifact.relativePath.split('/').none { it.isBlank() || it == "." || it == ".." } &&
            !artifact.relativePath.contains('\\') &&
            !artifact.relativePath.contains(':') &&
            !artifact.relativePath.contains('\u0000')) {
            "save artifact path is unsafe"
        }
        val file = File(root, artifact.relativePath).canonicalFile
        require(file.path.startsWith(root.path + File.separator)) {
            "save artifact path escapes configured root"
        }
        require(file.path.startsWith(File(root, artifact.gameId).canonicalPath + File.separator)) {
            "save artifact does not belong to its game"
        }
        require(!Files.isSymbolicLink(file.toPath())) { "Linked save artifacts are not supported" }
        return file
    }
}

package com.gamebox.os.storage

import java.io.File
import java.nio.file.Files

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

class DirectorySaveAdapter(
    rootDirectory: File,
    private val maxArtifacts: Int = 1_000,
) : SaveAdapter {
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
}

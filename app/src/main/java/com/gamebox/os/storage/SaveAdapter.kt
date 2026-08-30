package com.gamebox.os.storage

import java.io.File

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
        val gameRoot = File(root, gameId).canonicalFile
        require(gameRoot.path.startsWith(root.path + File.separator)) { "game save path escapes configured root" }
        if (!gameRoot.isDirectory) return emptyList()

        val discovered = gameRoot.walkTopDown()
            .filter { it.isFile && it.canonicalPath.startsWith(gameRoot.path + File.separator) }
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

package com.gamebox.os.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream
import java.security.MessageDigest

/** The SAF save-root convention is <selected-root>/<game-id>/<save files>. */
internal fun safSaveSegments(gameId: String, relativePath: String? = null): List<String> {
    require(gameId.matches(Regex("[A-Za-z0-9._-]+"))) {
        "gameId contains unsupported characters"
    }
    val path = relativePath ?: gameId
    require(path == gameId || path.startsWith(gameId + "/")) {
        "save artifact does not belong to its game"
    }
    val segments = path.split('/')
    require(segments.none { segment ->
        segment.isBlank() || segment == "." || segment == ".." ||
            segment.contains('\\') || segment.contains(':') || segment.contains('\u0000')
    }) { "save artifact path is unsafe" }
    return segments
}

/**
 * Bidirectional save access for an explicitly user-authorized SAF tree.
 *
 * This adapter never guesses an emulator's private directory. The user must select the
 * root that the configured emulator actually uses, and GameBox addresses only the
 * game-id child below that root.
 */
class SafSaveAdapter(
    context: Context,
    private val treeUri: () -> String,
    private val maxArtifacts: Int = 1_000,
) : BidirectionalSaveAdapter {
    private val applicationContext = context.applicationContext
    private val resolver = applicationContext.contentResolver

    init {
        require(maxArtifacts > 0) { "maxArtifacts must be positive" }
    }

    override fun discover(gameId: String): List<SaveArtifact> {
        val gameRoot = resolveDirectory(safSaveSegments(gameId)) ?: return emptyList()
        val discovered = mutableListOf<SaveArtifact>()
        walk(gameRoot, gameId, gameId, discovered)
        if (discovered.size > maxArtifacts) {
            throw SaveDiscoveryLimitExceededException(maxArtifacts)
        }
        return discovered.sortedBy { it.relativePath }
    }

    override fun openArtifact(artifact: SaveArtifact): InputStream {
        val file = resolveFile(safSaveSegments(artifact.gameId, artifact.relativePath))
        return requireNotNull(resolver.openInputStream(file.uri)) {
            "SAF save artifact could not be opened"
        }
    }

    override fun restoreArtifact(artifact: SaveArtifact, source: InputStream) {
        val segments = safSaveSegments(artifact.gameId, artifact.relativePath)
        require(segments.size > 1) { "SAF save artifact must name a file" }
        val parent = resolveDirectory(segments.dropLast(1))
            ?: throw IllegalStateException("SAF save destination is unavailable")
        val fileName = segments.last()
        val partialName = fileName + ".gamebox-restore.part"
        val partial = parent.findFile(partialName) ?: parent.createFile(
            "application/octet-stream",
            partialName,
        ) ?: throw IllegalStateException("SAF save destination could not be created")
        try {
            val sourceDigest = copyAndDigest(source, partial)
            val destination = parent.findFile(fileName)
            val destinationDigest = digest(requireNotNull(resolver.openInputStream(partial.uri)))
            check(sourceDigest.contentEquals(destinationDigest)) { "SAF save destination verification failed" }
            destination?.delete()
            check(partial.renameTo(fileName)) { "SAF save destination could not be finalized" }
        } finally {
            parent.findFile(partialName)?.delete()
        }
    }

    private fun walk(
        directory: DocumentFile,
        gameId: String,
        relativeDirectory: String,
        output: MutableList<SaveArtifact>,
    ) {
        directory.listFiles().sortedBy { it.name.orEmpty() }.forEach { child ->
            val name = child.name ?: return@forEach
            require(name.isNotBlank() && !name.contains('/') && !name.contains('\\')) {
                "SAF save entry has an unsafe name"
            }
            val relativePath = "$relativeDirectory/$name"
            if (child.isDirectory) {
                walk(child, gameId, relativePath, output)
            } else if (child.isFile &&
                !name.endsWith(".gamebox-restore.part") &&
                !name.endsWith(".restore.part")
            ) {
                output += SaveArtifact(
                    gameId = gameId,
                    relativePath = relativePath,
                    sizeBytes = child.length().coerceAtLeast(0L),
                    modifiedAtMillis = child.lastModified().coerceAtLeast(0L),
                )
            }
            if (output.size > maxArtifacts) return
        }
    }

    private fun resolveDirectory(segments: List<String>): DocumentFile? {
        val root = rootDocument() ?: return null
        var current = root
        segments.forEach { segment ->
            current = current.findFile(segment)?.takeIf { it.isDirectory } ?: return null
        }
        return current.takeIf { it.exists() && it.isDirectory && it.canRead() }
    }

    private fun resolveFile(segments: List<String>): DocumentFile {
        require(segments.size > 1) { "SAF save artifact must name a file" }
        val parent = resolveDirectory(segments.dropLast(1))
            ?: throw IllegalStateException("SAF save artifact is unavailable")
        return parent.findFile(segments.last())?.takeIf { it.isFile && it.canRead() }
            ?: throw IllegalStateException("SAF save artifact is unavailable")
    }

    private fun rootDocument(): DocumentFile? {
        val configured = treeUri().takeIf(String::isNotBlank) ?: return null
        val uri = runCatching { Uri.parse(configured) }.getOrNull() ?: return null
        return DocumentFile.fromTreeUri(applicationContext, uri)
            ?.takeIf { it.exists() && it.isDirectory && it.canRead() }
    }

    private fun copyAndDigest(source: InputStream, destination: DocumentFile): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        resolver.openOutputStream(destination.uri, "w")?.use { output ->
            source.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    digest.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                }
            }
        } ?: throw IllegalStateException("SAF save destination could not be opened")
        return digest.digest()
    }

    private fun digest(source: InputStream): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        source.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest()
    }
}

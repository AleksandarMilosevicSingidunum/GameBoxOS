package com.gamebox.os.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream

internal fun externalLibrarySegments(gameId: String, relativePath: String): List<String> {
    require(gameId.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,95}"))) {
        "Invalid game ID"
    }
    require(
        relativePath.isNotBlank() &&
            relativePath.none { it == '\\' || it == ':' || it == '\u0000' } &&
            relativePath.split('/').none { it.isBlank() || it == "." || it == ".." }
    ) { "Unsafe external content path" }
    return listOf(gameId) + relativePath.split('/')
}

data class ExternalGameContent(
    val uri: Uri,
    val sizeBytes: Long,
)

/**
 * Resolves only game-owned files below the persisted external library tree.
 *
 * Migrated installed content is stored as:
 *   <tree>/<gameId>/<installed-root-relative-path>
 */
class ExternalLibraryContentStore(
    context: Context,
    private val treeUri: () -> String,
) {
    private val applicationContext = context.applicationContext
    private val resolver = applicationContext.contentResolver

    fun content(gameId: String, relativePath: String): ExternalGameContent? {
        val file = resolve(gameId, relativePath) ?: return null
        return ExternalGameContent(file.uri, file.length().coerceAtLeast(0L))
    }

    fun open(content: ExternalGameContent): InputStream =
        requireNotNull(resolver.openInputStream(content.uri)) {
            "External game content could not be opened"
        }

    fun preview(gameId: String, relativePaths: List<String>): ContentRemovalPreview {
        val files = validateOwnedFiles(gameId, relativePaths)
        return ContentRemovalPreview(files.sumOf { it.length().coerceAtLeast(0L) }, files.size)
    }

    fun uninstall(gameId: String, relativePaths: List<String>): Int {
        val files = validateOwnedFiles(gameId, relativePaths)
        var removed = 0
        try {
            files.forEach { file ->
                check(file.delete()) { "Unable to remove external game content" }
                removed++
            }
        } catch (error: Exception) {
            throw ContentRemovalFailed(removed, error)
        }
        return removed
    }

    private fun validateOwnedFiles(gameId: String, relativePaths: List<String>): List<DocumentFile> {
        require(relativePaths.isNotEmpty()) { "No external content files are recorded" }
        return relativePaths.distinct().map { relativePath ->
            requireNotNull(resolve(gameId, relativePath)) {
                "External game content is unavailable; reconnect the selected library and retry"
            }
        }
    }

    private fun resolve(gameId: String, relativePath: String): DocumentFile? {
        val segments = externalLibrarySegments(gameId, relativePath)
        val configured = treeUri().takeIf(String::isNotBlank) ?: return null
        val uri = runCatching { Uri.parse(configured) }.getOrNull() ?: return null
        val root = DocumentFile.fromTreeUri(applicationContext, uri)
            ?.takeIf { it.exists() && it.isDirectory && it.canRead() }
            ?: return null
        var current = root.findFile(segments.first())?.takeIf { it.isDirectory } ?: return null
        segments.drop(1).forEach { segment ->
            current = current.findFile(segment) ?: return null
        }
        return current.takeIf { it.exists() && it.isFile && it.canRead() }
    }
}

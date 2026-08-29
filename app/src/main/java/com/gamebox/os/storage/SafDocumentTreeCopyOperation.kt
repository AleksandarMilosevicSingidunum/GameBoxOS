package com.gamebox.os.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/** Copies verified app-private content into a user-authorized SAF tree without deleting sources. */
class SafDocumentTreeCopyOperation(
    context: Context,
    treeUri: Uri,
    private val sourceRoot: File
) : ContentCopyOperation {
    private val resolver = context.applicationContext.contentResolver
    private val tree = DocumentFile.fromTreeUri(context.applicationContext, treeUri)

    override fun copy(item: ContentMigrationItem): Result<Unit> = runCatching {
        val source = File(sourceRoot, item.relativePath)
        require(source.canonicalFile.toPath().startsWith(sourceRoot.canonicalFile.toPath())) { "source path escapes app storage" }
        require(source.isFile) { "source content is missing" }
        val root = tree ?: throw ExternalStorageUnavailableException("external library is unavailable")
        if (!root.exists() || !root.isDirectory || !root.canWrite()) {
            throw ExternalStorageUnavailableException("external library is not writable")
        }
        val destination = ensureDirectory(root, item.gameId.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_').ifBlank { "game" }, item.relativePath)
        val tempName = destination.name + ".gamebox-partial"
        val partial = destination.parentFile?.createFile("application/octet-stream", tempName)
            ?: throw ExternalStorageUnavailableException("unable to create destination")
        try {
            source.inputStream().use { input -> resolver.openOutputStream(partial.uri, "w")?.use { output -> input.copyTo(output) }
                ?: throw ExternalStorageUnavailableException("unable to open destination") }
            val finalFile = destination.parentFile?.createFile("application/octet-stream", destination.name)
                ?: throw ExternalStorageUnavailableException("unable to finalize destination")
            resolver.openInputStream(partial.uri)?.use { input -> resolver.openOutputStream(finalFile.uri, "w")?.use { output -> input.copyTo(output) } }
                ?: throw ExternalStorageUnavailableException("unable to verify destination")
            partial.delete()
        } catch (error: Throwable) {
            partial.delete()
            throw error
        }
    }

    private fun ensureDirectory(root: DocumentFile, gameId: String, relativePath: String): DocumentFile {
        var current = root.findFile(gameId)?.takeIf { it.isDirectory } ?: root.createDirectory(gameId)
            ?: throw ExternalStorageUnavailableException("unable to create game directory")
        val segments = relativePath.trim('/').split('/').filter { it.isNotBlank() }
        require(segments.isNotEmpty() && segments.none { it == ".." }) { "invalid migration path" }
        segments.dropLast(1).forEach { segment ->
            current = current.findFile(segment)?.takeIf { it.isDirectory } ?: current.createDirectory(segment)
                ?: throw ExternalStorageUnavailableException("unable to create destination directory")
        }
        return current.createFile("application/octet-stream", segments.last())
            ?: throw ExternalStorageUnavailableException("unable to create destination file")
    }
}

package com.gamebox.os.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.security.MessageDigest

/** Copies app-private content into a user-authorized SAF tree and optionally cuts over after verification. */
class SafDocumentTreeCopyOperation(
    context: Context,
    treeUri: Uri,
    private val sourceRoot: File,
    private val deleteSourceAfterVerification: Boolean = false,
) : ContentCopyOperation {
    private val resolver = context.applicationContext.contentResolver
    private val tree = DocumentFile.fromTreeUri(context.applicationContext, treeUri)
    override fun copy(item: ContentMigrationItem): Result<Unit> = runCatching {
        val source = File(sourceRoot, item.relativePath)
        require(source.canonicalFile.toPath().startsWith(sourceRoot.canonicalFile.toPath())) { "source path escapes app storage" }
        require(source.isFile) { "source content is missing" }
        val root = tree ?: throw ExternalStorageUnavailableException("external library is unavailable")
        if (!root.exists() || !root.isDirectory || !root.canWrite()) throw ExternalStorageUnavailableException("external library is not writable")
        val gameId = item.gameId.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_').ifBlank { "game" }
        val segments = item.relativePath.trim('/').split('/').filter { it.isNotBlank() }
        require(segments.isNotEmpty() && segments.none { it == ".." }) { "invalid migration path" }
        val parent = ensureParent(root, gameId, segments.dropLast(1))
        val fileName = segments.last()
        val partial = parent.createFile("application/octet-stream", fileName + ".gamebox-partial") ?: throw ExternalStorageUnavailableException("unable to create destination")
        try {
            source.inputStream().use { input -> resolver.openOutputStream(partial.uri, "w")?.use { output -> input.copyTo(output) } ?: throw ExternalStorageUnavailableException("unable to open destination") }
            val finalFile = parent.findFile(fileName)?.takeIf { it.isFile } ?: parent.createFile("application/octet-stream", fileName) ?: throw ExternalStorageUnavailableException("unable to finalize destination")
            resolver.openInputStream(partial.uri)?.use { input ->
                resolver.openOutputStream(finalFile.uri, "w")?.use { output -> input.copyTo(output) }
                    ?: throw ExternalStorageUnavailableException("unable to finalize destination")
            } ?: throw ExternalStorageUnavailableException("unable to finalize destination")
            val sourceDigest = source.inputStream().use(::sha256)
            val destinationDigest = resolver.openInputStream(finalFile.uri)?.use(::sha256)
                ?: throw ExternalStorageUnavailableException("unable to verify destination")
            check(finalFile.length() == source.length() && destinationDigest.contentEquals(sourceDigest)) {
                "destination verification failed"
            }
            partial.delete()
            if (deleteSourceAfterVerification) {
                check(source.delete()) { "destination verified but internal source could not be removed" }
            }
        } catch (error: Throwable) { partial.delete(); throw error }
    }
    private fun sha256(input: java.io.InputStream): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count > 0) digest.update(buffer, 0, count)
        }
        return digest.digest()
    }

    private fun ensureParent(root: DocumentFile, gameId: String, directories: List<String>): DocumentFile {
        var current = root.findFile(gameId)?.takeIf { it.isDirectory } ?: root.createDirectory(gameId) ?: throw ExternalStorageUnavailableException("unable to create game directory")
        directories.forEach { segment -> current = current.findFile(segment)?.takeIf { it.isDirectory } ?: current.createDirectory(segment) ?: throw ExternalStorageUnavailableException("unable to create destination directory") }
        return current
    }
}

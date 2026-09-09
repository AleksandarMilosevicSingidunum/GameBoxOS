package com.gamebox.os.storage

import java.io.File
import java.nio.file.Files

data class ContentRemovalManifest(val gameId: String, val relativePaths: List<String>)
data class ContentRemovalPreview(val bytes: Long, val files: Int)
class ContentRemovalFailed(val removedFiles: Int, cause: Exception) : Exception("Content removal incomplete", cause)

/** Deletes only individually recorded files, never a directory or save root. */
class GameOwnedContentUninstaller(filesDirectory: File) {
    private val root = filesDirectory.canonicalFile

    fun preview(manifest: ContentRemovalManifest): ContentRemovalPreview {
        val files = validate(manifest).filter(File::isFile)
        return ContentRemovalPreview(files.sumOf(File::length), files.size)
    }

    fun uninstall(manifest: ContentRemovalManifest): Int {
        val files = validate(manifest) // Validate the whole multi-file set before deleting anything.
        var removed = 0
        try {
            files.forEach { file ->
                validateFile(file)
                if (Files.deleteIfExists(file.toPath())) removed++
            }
        } catch (error: Exception) {
            throw ContentRemovalFailed(removed, error)
        }
        return removed
    }

    private fun validate(manifest: ContentRemovalManifest): List<File> {
        val id = manifest.gameId
        require(id.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,95}"))) { "Invalid game ID" }
        require(manifest.relativePaths.isNotEmpty()) { "No owned content files are recorded" }
        return manifest.relativePaths.distinct().map { path ->
            require(path.isNotBlank() && !File(path).isAbsolute &&
                path.none { it == '\\' || it == ':' || it == '\u0000' } &&
                path.split('/').none { it.isEmpty() || it == "." || it == ".." }) { "Unsafe content path" }
            val owned = path.startsWith("imports/$id/") || path.startsWith("installed/remote/$id/") ||
                (id == "galaxy-patrol" && path == "installed/retro/galaxy-patrol/content/galaxy-patrol.nes")
            require(owned) { "Content does not belong to the selected game" }
            File(root, path).also(::validateFile)
        }
    }

    private fun validateFile(file: File) {
        require(file.canonicalFile == file.absoluteFile &&
            file.canonicalPath.startsWith(root.path + File.separator)) { "Content path redirects outside its owner" }
        var part: File? = file
        while (part != null && part != root) {
            require(!Files.isSymbolicLink(part.toPath())) { "Symbolic links cannot be uninstalled" }
            part = part.parentFile
        }
        require(!file.exists() || file.isFile) { "Only exact content files may be removed" }
    }
}

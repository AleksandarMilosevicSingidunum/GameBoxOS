package com.gamebox.os.storage

import java.io.File

class FileContentUninstaller(rootDirectory: File) {
    private val root = rootDirectory.canonicalFile

    fun uninstall(relativeContentPath: String): Boolean {
        require(relativeContentPath.isNotBlank()) { "Content path cannot be blank" }
        require(!File(relativeContentPath).isAbsolute) { "Content path must be relative" }
        val target = File(root, relativeContentPath).canonicalFile
        require(target.path.startsWith(root.path + File.separator)) {
            "Content path escapes install storage"
        }
        if (!target.exists()) return false
        require(target.isFile) { "Only an exact content file can be uninstalled" }
        check(target.delete()) { "Unable to uninstall content file" }
        return true
    }
}

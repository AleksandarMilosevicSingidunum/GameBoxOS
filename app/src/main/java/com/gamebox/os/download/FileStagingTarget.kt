package com.gamebox.os.download

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class FileStagingTarget(rootDirectory: File, relativePath: String) : StagingTarget {
    private val root = rootDirectory.canonicalFile
    val finalFile: File = resolveContained(relativePath)
    val stagingFile: File = File(
        requireNotNull(finalFile.parentFile),
        finalFile.name + ".part"
    )

    init {
        require(relativePath.isNotBlank()) { "Relative path cannot be blank" }
        require(!File(relativePath).isAbsolute) { "Install path must be relative" }
    }

    override fun openOutput(): OutputStream {
        check(finalFile.parentFile?.mkdirs() != false || finalFile.parentFile?.isDirectory == true) {
            "Unable to create staging directory"
        }
        return FileOutputStream(stagingFile, false)
    }

    override fun openInput(): InputStream = FileInputStream(stagingFile)

    override fun commit() {
        check(stagingFile.isFile) { "Staged file is missing" }
        try {
            Files.move(
                stagingFile.toPath(),
                finalFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                stagingFile.toPath(),
                finalFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    override fun discard() {
        if (stagingFile.exists()) check(stagingFile.delete()) { "Unable to discard staged file" }
    }

    private fun resolveContained(relativePath: String): File {
        require(relativePath.isNotBlank()) { "Relative path cannot be blank" }
        require(!File(relativePath).isAbsolute) { "Install path must be relative" }
        val candidate = File(root, relativePath).canonicalFile
        val prefix = root.path + File.separator
        require(candidate.path.startsWith(prefix)) { "Install path escapes app-private storage" }
        require(candidate != root) { "Install path must identify a file" }
        return candidate
    }
}

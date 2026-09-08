package com.gamebox.os.storage

import java.io.File
import java.io.InputStream
import java.nio.file.Files

/** Imports a first managed save only; replacing existing saves is a separate action. */
class InitialSaveImporter(private val root: File, private val limit: Long = 16L * 1024 * 1024) {
    init { require(limit > 0) }

    @Synchronized fun importNew(relativePath: String, input: InputStream): Long {
        val directory = root.canonicalFile
        val destination = directory.resolve(relativePath).canonicalFile
        require(relativePath.isNotBlank() && !File(relativePath).isAbsolute)
        require(destination.path.startsWith(directory.path + File.separator))
        check(!destination.exists()) { "A save already exists; use backup and restore instead" }
        check(destination.parentFile.mkdirs() || destination.parentFile.isDirectory)
        val staged = File.createTempFile("save-import-", ".part", destination.parentFile)
        try {
            var bytes = 0L
            staged.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    bytes += count
                    require(bytes <= limit) { "Save exceeds the 16 MiB import limit" }
                    output.write(buffer, 0, count)
                }
            }
            require(bytes > 0) { "Selected save is empty" }
            Files.move(staged.toPath(), destination.toPath())
            return bytes
        } finally {
            staged.delete()
        }
    }
}

package com.gamebox.os.storage

import com.gamebox.os.importer.RomImportPolicy
import java.io.File

/** Creates the stable per-console managed game tree on first app start. */
object GameStorageTree {
    private val consoles = RomImportPolicy.managedPlatformDirectories()

    fun ensure(filesDirectory: File) {
        val root = filesDirectory.resolve("imports")
        consoles.forEach { console ->
            root.resolve(console).resolve(".keep").apply {
                requireNotNull(parentFile).mkdirs()
                if (!exists()) writeText("GameBox managed $console directory\n")
            }
        }
    }
}

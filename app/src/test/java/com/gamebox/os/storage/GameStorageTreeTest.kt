package com.gamebox.os.storage

import com.gamebox.os.importer.RomImportPolicy
import java.nio.file.Files
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStorageTreeTest {
    @Test
    fun createsEveryManagedConsoleDirectoryUsedByImportPolicy() {
        val root = Files.createTempDirectory("gamebox-storage-tree-").toFile()
        try {
            GameStorageTree.ensure(root)
            RomImportPolicy.managedPlatformDirectories().forEach { console ->
                assertTrue(console, root.resolve("imports/$console/.keep").isFile)
            }
        } finally {
            root.deleteRecursively()
        }
    }
}

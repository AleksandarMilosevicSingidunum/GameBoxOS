package com.gamebox.os.storage

import java.nio.file.Files
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStorageTreeTest {
    @Test
    fun createsEveryManagedConsoleDirectoryUsedByImportPolicy() {
        val root = Files.createTempDirectory("gamebox-storage-tree-").toFile()
        try {
            GameStorageTree.ensure(root)
            listOf(
                "nintendo-entertainment-system", "super-nintendo", "nintendo-64",
                "game-boy", "game-boy-color", "game-boy-advance", "nintendo-ds",
                "nintendo-3ds", "nintendo-gamecube", "nintendo-wii", "nintendo-wii-u",
                "nintendo-switch", "playstation", "playstation-2", "playstation-3",
                "playstation-portable", "playstation-vita", "sega-master-system",
                "sega-game-gear", "sega-genesis-mega-drive", "sega-cd", "sega-saturn",
                "sega-dreamcast", "xbox", "xbox-360", "pc-engine-turbografx-16",
                "neo-geo", "atari", "wonderswan", "arcade", "homebrew", "retro",
            ).forEach { console ->
                assertTrue(console, root.resolve("imports/$console/.keep").isFile)
            }
        } finally {
            root.deleteRecursively()
        }
    }
}

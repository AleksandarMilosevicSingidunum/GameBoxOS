package com.gamebox.os.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class InstalledContentMigrationTest {
    @Test fun plansRealInstalledFilesByGame() {
        val root = Files.createTempDirectory("gamebox-installed").toFile()
        try {
            val file = root.resolve("retro/game-one/content/game.bin")
            file.parentFile.mkdirs(); file.writeText("TEST")
            val plan = InstalledContentMigration(root).plan()
            assertEquals(1, plan.items.size); assertEquals("game-one", plan.items.single().gameId)
            assertEquals("retro/game-one/content/game.bin", plan.items.single().relativePath)
            assertEquals(4, plan.totalBytes)
        } finally { root.deleteRecursively() }
    }
    @Test fun missingRootProducesEmptyPlan() {
        val root = Files.createTempDirectory("gamebox-missing").resolve("none").toFile()
        assertTrue(InstalledContentMigration(root).plan().isEmpty)
    }
}

package com.gamebox.os

import com.gamebox.os.storage.DirectorySaveAdapter
import java.io.File
import org.junit.Assert.assertEquals
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertThrows
import org.junit.Test

class DirectorySaveAdapterTest {
    @Test
    fun discoversOnlyRequestedGameDirectory() {
        val root = createTempDirectory("gamebox-saves-").toFile()
        try {
            File(root, "game-a").mkdirs()
            File(root, "game-a/progress.sav").writeText("A")
            File(root, "game-b").mkdirs()
            File(root, "game-b/progress.sav").writeText("B")

            val artifacts = DirectorySaveAdapter(root).discover("game-a")

            assertEquals(1, artifacts.size)
            assertEquals("game-a/progress.sav", artifacts.single().relativePath)
            assertEquals("game-a", artifacts.single().gameId)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsTraversalGameId() {
        val root = createTempDirectory("gamebox-saves-").toFile()
        try {
            assertThrows(IllegalArgumentException::class.java) {
                DirectorySaveAdapter(root).discover("../other")
            }
        } finally {
            root.deleteRecursively()
        }
    }
}

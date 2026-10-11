package com.gamebox.os.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DirectorySaveAdapterDeviceTest {
    @Test
    fun ignoresLinkedSaveFilesAndDirectoriesOnDevice() {
        val cache = ApplicationProvider.getApplicationContext<Context>().cacheDir
        val root = File(cache, "save-link-check-${System.nanoTime()}").apply { check(mkdirs()) }
        val game = File(root, "game-a").apply { mkdirs() }
        val other = File(root, "game-b").apply { mkdirs() }
        val loop = File(game, "loop").toPath()
        val linkedFile = File(game, "linked.sav").toPath()
        val linkedGame = File(root, "game-c").toPath()
        try {
            File(game, "progress.sav").writeText("A")
            val otherSave = File(other, "progress.sav").apply { writeText("B") }
            Files.createSymbolicLink(loop, game.toPath())
            Files.createSymbolicLink(linkedFile, otherSave.toPath())
            Files.createSymbolicLink(linkedGame, other.toPath())

            assertEquals(
                listOf("game-a/progress.sav"),
                DirectorySaveAdapter(root).discover("game-a").map { it.relativePath },
            )
            assertThrows(IllegalArgumentException::class.java) {
                DirectorySaveAdapter(root).discover("game-c")
            }
            assertEquals("B", otherSave.readText())
        } finally {
            Files.deleteIfExists(loop)
            Files.deleteIfExists(linkedFile)
            Files.deleteIfExists(linkedGame)
            root.deleteRecursively()
        }
    }
}

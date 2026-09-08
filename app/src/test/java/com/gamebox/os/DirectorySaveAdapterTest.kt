package com.gamebox.os

import com.gamebox.os.storage.DirectorySaveAdapter
import com.gamebox.os.storage.SaveDiscoveryLimitExceededException
import java.io.File
import org.junit.Assert.assertEquals
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertThrows
import org.junit.Test

class DirectorySaveAdapterTest {
    @Test(timeout = 5_000)
    fun ignoresLinkedDirectoriesFilesAndStagingArtifacts() {
        val root = createTempDirectory("gamebox-save-links-").toFile()
        val game = File(root, "game-a").apply { mkdirs() }
        val other = File(root, "game-b").apply { mkdirs() }
        val loop = File(game, "loop").toPath()
        val linkedFile = File(game, "linked.sav").toPath()
        val linkedGame = File(root, "game-c").toPath()
        try {
            File(game, "progress.sav").writeText("A")
            val otherSave = File(other, "progress.sav").apply { writeText("B") }
            File(game, "save-import-123.part").writeText("incomplete")
            File(game, "progress.sav.restore.part").writeText("incomplete")
            java.nio.file.Files.createSymbolicLink(loop, game.toPath())
            java.nio.file.Files.createSymbolicLink(linkedFile, otherSave.toPath())
            java.nio.file.Files.createSymbolicLink(linkedGame, other.toPath())
            assertEquals(listOf("game-a/progress.sav"),
                DirectorySaveAdapter(root).discover("game-a").map { it.relativePath })
            assertThrows(IllegalArgumentException::class.java) {
                DirectorySaveAdapter(root).discover("game-c")
            }
            assertEquals("B", otherSave.readText())
        } finally {
            java.nio.file.Files.deleteIfExists(loop)
            java.nio.file.Files.deleteIfExists(linkedFile)
            java.nio.file.Files.deleteIfExists(linkedGame)
            root.deleteRecursively()
        }
    }

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
    fun failsClosedWhenArtifactLimitWouldTruncateDiscovery() {
        val root = createTempDirectory("gamebox-saves-").toFile()
        try {
            File(root, "game-a").mkdirs()
            File(root, "game-a/one.sav").writeText("1")
            File(root, "game-a/two.sav").writeText("2")

            assertThrows(SaveDiscoveryLimitExceededException::class.java) {
                DirectorySaveAdapter(root, maxArtifacts = 1).discover("game-a")
            }
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

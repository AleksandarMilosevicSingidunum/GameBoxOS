package com.gamebox.os

import com.gamebox.os.storage.SaveSnapshotManifest
import com.gamebox.os.storage.SaveSnapshotManifestStore
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SaveSnapshotManifestStoreTest {
    @Test
    fun persistsAndReloadsManifestAcrossStoreInstances() {
        val root = createTempDirectory("gamebox-save-manifest-").toFile()
        try {
            SaveSnapshotManifestStore(root).save(
                SaveSnapshotManifest(
                    gameId = "game-a",
                    createdAtMillis = 123L,
                    relativePaths = listOf("game-a/slot1.sav", "game-a/slot2.sav"),
                ),
            )

            val loaded = SaveSnapshotManifestStore(root).load("game-a")

            assertEquals("game-a", loaded?.gameId)
            assertEquals(123L, loaded?.createdAtMillis)
            assertEquals(listOf("game-a/slot1.sav", "game-a/slot2.sav"), loaded?.relativePaths)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsTraversalPaths() {
        val root = createTempDirectory("gamebox-save-manifest-").toFile()
        try {
            assertThrows(IllegalArgumentException::class.java) {
                SaveSnapshotManifestStore(root).save(
                    SaveSnapshotManifest("game-a", 1L, listOf("../other/save.dat")),
                )
            }
        } finally {
            root.deleteRecursively()
        }
    }
}

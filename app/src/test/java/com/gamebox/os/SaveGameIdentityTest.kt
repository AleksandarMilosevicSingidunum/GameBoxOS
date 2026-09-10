package com.gamebox.os

import com.gamebox.os.storage.requireSaveGameId
import com.gamebox.os.storage.requireSavePathForGame
import com.gamebox.os.storage.resolveGameSave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SaveGameIdentityTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    @Test fun resolvesNestedPathInsideOnlySelectedGame() {
        val root = temporaryFolder.newFolder("saves")
        assertEquals(root.resolve("game/slot/save.dat").canonicalFile,
            resolveGameSave(root, "game", "game/slot/save.dat"))
        assertThrows(IllegalArgumentException::class.java) {
            resolveGameSave(root, "game", "other/save.dat")
        }
    }
    @Test fun acceptsGameScopedNestedArtifacts() {
        requireSavePathForGame("owned-game", "owned-game/slot-1/progress.sav")
    }
    @Test fun rejectsOtherGamesAndTraversal() {
        for (path in listOf("other/save.dat", "game-other/save.dat", "game/../other/save.dat",
            "game/./save.dat", "game//save.dat", "game/save:stream", "game/\\save", "game/")) {
            assertThrows(IllegalArgumentException::class.java) { requireSavePathForGame("game", path) }
        }
        for (id in listOf("", ".", "..", "../game", "game/other")) {
            assertThrows(IllegalArgumentException::class.java) { requireSaveGameId(id) }
        }
    }
}

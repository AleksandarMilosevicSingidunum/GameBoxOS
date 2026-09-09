package com.gamebox.os

import com.gamebox.os.storage.requireSaveGameId
import com.gamebox.os.storage.requireSavePathForGame
import org.junit.Assert.assertThrows
import org.junit.Test

class SaveGameIdentityTest {
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


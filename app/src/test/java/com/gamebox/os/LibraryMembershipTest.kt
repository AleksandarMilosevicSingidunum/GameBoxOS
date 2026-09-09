package com.gamebox.os

import com.gamebox.os.domain.*
import org.junit.Assert.*
import org.junit.Test

class LibraryMembershipTest {
    private val game = Game(GameId("imported"), "Imported", "PSP", 2000, "Test", 1, InstallState.NOT_INSTALLED)
    @Test fun removedImportRemainsReachableButIsNotPlayable() {
        val retained = game.copy(localContentRelativePath = "imported/game.iso")
        assertTrue(retained.belongsToLibrary())
        assertTrue(retained.canReimportContent())
        assertFalse(retained.copy(state = InstallState.INSTALLED).canReimportContent())
        assertFalse(retained.copy(state = InstallState.DOWNLOADING).canReimportContent())
    }
    @Test fun unownedCatalogEntryDoesNotPopulateLibrary() {
        assertFalse(game.belongsToLibrary())
        assertFalse(game.canReimportContent())
        assertTrue(game.copy(savePresent = true).belongsToLibrary())
        assertTrue(game.copy(lastPlayed = "2026-09-09").belongsToLibrary())
    }
}


package com.gamebox.os.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class SafSaveAdapterPathTest {
    @Test
    fun resolvesGameRelativeSavePaths() {
        assertEquals(
            listOf("game-a", "PSP", "SAVEDATA", "slot.dat"),
            safSaveSegments("game-a", "game-a/PSP/SAVEDATA/slot.dat"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCrossGamePath() {
        safSaveSegments("game-a", "game-b/slot.dat")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTraversal() {
        safSaveSegments("game-a", "game-a/../other/slot.dat")
    }
}

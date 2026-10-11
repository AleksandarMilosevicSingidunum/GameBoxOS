package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityLabelTest {
    @Test
    fun usesSingularOnlyForOne() {
        assertEquals("0 games", quantityLabel(0, "game"))
        assertEquals("1 game", quantityLabel(1, "game"))
        assertEquals("2 games", quantityLabel(2, "game"))
    }

    @Test
    fun supportsCustomPluralPhrases() {
        assertEquals("1 game with saves", quantityLabel(1, "game with saves"))
        assertEquals("3 games with saves", quantityLabel(3, "game with saves", "games with saves"))
    }
}

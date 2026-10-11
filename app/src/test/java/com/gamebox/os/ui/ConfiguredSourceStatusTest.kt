package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ConfiguredSourceStatusTest {
    @Test
    fun sourceMessagesUseDistinctAccessibleOutcomeTones() {
        assertEquals(
            ConfiguredSourceStatusKind.ERROR,
            configuredSourceStatusKind("Vimm Lair search failed for PSP"),
        )
        assertEquals(
            ConfiguredSourceStatusKind.SUCCESS,
            configuredSourceStatusKind("Found 4 results from Vimm Lair"),
        )
        assertEquals(
            ConfiguredSourceStatusKind.INFO,
            configuredSourceStatusKind("Searching Vimm Lair across 4 consoles…"),
        )
    }
}

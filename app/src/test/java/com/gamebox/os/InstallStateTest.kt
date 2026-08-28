package com.gamebox.os

import org.junit.Assert.assertEquals
import org.junit.Test

class InstallStateTest {
    @Test fun blueprintInstallStatesRemainExplicit() {
        assertEquals(10, InstallState.entries.size)
        assertEquals("NOT_INSTALLED", InstallState.entries.first().name)
        assertEquals("FAILED", InstallState.entries.last().name)
    }
}

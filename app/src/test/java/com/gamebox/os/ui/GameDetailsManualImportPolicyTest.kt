package com.gamebox.os.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDetailsManualImportPolicyTest {
    @Test
    fun onlySuccessfulArchiveImportsRequestSourceRemoval() {
        assertTrue(shouldRemoveImportedArchive("copy.zip"))
        assertTrue(shouldRemoveImportedArchive("copy.7Z"))
        assertFalse(shouldRemoveImportedArchive("copy.nes"))
        assertFalse(shouldRemoveImportedArchive("copy"))
    }
}

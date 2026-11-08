package com.gamebox.os.importer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportedSourceCleanupTest {
    @Test
    fun originalIsKeptUntilRegistrationJournalCleanupIsConfirmed() {
        assertFalse(shouldDeleteImportedSource(cleanupDeferred = true))
        assertTrue(shouldDeleteImportedSource(cleanupDeferred = false))
    }
}

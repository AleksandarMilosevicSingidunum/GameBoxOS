package com.gamebox.os.importer

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportedSourceCleanupTest {
    @Test
    fun removesOnlyTheExplicitlySelectedLocalSource() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val selectedSource = File.createTempFile("gamebox-import-source-", ".rom", context.cacheDir)
        selectedSource.writeText("test fixture")

        assertTrue(deleteImportedSource(context, Uri.fromFile(selectedSource)))
        assertFalse(selectedSource.exists())
    }

    @Test
    fun reportsFalseWhenSelectedLocalSourceIsAlreadyMissing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val missingSource = File(context.cacheDir, "gamebox-missing-import-source.rom")
        missingSource.delete()

        assertFalse(deleteImportedSource(context, Uri.fromFile(missingSource)))
    }
}

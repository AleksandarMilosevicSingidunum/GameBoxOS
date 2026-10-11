package com.gamebox.os.importer

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.domain.GameId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AuthorizedRomImporterStorageFailureTest {
    @Test
    fun unavailableConsoleDirectoryReturnsFailureInsteadOfEscapingImport() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val filesRoot = File(app.cacheDir, "import-storage-${UUID.randomUUID()}").apply { mkdirs() }
        val imports = File(filesRoot, "imports").apply { mkdirs() }
        val consolePathBlocker = File(imports, "psp").apply { writeText("not a directory") }
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = filesRoot
            override fun getApplicationContext(): Context = this
        }

        try {
            val result = AuthorizedRomImporter(context).importSet(
                gameId = GameId("storage-failure-test"),
                sources = listOf(RomImportSource(Uri.parse("content://unused/game.iso"), "game.iso")),
                platform = "PSP",
            )

            assertTrue("storage failure should be returned to the import UI: $result", result is RomImportSetResult.Failed)
            assertTrue("the isolated obstruction should be untouched", consolePathBlocker.isFile)
        } finally {
            filesRoot.deleteRecursively()
        }
    }
}

package com.gamebox.os.storage

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.LocalContentFile
import com.gamebox.os.importer.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class ReimportIdentityIntegrationTest {
    @Test fun identityIsCheckedBeforeReplacingExistingFiles(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(app.cacheDir.toPath(), "reimport-identity-").toFile()
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = root
            override fun getApplicationContext(): Context = this
        }
        try {
            val id = GameId("identity-test")
            val originalBytes = "original synthetic game bytes".toByteArray()
            val source = root.resolve("source/game.iso").apply { parentFile.mkdirs(); writeBytes(originalBytes) }
            val importer = AuthorizedRomImporter(context)
            val sources = listOf(RomImportSource(Uri.fromFile(source), "game.iso"))
            val initial = importer.importSet(id, sources, "PSP") as RomImportSetResult.Imported
            val expected = initial.files.map {
                LocalContentFile(RomImportPolicy.importRootRelativePath(id, it.relativePath),
                    it.hashes.sha256, it.mimeType)
            }
            val installed = root.resolve(initial.launchFile.relativePath)
            val save = root.resolve("saves/identity-test/progress.sav").apply {
                parentFile.mkdirs(); writeText("synthetic saved progress")
            }
            source.writeText("different game bytes")
            val rejected = importer.importSet(id, sources, "PSP", expected)
            assertTrue(rejected.toString(), rejected is RomImportSetResult.Rejected)
            assertArrayEquals(originalBytes, installed.readBytes())
            assertEquals("synthetic saved progress", save.readText())
            assertFalse(root.resolve("imports").listFiles().orEmpty().any {
                it.name.startsWith(".staging-") || it.name.startsWith(".backup-")
            })
            // A removed file can be restored only with its retained identity.
            assertTrue(installed.delete())
            source.writeBytes(originalBytes)
            val restored = importer.importSet(id, sources, "PSP", expected)
            assertTrue(restored.toString(), restored is RomImportSetResult.Imported)
            assertArrayEquals(originalBytes, installed.readBytes())
            assertEquals("synthetic saved progress", save.readText())
        } finally {
            // This unique test-owned private temporary directory only.
            root.deleteRecursively()
        }
    }
}


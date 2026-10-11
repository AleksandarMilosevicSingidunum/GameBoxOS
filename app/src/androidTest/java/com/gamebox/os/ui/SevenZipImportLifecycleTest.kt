package com.gamebox.os.ui

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.ImportedGameRegistration
import com.gamebox.os.domain.GameId
import com.gamebox.os.importer.AuthorizedRomImporter
import java.io.File
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SevenZipImportLifecycleTest {
    @Test
    fun archiveIsRemovedOnlyAfterItsPayloadIsRegistered() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        assertEquals("com.gamebox.os.debug", app.packageName)
        val root = createTempDir(app.cacheDir, "7z-import-lifecycle-")
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = root
            override fun getApplicationContext(): Context = this
        }
        val registrations = mutableListOf<ImportedGameRegistration>()
        val repository = object : GameRepository by app.container.gameRepository {
            override suspend fun registerImportedGame(imported: ImportedGameRegistration) {
                registrations += imported
            }
        }
        val importer = AuthorizedRomImporter(context)
        try {
            val game = discoveryGame("sevenzip-fixture")
            val bytes = byteArrayOf(7, 8, 9, 10)
            val archive = sevenZip(root, "fixture.7z", "nested/fixture.iso", bytes)

            val result = DiscoveryImportWorkflow(context, importer, repository, game, "PSP")
                .importSingle(Uri.fromFile(archive), archive.name, removeSourceAfterSuccess = true)

            assertTrue(result.message, result.succeeded)
            assertTrue(result.message.contains("source removed"))
            assertFalse("original archive should be removed after registration", archive.exists())
            val registration = registrations.single()
            assertEquals(game.id, registration.id)
            assertEquals("PSP", registration.platform)
            val importedFile = root.resolve("imports").resolve(registration.relativePath)
            assertTrue(importedFile.isFile)
            assertTrue(importedFile.readBytes().contentEquals(bytes))
            assertFalse(registration.relativePath.endsWith(".7z"))

            val broken = root.resolve("broken.7z").apply { writeText("not a 7z archive") }
            val failed = DiscoveryImportWorkflow(
                context,
                importer,
                repository,
                discoveryGame("sevenzip-failure"),
                "PSP",
            ).importSingle(Uri.fromFile(broken), broken.name, removeSourceAfterSuccess = true)

            assertFalse(failed.succeeded)
            assertTrue(failed.message.startsWith("Import failed:") || failed.message.startsWith("Import rejected:"))
            assertTrue("source archive must remain after unsuccessful import", broken.exists())
            assertEquals(1, registrations.size)

            val unavailable = root.resolve("missing.iso")
            val unavailableResult = DiscoveryImportWorkflow(
                context,
                importer,
                repository,
                discoveryGame("sevenzip-unavailable"),
                "PSP",
            ).importSingle(Uri.fromFile(unavailable), unavailable.name, removeSourceAfterSuccess = true)
            assertFalse(unavailableResult.succeeded)
            assertTrue(unavailableResult.message.isNotBlank())
            assertEquals(1, registrations.size)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun discoveryGame(id: String) = DiscoveryGame(
        id = GameId(id),
        title = "Synthetic 7-Zip fixture",
        platformId = "psp",
        region = null,
        releaseDate = null,
        description = null,
        players = null,
        rating = null,
        coverUrl = null,
        backgroundUrl = null,
        logoUrl = null,
        screenshots = emptyList(),
        favorite = false,
    )

    private fun sevenZip(root: File, archiveName: String, entryName: String, bytes: ByteArray): File {
        val payload = root.resolve("payload.iso").apply { writeBytes(bytes) }
        return root.resolve(archiveName).also { archive ->
            SevenZOutputFile(archive).use { sevenZip ->
                sevenZip.putArchiveEntry(sevenZip.createArchiveEntry(payload, entryName))
                sevenZip.write(bytes)
                sevenZip.closeArchiveEntry()
            }
            payload.delete()
        }
    }

    private fun createTempDir(parent: File, prefix: String): File =
        File(parent, "$prefix${System.nanoTime()}").apply { check(mkdirs()) }
}

package com.gamebox.os.ui

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.gamebox.os.catalog.CatalogProvider
import com.gamebox.os.catalog.CatalogSnapshot
import com.gamebox.os.data.RoomGameRepository
import com.gamebox.os.data.local.GameBoxDatabase
import com.gamebox.os.data.local.toEntity
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.importer.AuthorizedRomImporter
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemDocumentPickerReimportTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun productionReimportCompletesThroughAndroidDocumentsUi(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(app.cacheDir.toPath(), "picker-reimport-").toFile()
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = root
            override fun getApplicationContext(): Context = this
        }
        val database = Room.inMemoryDatabaseBuilder(app, GameBoxDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val fileName = "gamebox-picker-" + UUID.randomUUID().toString().take(8) + ".iso"
        val payload = "synthetic owned game bytes for Android DocumentsUI".toByteArray()
        val hash = sha256(payload)
        val gameId = GameId("picker-e2e-" + UUID.randomUUID().toString().take(8))
        val retained = Game(
            id = gameId,
            title = "Picker E2E",
            platform = "PSP",
            year = 2008,
            genre = "Test",
            sizeMb = 1,
            state = InstallState.MISSING_FILES,
            localContentRelativePath = gameId.value + "/" + fileName,
            localContentSha256 = hash,
            localContentMimeType = "application/x-iso9660-image",
        )
        var sharedDocument: android.net.Uri? = null

        try {
            database.gameDao().upsert(retained.toEntity())
            val repository = RoomGameRepository(
                database.gameDao(),
                database.saveRecordDao(),
                object : CatalogProvider {
                    override suspend fun load(): CatalogSnapshot =
                        error("Seeded database must not load a catalog")
                },
                scope,
                {},
                {},
            )
            withTimeout(5_000) {
                repository.observeGames().first { rows -> rows.any { it.id == gameId } }
            }

            val resolver = app.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS,
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            sharedDocument = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            assertNotNull("Could not create the document-picker fixture", sharedDocument)
            resolver.openOutputStream(requireNotNull(sharedDocument), "w")!!.use { output ->
                output.write(payload)
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(requireNotNull(sharedDocument), values, null, null)

            val importer = AuthorizedRomImporter(context)
            compose.setContent {
                MaterialTheme {
                    ImportedGameReimportCard(
                        game = retained,
                        importer = importer,
                        repository = repository,
                        enabled = true,
                    )
                }
            }

            compose.onNodeWithText("Locate files").assertIsDisplayed().performClick()

            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val document = waitForDocument(device, fileName)
            document.click()

            // Some DocumentsUI builds return a single item immediately even when
            // multiple selection is allowed; others require the explicit Open action.
            SystemClock.sleep(600)
            if (device.currentPackageName.orEmpty().contains("documentsui", ignoreCase = true)) {
                device.findObject(By.text("Open"))?.click()
            }

            compose.waitUntil(timeoutMillis = 20_000) {
                repository.game(gameId)?.state == InstallState.INSTALLED
            }
            compose.onNodeWithText(
                "Content verified and restored. Existing saves and history retained."
            ).assertIsDisplayed()

            val installed = root.resolve("imports/" + gameId.value + "/" + fileName)
            assertArrayEquals(payload, installed.readBytes())
            assertEquals(hash, repository.game(gameId)?.localContentSha256)
            assertFalse(
                root.resolve("imports").listFiles().orEmpty().any { entry ->
                    entry.name.startsWith(".staging-" + gameId.value + "-") ||
                        entry.name.startsWith(".backup-" + gameId.value + "-") ||
                        entry.name.startsWith(".pending-registration-" + gameId.value + "-")
                }
            )
        } finally {
            sharedDocument?.let { app.contentResolver.delete(it, null, null) }
            scope.coroutineContext[Job]?.cancelAndJoin()
            database.close()
            root.deleteRecursively()
        }
    }

    private fun waitForDocument(device: UiDevice, fileName: String): androidx.test.uiautomator.UiObject2 {
        device.wait(Until.hasObject(By.pkg("com.google.android.documentsui")), 5_000)
        device.wait(Until.hasObject(By.pkg("com.android.documentsui")), 1_000)

        device.wait(Until.findObject(By.text(fileName)), 5_000)?.let { return it }

        // API/system-image variants may not open on Recent. Navigate to Downloads
        // through the standard roots drawer before retrying the exact fixture name.
        device.findObject(By.descContains("Show roots"))?.click()
        val downloads = device.wait(Until.findObject(By.text("Downloads")), 5_000)
            ?: error("Android DocumentsUI did not expose Downloads")
        downloads.click()

        return device.wait(Until.findObject(By.text(fileName)), 15_000)
            ?: error("Android DocumentsUI did not expose the test document in Downloads")
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}

package com.gamebox.os.storage

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.catalog.CatalogProvider
import com.gamebox.os.catalog.CatalogSnapshot
import com.gamebox.os.data.RoomGameRepository
import com.gamebox.os.data.ImportedGameRegistration
import com.gamebox.os.importer.*
import com.gamebox.os.data.local.GameBoxDatabase
import com.gamebox.os.data.local.SaveRecordEntity
import com.gamebox.os.data.local.toEntity
import com.gamebox.os.domain.*
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files

/** Production controller + Room + exact file deletion; save bytes are synthetic. */
@RunWith(AndroidJUnit4::class)
class GeneralContentRemovalTest {
    @Test fun symbolicLinkCannotRemoveAnotherGamesContent() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(app.cacheDir.toPath(), "content-link-test-").toFile()
        val link = root.resolve("imports/disc/game.chd")
        try {
            val other = root.resolve("imports/other/game.chd").apply { parentFile.mkdirs(); writeText("keep") }
            link.parentFile.mkdirs()
            Files.createSymbolicLink(link.toPath(), other.toPath())
            assertThrows(IllegalArgumentException::class.java) {
                GameOwnedContentUninstaller(root).uninstall(ContentRemovalManifest("disc", listOf("imports/disc/game.chd")))
            }
            assertEquals("keep", other.readText())
        } finally {
            Files.deleteIfExists(link.toPath())
            root.deleteRecursively()
        }
    }

    @Test fun importedDiscSetUninstallAndReimportRetainProgressMetadata(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(app.cacheDir.toPath(), "content-removal-").toFile()
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = root
            override fun getApplicationContext(): Context = this
        }
        val database = Room.inMemoryDatabaseBuilder(app, GameBoxDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val game = Game(GameId("disc-test"), "Disc Test", "PS1", 2000, "Test", 1,
                InstallState.INSTALLED, lastPlayed = "2026-09-09T00:00:00Z", minutesPlayed = 42,
                favorite = true, description = "Retained metadata",
                localContentRelativePath = "disc-test/game.cue", localContentSha256 = "a".repeat(64),
                localContentMimeType = "application/x-cue", localContentFiles = listOf(
                    LocalContentFile("disc-test/track.bin", "b".repeat(64), "application/octet-stream")))
            database.gameDao().upsert(game.toEntity())
            val repository = RoomGameRepository(database.gameDao(), database.saveRecordDao(),
                object : CatalogProvider { override suspend fun load(): CatalogSnapshot = error("Seeded database must not load a catalog") },
                scope, {}, {})
            withTimeout(5_000) { repository.observeGames().first { it.any { row -> row.id == game.id } } }
            fun fixture(path: String) = root.resolve(path).apply { parentFile.mkdirs(); writeText("content") }
            val cue = fixture("imports/disc-test/game.cue")
            val track = fixture("imports/disc-test/track.bin")
            val save = fixture("saves/disc-test/progress.sav")
            database.saveRecordDao().upsert(
                SaveRecordEntity(game.id.value, "disc-test/progress.sav", 1_000L, save.length())
            )
            val other = fixture("imports/other/content.chd")
            val controller = DefaultSaveSafetyController(context, database.saveRecordDao(), repository,
                scope, SettingsRepository(app), game.id)
            assertEquals(ContentRemovalPreview(14, 2), controller.contentRemovalPreview(game))
            // This isolated instrumentation environment intentionally has no configured
            // cloud provider. Exercise the explicit, user-acknowledged local-backup path.
            controller.uninstallContent(game, allowWithoutCloudBackup = true)
            withTimeout(5_000) { repository.observeGames().first { rows -> rows.any { it.id == game.id && it.state == InstallState.NOT_INSTALLED } } }
            assertFalse(cue.exists())
            assertFalse(track.exists())
            listOf(save, other).forEach { assertEquals("content", it.readText()) }
            assertTrue(
                SaveBackupService(root.resolve("saves"), root.resolve("save-backups"))
                    .hasBackup("disc-test/progress.sav")
            )
            val removed = requireNotNull(repository.game(game.id))
            assertEquals(42, removed.minutesPlayed)
            assertEquals(game.lastPlayed, removed.lastPlayed)
            assertTrue(removed.favorite)
            assertEquals(game.description, removed.description)
            // Exercise the actual multi-file importer and registration path with
            // a synthetic descriptor/track set, not a commercial game or emulator.
            val sourceCue = fixture("sources/game.cue").apply {
                writeText("FILE \"track.bin\" BINARY\n  TRACK 01 MODE2/2352\n    INDEX 01 00:00:00\n")
            }
            val sourceTrack = fixture("sources/track.bin")
            val result = AuthorizedRomImporter(context).importSet(game.id,
                listOf(RomImportSource(Uri.fromFile(sourceCue), "game.cue"),
                    RomImportSource(Uri.fromFile(sourceTrack), "track.bin")), "PS1")
            assertTrue(result.toString(), result is RomImportSetResult.Imported)
            val imported = result as RomImportSetResult.Imported
            val primary = imported.launchFile
            repository.registerImportedGame(ImportedGameRegistration(game.id, game.title, game.platform,
                game.year, imported.files.sumOf { it.hashes.sizeBytes },
                RomImportPolicy.importRootRelativePath(game.id, primary.relativePath),
                primary.hashes.sha256, primary.mimeType,
                additionalFiles = imported.files.filter { it != primary }.map {
                    LocalContentFile(RomImportPolicy.importRootRelativePath(game.id, it.relativePath),
                        it.hashes.sha256, it.mimeType)
                }))
            withTimeout(5_000) { repository.observeGames().first { rows -> rows.any { it.id == game.id && it.state == InstallState.INSTALLED } } }
            assertEquals("content", save.readText())
            assertEquals(42, repository.game(game.id)?.minutesPlayed)
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
            database.close()
            // Only this test's newly created private temporary tree is removed.
            root.deleteRecursively()
        }
    }

    @Test fun failedSaveBackupAbortsContentRemoval(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(app.cacheDir.toPath(), "content-backup-failure-").toFile()
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = root
            override fun getApplicationContext(): Context = this
        }
        val database = Room.inMemoryDatabaseBuilder(app, GameBoxDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val game = Game(
                id = GameId("backup-failure"),
                title = "Backup Failure",
                platform = "NES",
                year = 1985,
                genre = "Test",
                sizeMb = 1,
                state = InstallState.INSTALLED,
                localContentRelativePath = "backup-failure/game.nes",
                localContentSha256 = "c".repeat(64),
                localContentMimeType = "application/octet-stream",
            )
            database.gameDao().upsert(game.toEntity())
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
                repository.observeGames().first { rows -> rows.any { it.id == game.id } }
            }
            val content = root.resolve("imports/backup-failure/game.nes").apply {
                parentFile.mkdirs()
                writeText("owned game content")
            }
            val emptySave = root.resolve("saves/backup-failure/progress.sav").apply {
                parentFile.mkdirs()
                createNewFile()
            }
            database.saveRecordDao().upsert(
                SaveRecordEntity(game.id.value, "backup-failure/progress.sav", 1_000L, emptySave.length())
            )
            val controller = DefaultSaveSafetyController(
                context,
                database.saveRecordDao(),
                repository,
                scope,
                SettingsRepository(app),
                game.id,
            )

            val failure = runCatching { controller.uninstallContent(game) }.exceptionOrNull()

            assertNotNull("An invalid save backup must fail closed", failure)
            assertEquals("owned game content", content.readText())
            assertTrue(emptySave.exists())
            assertEquals(InstallState.INSTALLED, repository.game(game.id)?.state)
            assertFalse(
                SaveBackupService(root.resolve("saves"), root.resolve("save-backups"))
                    .hasBackup("backup-failure/progress.sav")
            )
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
            database.close()
            root.deleteRecursively()
        }
    }

}

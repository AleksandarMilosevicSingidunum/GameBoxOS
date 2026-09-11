package com.gamebox.os.storage

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.data.local.GameBoxDatabase
import com.gamebox.os.data.local.SaveRecordEntity
import com.gamebox.os.data.local.SaveRecordDao
import com.gamebox.os.domain.GameId
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class PerGameSaveControllerTest {
    @Test fun controllersKeepImportedSaveRecordsAndBackupsSeparate(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val root = Files.createTempDirectory(app.cacheDir.toPath(), "per-game-save-").toFile()
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = root
            override fun getApplicationContext(): Context = this
        }
        val database = Room.inMemoryDatabaseBuilder(app, GameBoxDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            fun controller(id: String) = DefaultSaveSafetyController(context, database.saveRecordDao(),
                app.container.gameRepository, scope, SettingsRepository(app), GameId(id))
            val first = controller("first-game")
            val second = controller("second-game")
            val source = root.resolve("source.sav").apply { writeText("FIRST synthetic save") }
            first.importInitialSave(Uri.fromFile(source))
            withTimeout(5_000) { first.observeState().first { it.saveRecordPresent } }
            withTimeout(5_000) { first.observeBusy().first { !it } }
            assertFalse(second.observeState().value.saveRecordPresent)
            source.writeText("SECOND synthetic save")
            second.importInitialSave(Uri.fromFile(source))
            withTimeout(5_000) { second.observeState().first { it.saveRecordPresent } }
            withTimeout(5_000) { second.observeBusy().first { !it } }
            first.backupSave()
            withTimeout(5_000) { first.observeState().first { it.operationMessage == "Backup completed" } }
            withTimeout(5_000) { first.observeBusy().first { !it } }
            val firstFile = root.resolve("saves/first-game/save.dat")
            val recordBeforeEmptyBackup = database.saveRecordDao().observe("first-game").first()
            firstFile.writeText("")
            first.backupSave()
            withTimeout(5_000) { first.observeState().first { it.operationMessage == "Backup failed safely" } }
            withTimeout(5_000) { first.observeBusy().first { !it } }
            assertEquals(recordBeforeEmptyBackup, database.saveRecordDao().observe("first-game").first())
            val retainedPayload = java.io.ByteArrayOutputStream()
            assertEquals(
                BackupResult.SUCCESS,
                SaveBackupService(root.resolve("saves"), root.resolve("save-backups"))
                    .exportBackup("first-game/save.dat", retainedPayload)
            )
            assertEquals("FIRST synthetic save", retainedPayload.toString(Charsets.UTF_8.name()))
            assertFalse(root.resolve("save-backups/first-game/save.dat.part").exists())
            firstFile.writeText("CHANGED")
            first.restoreSave()
            withTimeout(5_000) { first.observeState().first { it.operationMessage == "Restore completed" } }
            withTimeout(5_000) { first.observeBusy().first { !it } }
            assertEquals("FIRST synthetic save", firstFile.readText())
            assertEquals("SECOND synthetic save", root.resolve("saves/second-game/save.dat").readText())
            assertFalse(root.resolve("save-backups/second-game/save.dat").exists())
            source.writeText("NEW imported backup")
            first.importBackup(Uri.fromFile(source))
            withTimeout(5_000) { first.observeState().first { it.operationMessage == "Import completed" } }
            withTimeout(5_000) { first.observeBusy().first { !it } }
            assertEquals("FIRST synthetic save", firstFile.readText())
            first.restoreSave()
            withTimeout(5_000) { first.observeBusy().first { !it } }
            assertEquals("NEW imported backup", firstFile.readText())
            assertEquals("SECOND synthetic save", root.resolve("saves/second-game/save.dat").readText())
            database.saveRecordDao().upsert(SaveRecordEntity("first-game", "second-game/save.dat", 1, 1))
            withTimeout(5_000) { first.observeState().first { !it.saveRecordPresent } }
            assertTrue(second.observeState().value.saveRecordPresent)
            assertThrows(IllegalArgumentException::class.java) { first.uninstallTestContent() }
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
            database.close()
            root.deleteRecursively()
        }
    }

    @Test fun leavingPanelFinishesStartedImportAndKeepsSameGameLocked(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        val root = Files.createTempDirectory(app.cacheDir.toPath(), "save-lifecycle-").toFile()
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = root
            override fun getApplicationContext(): Context = this
        }
        val database = Room.inMemoryDatabaseBuilder(app, GameBoxDatabase::class.java).build()
        val oldScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val reachedRecordWrite = CompletableDeferred<Unit>()
        val allowRecordWrite = CompletableDeferred<Unit>()
        val dao = database.saveRecordDao()
        val delayedDao = object : SaveRecordDao by dao {
            override suspend fun upsert(record: SaveRecordEntity) {
                reachedRecordWrite.complete(Unit)
                allowRecordWrite.await()
                dao.upsert(record)
            }
        }
        try {
            val first = DefaultSaveSafetyController(context, delayedDao, app.container.gameRepository,
                oldScope, SettingsRepository(app), GameId("lifecycle-game"))
            val reopened = DefaultSaveSafetyController(context, dao, app.container.gameRepository,
                newScope, SettingsRepository(app), GameId("lifecycle-game"))
            val source = root.resolve("source.sav").apply { writeText("Synthetic lifecycle save") }
            first.importInitialSave(Uri.fromFile(source))
            withTimeout(5_000) { reachedRecordWrite.await() }
            oldScope.cancel()
            assertTrue(first.observeBusy().value)
            reopened.importInitialSave(Uri.fromFile(source))
            withTimeout(5_000) {
                reopened.observeState().first {
                    it.operationMessage == "A save operation is already running for this game"
                }
            }
            allowRecordWrite.complete(Unit)
            withTimeout(5_000) { oldScope.coroutineContext[Job]!!.join() }
            assertFalse(first.observeBusy().value)
            withTimeout(5_000) { reopened.observeState().first { it.saveRecordPresent } }
            assertEquals("Synthetic lifecycle save", root.resolve("saves/lifecycle-game/save.dat").readText())
            reopened.backupSave()
            withTimeout(5_000) { reopened.observeState().first { it.operationMessage == "Backup completed" } }
            withTimeout(5_000) { reopened.observeBusy().first { !it } }
        } finally {
            allowRecordWrite.complete(Unit)
            oldScope.coroutineContext[Job]?.cancelAndJoin()
            newScope.coroutineContext[Job]?.cancelAndJoin()
            database.close()
            root.deleteRecursively()
        }
    }
}


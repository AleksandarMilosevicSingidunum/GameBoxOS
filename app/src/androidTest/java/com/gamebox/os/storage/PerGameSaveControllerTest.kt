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
            assertFalse(second.observeState().value.saveRecordPresent)
            source.writeText("SECOND synthetic save")
            second.importInitialSave(Uri.fromFile(source))
            withTimeout(5_000) { second.observeState().first { it.saveRecordPresent } }
            first.backupSave()
            withTimeout(5_000) { first.observeState().first { it.operationMessage == "Backup completed" } }
            val firstFile = root.resolve("saves/first-game/save.dat")
            firstFile.writeText("CHANGED")
            first.restoreSave()
            withTimeout(5_000) { first.observeState().first { it.operationMessage == "Restore completed" } }
            assertEquals("FIRST synthetic save", firstFile.readText())
            assertEquals("SECOND synthetic save", root.resolve("saves/second-game/save.dat").readText())
            assertFalse(root.resolve("save-backups/second-game/save.dat").exists())
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
}


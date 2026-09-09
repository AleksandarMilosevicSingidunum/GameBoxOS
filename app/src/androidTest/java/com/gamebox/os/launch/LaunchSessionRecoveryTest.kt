package com.gamebox.os.launch

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.data.local.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Real Room transactions/reopen; not evidence that an emulator executed a game. */
@RunWith(AndroidJUnit4::class)
class LaunchSessionRecoveryTest {
    private fun game() = GameEntity("session-test", "Session test", "NES", 2026, "Homebrew", 1,
        "INSTALLED", null, 10, favorite = true)

    @Test fun reopeningRecoversConfirmedSessionExactlyOnce(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "session-recovery-${UUID.randomUUID()}.db"
        var database: GameBoxDatabase? = null
        try {
            val first = Room.databaseBuilder(context, GameBoxDatabase::class.java, name).build()
            database = first
            first.gameDao().upsert(game())
            val journal = RoomLaunchSessionJournal(first.launchSessionDao()) { 1_000L }
            journal.confirm(journal.begin("session-test"))
            first.close()
            val reopened = Room.databaseBuilder(context, GameBoxDatabase::class.java, name).build()
            database = reopened
            val recovered = RoomLaunchSessionJournal(reopened.launchSessionDao()) { 126_000L }
            assertEquals(2, recovered.finish()?.minutesAway)
            assertNull(recovered.finish())
            assertNull(reopened.launchSessionDao().pending())
            val saved = requireNotNull(reopened.gameDao().getById("session-test"))
            assertEquals(12, saved.minutesPlayed)
            assertTrue(saved.favorite)
            assertEquals("INSTALLED", saved.installState)
        } finally {
            database?.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun interruptedHandoffDoesNotInventPlaytimeAndPendingCannotBeOverwritten(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, GameBoxDatabase::class.java).build()
        try {
            database.gameDao().upsert(game())
            val journal = RoomLaunchSessionJournal(database.launchSessionDao()) { 1_000L }
            journal.begin("session-test")
            var rejected = false
            try { journal.begin("different-game") } catch (_: Exception) { rejected = true }
            assertTrue(rejected)
            val result = requireNotNull(journal.finish())
            assertEquals("session-test", result.gameId)
            assertFalse(result.launchConfirmed)
            assertEquals(10, database.gameDao().getById("session-test")?.minutesPlayed)
            assertNull(database.gameDao().getById("session-test")?.lastPlayed)
            val retry = journal.begin("session-test")
            journal.abandon(retry)
            assertNull(journal.finish())
        } finally { database.close() }
    }

    @Test fun versionElevenStructureMigratesWithoutChangingGameData(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "session-migration-${UUID.randomUUID()}.db"
        var database: GameBoxDatabase? = null
        try {
            val initial = Room.databaseBuilder(context, GameBoxDatabase::class.java, name).build()
            database = initial
            initial.gameDao().upsert(game())
            initial.close()
            // Version 12 changes only this table. Recreate the preceding structure;
            // this is not a substitute for a full released-schema migration matrix.
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("DROP TABLE pending_launch_session")
                it.version = 11
            }
            val migrated = Room.databaseBuilder(context, GameBoxDatabase::class.java, name)
                .addMigrations(MIGRATION_11_12).build()
            database = migrated
            assertEquals(game(), migrated.gameDao().getById("session-test"))
            assertNull(migrated.launchSessionDao().pending())
            val journal = RoomLaunchSessionJournal(migrated.launchSessionDao()) { 1_000L }
            journal.confirm(journal.begin("session-test"))
            assertTrue(requireNotNull(journal.finish()).launchConfirmed)
        } finally {
            database?.close()
            context.deleteDatabase(name)
        }
    }
}

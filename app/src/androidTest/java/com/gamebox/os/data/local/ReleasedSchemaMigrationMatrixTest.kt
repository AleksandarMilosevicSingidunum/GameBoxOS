package com.gamebox.os.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReleasedSchemaMigrationMatrixTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val migrations: List<Migration> = listOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
    )

    @Test
    fun everyReleasedSchemaUpgradesToCurrentWithoutDataLoss() {
        for (startVersion in 1 until GAMEBOX_DATABASE_VERSION) {
            val name = "migration-matrix-${startVersion}.db"
            context.deleteDatabase(name)
            createReleasedSchema(name, startVersion)

            val room = Room.databaseBuilder(context, GameBoxDatabase::class.java, name)
                .addMigrations(*migrations.toTypedArray())
                .build()
            try {
                val database = room.openHelper.writableDatabase
                database.query(
                    """
                    SELECT title, favorite, graphicsProfile, localContentFilesJson
                    FROM games WHERE id = ?
                    """.trimIndent(),
                    arrayOf("sentinel-${startVersion}"),
                ).use { cursor ->
                    assertTrue("sentinel missing after v$startVersion migration", cursor.moveToFirst())
                    assertEquals("Migration sentinel", cursor.getString(0))
                    assertEquals(0, cursor.getInt(1))
                    assertEquals("Balanced", cursor.getString(2))
                    assertTrue(cursor.isNull(3))
                }
                assertEquals(
                    GAMEBOX_DATABASE_VERSION,
                    database.query("PRAGMA user_version").use {
                        assertTrue(it.moveToFirst())
                        it.getInt(0)
                    },
                )
                assertCurrentTablesExist(database)
            } finally {
                room.close()
                context.deleteDatabase(name)
            }
        }
    }

    private fun createReleasedSchema(name: String, version: Int) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(database: SupportSQLiteDatabase) {
                        createVersionOne(database)
                        migrations
                            .filter { it.endVersion <= version }
                            .forEach { it.migrate(database) }
                    }

                    override fun onUpgrade(
                        database: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = error("Unexpected helper upgrade $oldVersion -> $newVersion")
                })
                .build()
        )
        try {
            helper.writableDatabase.execSQL(
                """
                INSERT INTO games (
                    id, title, platform, year, genre, sizeMb,
                    installState, lastPlayed, minutesPlayed
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    "sentinel-${version}",
                    "Migration sentinel",
                    "Retro",
                    2026,
                    "Test",
                    1,
                    "INSTALLED",
                    null,
                    37,
                ),
            )
        } finally {
            helper.close()
        }
    }

    private fun createVersionOne(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS games (
                id TEXT NOT NULL,
                title TEXT NOT NULL,
                platform TEXT NOT NULL,
                year INTEGER NOT NULL,
                genre TEXT NOT NULL,
                sizeMb INTEGER NOT NULL,
                installState TEXT NOT NULL,
                lastPlayed TEXT,
                minutesPlayed INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )
    }

    private fun assertCurrentTablesExist(database: SupportSQLiteDatabase) {
        val expected = setOf(
            "games",
            "download_jobs",
            "save_records",
            "catalog_platforms",
            "catalog_games",
            "catalog_external_ids",
            "pending_launch_session",
        )
        val actual = mutableSetOf<String>()
        database.query(
            "SELECT name FROM sqlite_master WHERE type = 'table'"
        ).use { cursor ->
            while (cursor.moveToNext()) actual += cursor.getString(0)
        }
        assertTrue("missing current tables: ${expected - actual}", actual.containsAll(expected))
    }
}

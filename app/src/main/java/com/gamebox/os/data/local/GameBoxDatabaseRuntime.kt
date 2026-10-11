package com.gamebox.os.data.local

import android.content.Context
import androidx.room.Room

/** Creates the durable Room store, preserving the pre-migration backup boundary. */
internal class GameBoxDatabaseRuntime(context: Context) {
    private val applicationContext = context.applicationContext
    private val databaseName = "gamebox.db"
    private val preMigrationBackup = DatabaseMigrationBackupManager(
        applicationContext.filesDir.resolve("database-backups"),
    ).createIfNeeded(
        applicationContext.getDatabasePath(databaseName),
        GAMEBOX_DATABASE_VERSION,
    )

    val database: GameBoxDatabase = Room.databaseBuilder(
        applicationContext,
        GameBoxDatabase::class.java,
        databaseName,
    ).addMigrations(
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
        MIGRATION_12_13,
        MIGRATION_13_14,
    ).build()
}

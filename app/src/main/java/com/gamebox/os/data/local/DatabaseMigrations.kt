package com.gamebox.os.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS download_jobs (
                id TEXT NOT NULL PRIMARY KEY,
                gameId TEXT NOT NULL,
                title TEXT NOT NULL,
                status TEXT NOT NULL,
                totalBytes INTEGER NOT NULL,
                downloadedBytes INTEGER NOT NULL,
                errorReason TEXT
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS save_records (
                gameId TEXT NOT NULL PRIMARY KEY,
                relativePath TEXT NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                sizeBytes INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}


val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE games ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0"
        )
    }
}

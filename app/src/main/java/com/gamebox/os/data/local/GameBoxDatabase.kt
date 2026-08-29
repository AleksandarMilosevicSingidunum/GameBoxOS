package com.gamebox.os.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameEntity::class, DownloadJobEntity::class, SaveRecordEntity::class],
    version = 6,
    exportSchema = true
)
abstract class GameBoxDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao()
    abstract fun downloadJobDao(): DownloadJobDao()
    abstract fun saveRecordDao(): SaveRecordDao()
}

package com.gamebox.os.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameEntity::class],
    version = 1,
    exportSchema = true
)
abstract class GameBoxDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}

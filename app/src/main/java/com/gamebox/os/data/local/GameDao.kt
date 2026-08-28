package com.gamebox.os.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games")
    suspend fun getAllOnce(): List<GameEntity>

    @Query("SELECT COUNT(*) FROM games")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(games: List<GameEntity>)

    @Query("UPDATE games SET installState = :state WHERE id = :id")
    suspend fun updateInstallState(id: String, state: String)

    @Query("""
        UPDATE games
        SET lastPlayed = :lastPlayed,
            minutesPlayed = minutesPlayed + :additionalMinutes
        WHERE id = :id
    """)
    suspend fun recordPlaySession(id: String, lastPlayed: String, additionalMinutes: Int)
}

package com.gamebox.os.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val title: String,
    val platform: String,
    val year: Int,
    val genre: String,
    val sizeMb: Int,
    val installState: String,
    val lastPlayed: String?,
    val minutesPlayed: Int
)

fun GameEntity.toDomain(): Game = Game(
    id = GameId(id),
    title = title,
    platform = platform,
    year = year,
    genre = genre,
    sizeMb = sizeMb,
    state = runCatching { InstallState.valueOf(installState) }.getOrDefault(InstallState.FAILED),
    lastPlayed = lastPlayed,
    minutesPlayed = minutesPlayed
)

fun Game.toEntity(): GameEntity = GameEntity(
    id = id.value,
    title = title,
    platform = platform,
    year = year,
    genre = genre,
    sizeMb = sizeMb,
    installState = state.name,
    lastPlayed = lastPlayed,
    minutesPlayed = minutesPlayed
)

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
    val minutesPlayed: Int,
    val favorite: Boolean = false,
    val sourceUrl: String? = null,
    val expectedSha256: String? = null,
    val emulatorPackage: String? = null,
    val graphicsProfile: String = "Balanced",
    val artworkUrl: String? = null,
    val description: String? = null,
    val players: String? = null,
    val language: String? = null,
    val region: String? = null
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
    minutesPlayed = minutesPlayed,
    favorite = favorite,
    sourceUrl = sourceUrl,
    expectedSha256 = expectedSha256,
    emulatorPackage = emulatorPackage,
    graphicsProfile = graphicsProfile,
    artworkUrl = artworkUrl,
    description = description,
    players = players,
    language = language,
    region = region
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
    minutesPlayed = minutesPlayed,
    favorite = favorite,
    sourceUrl = sourceUrl,
    expectedSha256 = expectedSha256,
    emulatorPackage = emulatorPackage,
    graphicsProfile = graphicsProfile,
    artworkUrl = artworkUrl,
    description = description,
    players = players,
    language = language,
    region = region
)

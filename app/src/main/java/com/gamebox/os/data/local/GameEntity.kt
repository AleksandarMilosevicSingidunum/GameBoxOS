package com.gamebox.os.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.LocalContentFile

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
    val region: String? = null,
    val localContentRelativePath: String? = null,
    val localContentSha256: String? = null,
    val localContentMimeType: String? = null,
    val localContentFilesJson: String? = null,
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
    region = region,
    localContentRelativePath = localContentRelativePath,
    localContentSha256 = localContentSha256,
    localContentMimeType = localContentMimeType,
    localContentFiles = decodeLocalContentFiles(localContentFilesJson),
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
    region = region,
    localContentRelativePath = localContentRelativePath,
    localContentSha256 = localContentSha256,
    localContentMimeType = localContentMimeType,
    localContentFilesJson = encodeLocalContentFiles(localContentFiles),
)

private fun encodeLocalContentFiles(files: List<LocalContentFile>): String? =
    if (files.isEmpty()) null else files.joinToString("\n") { file ->
        listOf(file.relativePath, file.sha256, file.mimeType).joinToString("\t")
    }

private fun decodeLocalContentFiles(value: String?): List<LocalContentFile> = value.orEmpty()
    .lineSequence()
    .filter(String::isNotBlank)
    .mapNotNull { line ->
        val fields = line.split('\t')
        if (fields.size != 3) null else runCatching {
            LocalContentFile(fields[0], fields[1], fields[2])
        }.getOrNull()
    }
    .toList()

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

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GameEntity?

    @Query("SELECT COUNT(*) FROM games")
    suspend fun count(): Int

    @Query("""
        UPDATE games SET installState = 'NOT_INSTALLED'
        WHERE id IN ('celeste', 'cave-story', 'openarena', 'supertuxkart', 'luanti', 'openmw')
          AND installState IN ('INSTALLED', 'UPDATE_AVAILABLE', 'QUEUED', 'PAUSED')
          AND localContentRelativePath IS NULL AND localContentSha256 IS NULL
          AND localContentFilesJson IS NULL
          AND sourceUrl IS NULL AND expectedSha256 IS NULL
    """)
    suspend fun clearLegacyCatalogInstallClaims(): Int

    @Upsert
    suspend fun upsertAll(games: List<GameEntity>)

    @Upsert
    suspend fun upsert(game: GameEntity)

    @Query("UPDATE games SET favorite = :favorite WHERE id = :id")
    suspend fun updateFavorite(id: String, favorite: Boolean)

    @Query("UPDATE games SET installState = :state WHERE id = :id")
    suspend fun updateInstallState(id: String, state: String)

    @Query("""
        UPDATE games
        SET installState = 'NOT_INSTALLED',
            localContentRelativePath = NULL,
            localContentSha256 = NULL,
            localContentMimeType = NULL,
            localContentFilesJson = NULL
        WHERE id = :id
          AND installState = 'MISSING_FILES'
          AND localContentRelativePath IS NOT NULL
    """)
    suspend fun forgetMissingImportedContent(id: String): Int

    @Query("UPDATE games SET emulatorPackage = :packageName, graphicsProfile = :profile WHERE id = :id")
    suspend fun updateEmulatorSettings(id: String, packageName: String?, profile: String)

    @Query("""
        UPDATE games
        SET userTitle = :title,
            userYear = :year,
            userGenre = :genre,
            userArtworkUrl = :artworkUrl,
            userDescription = :description
        WHERE id = :id
    """)
    suspend fun updateMetadataOverrides(
        id: String,
        title: String?,
        year: Int?,
        genre: String?,
        artworkUrl: String?,
        description: String?,
    ): Int

    @Query("""
        UPDATE games
        SET title = :title,
            year = COALESCE(:year, year),
            genre = COALESCE(:genre, genre),
            artworkUrl = COALESCE(:artworkUrl, artworkUrl),
            description = COALESCE(:description, description),
            metadataProvider = :provider,
            metadataExternalId = :externalId,
            metadataMatchedAtMillis = :matchedAtMillis
        WHERE id = :id
    """)
    suspend fun applyProviderMetadataMatch(
        id: String,
        provider: String,
        externalId: String,
        title: String,
        year: Int?,
        genre: String?,
        artworkUrl: String?,
        description: String?,
        matchedAtMillis: Long,
    ): Int

    @Query("""
        UPDATE games
        SET lastPlayed = :lastPlayed,
            minutesPlayed = minutesPlayed + :additionalMinutes
        WHERE id = :id
    """)
    suspend fun recordPlaySession(id: String, lastPlayed: String, additionalMinutes: Int)
}

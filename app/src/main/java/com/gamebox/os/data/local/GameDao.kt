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
        SET year = CASE WHEN year = :expectedYear THEN COALESCE(:year, year) ELSE year END,
            genre = CASE WHEN genre = :expectedGenre THEN COALESCE(:genre, genre) ELSE genre END,
            artworkUrl = CASE WHEN artworkUrl IS :expectedArtworkUrl THEN COALESCE(:artworkUrl, artworkUrl) ELSE artworkUrl END,
            description = CASE WHEN description IS :expectedDescription THEN COALESCE(:description, description) ELSE description END,
            metadataProvider = CASE WHEN metadataProvider IS :expectedProvider THEN COALESCE(:provider, metadataProvider) ELSE metadataProvider END,
            metadataExternalId = CASE WHEN metadataExternalId IS :expectedExternalId THEN COALESCE(:externalId, metadataExternalId) ELSE metadataExternalId END,
            metadataMatchedAtMillis = CASE WHEN metadataMatchedAtMillis IS :expectedMatchedAtMillis THEN COALESCE(:matchedAtMillis, metadataMatchedAtMillis) ELSE metadataMatchedAtMillis END
        WHERE id = :id
          AND metadataProvider IS :expectedProvider
          AND metadataExternalId IS :expectedExternalId
          AND metadataMatchedAtMillis IS :expectedMatchedAtMillis
    """)
    suspend fun applyMetadataEnrichmentIfUnchanged(
        id: String,
        expectedYear: Int,
        expectedGenre: String,
        expectedArtworkUrl: String?,
        expectedDescription: String?,
        expectedProvider: String?,
        expectedExternalId: String?,
        expectedMatchedAtMillis: Long?,
        year: Int?,
        genre: String?,
        artworkUrl: String?,
        description: String?,
        provider: String?,
        externalId: String?,
        matchedAtMillis: Long?,
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

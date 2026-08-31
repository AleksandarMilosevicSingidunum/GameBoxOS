package com.gamebox.os.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "catalog_platforms",
    primaryKeys = ["id"],
    indices = [Index(value = ["name"])]
)
data class CatalogPlatformEntity(
    val id: String,
    val name: String,
    val theGamesDbId: String?,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "catalog_games",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["platformId"]),
        Index(value = ["normalizedTitle"]),
        Index(value = ["platformId", "normalizedTitle"]),
    ]
)
data class CatalogGameEntity(
    val id: String,
    val title: String,
    val normalizedTitle: String,
    val platformId: String,
    val region: String?,
    val releaseDate: String?,
    val description: String?,
    val developer: String?,
    val publisher: String?,
    val players: String?,
    val rating: Double?,
    val coverUrl: String?,
    val backgroundUrl: String?,
    val logoUrl: String?,
    val screenshotsJson: String?,
    val favorite: Boolean,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "catalog_external_ids",
    primaryKeys = ["gameId", "provider"],
    indices = [Index(value = ["provider", "externalId"], unique = true)]
)
data class CatalogExternalIdEntity(
    val gameId: String,
    val provider: String,
    val externalId: String,
)

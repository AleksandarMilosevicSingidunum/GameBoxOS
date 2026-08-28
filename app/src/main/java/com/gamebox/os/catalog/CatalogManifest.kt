package com.gamebox.os.catalog

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CatalogManifest(
    val schemaVersion: Int,
    val provider: CatalogProviderInfo,
    val games: List<CatalogGame>
)

@Serializable
data class CatalogProviderInfo(
    val id: String,
    val displayName: String
)

@Serializable
data class CatalogGame(
    val id: String,
    val title: String,
    val platform: String,
    val year: Int,
    val genre: String,
    val sizeMb: Int,
    val contentPolicy: String,
    val initialState: String = InstallState.NOT_INSTALLED.name,
    val source: String? = null,
    val checksum: String? = null
)

data class CatalogSnapshot(
    val providerId: String,
    val providerName: String,
    val games: List<Game>
)

class CatalogFormatException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

class CatalogParser(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun parse(input: String): CatalogSnapshot {
        val manifest = try {
            json.decodeFromString<CatalogManifest>(input)
        } catch (error: Exception) {
            throw CatalogFormatException("Catalog JSON is malformed", error)
        }

        if (manifest.schemaVersion != SUPPORTED_SCHEMA) {
            throw CatalogFormatException("Unsupported catalog schema: " + manifest.schemaVersion)
        }
        if (manifest.provider.id.isBlank() || manifest.provider.displayName.isBlank()) {
            throw CatalogFormatException("Catalog provider identity is required")
        }
        if (manifest.games.map { it.id }.distinct().size != manifest.games.size) {
            throw CatalogFormatException("Catalog game IDs must be unique")
        }

        val games = manifest.games.map { item ->
            if (item.id.isBlank() || item.title.isBlank() || item.platform.isBlank()) {
                throw CatalogFormatException("Game identity, title, and platform are required")
            }
            if (item.sizeMb < 0) throw CatalogFormatException("Game size cannot be negative")
            val state = runCatching { InstallState.valueOf(item.initialState) }
                .getOrElse { throw CatalogFormatException("Unknown install state: " + item.initialState) }
            Game(
                id = GameId(item.id),
                title = item.title,
                platform = item.platform,
                year = item.year,
                genre = item.genre,
                sizeMb = item.sizeMb,
                state = state
            )
        }

        return CatalogSnapshot(manifest.provider.id, manifest.provider.displayName, games)
    }

    private companion object {
        const val SUPPORTED_SCHEMA = 1
    }
}

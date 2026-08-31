package com.gamebox.os.catalog

import com.gamebox.os.data.local.CatalogDiscoveryDao
import com.gamebox.os.data.local.CatalogExternalIdEntity
import com.gamebox.os.data.local.CatalogGameEntity
import com.gamebox.os.data.local.CatalogPlatformEntity
import com.gamebox.os.domain.MetadataProviderId
import com.gamebox.os.domain.normalizeCatalogTitle
import java.net.URI

fun interface TheGamesDbCatalogTransport {
    suspend fun get(uri: URI): String
}

sealed interface CatalogSyncResult {
    data class Success(val platformId: String, val pages: Int, val games: Int) : CatalogSyncResult
    data object MissingApiKey : CatalogSyncResult
    data class PlatformNotFound(val requestedName: String) : CatalogSyncResult
    data class Failed(val reason: String) : CatalogSyncResult
}

/**
 * Synchronizes metadata only. It never creates acquisition sources or install records.
 */
class TheGamesDbCatalogSync(
    private val apiKey: suspend () -> String?,
    private val transport: TheGamesDbCatalogTransport,
    private val dao: CatalogDiscoveryDao,
    private val maxPagesPerRun: Int = 100,
    private val maxGamesPerPlatform: Int = 20,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    init {
        require(maxPagesPerRun in 1..1_000)
        require(maxGamesPerPlatform in 1..500)
    }

    suspend fun syncPlatform(platformName: String): CatalogSyncResult {
        val key = apiKey()?.trim().orEmpty()
        if (key.isEmpty()) return CatalogSyncResult.MissingApiKey
        val requested = normalizeCatalogTitle(platformName)
        if (requested.isEmpty()) return CatalogSyncResult.PlatformNotFound(platformName)
        return runCatching {
            val platformsPayload = transport.get(TheGamesDbCatalogRequest.platforms(key))
            val platform = TheGamesDbCatalogParser.parsePlatforms(platformsPayload)
                .firstOrNull { it.id == requested }
                ?: return CatalogSyncResult.PlatformNotFound(platformName)
            val providerPlatformId = platform.externalIds[MetadataProviderId.THE_GAMES_DB]
                ?: return CatalogSyncResult.PlatformNotFound(platformName)

            var pageNumber = 1
            var pageCount = 0
            var gameCount = 0
            val visitedPages = mutableSetOf<Int>()
            while (pageCount < maxPagesPerRun && visitedPages.add(pageNumber)) {
                val payload = transport.get(
                    TheGamesDbCatalogRequest.gamesByPlatform(key, providerPlatformId, pageNumber)
                )
                val page = TheGamesDbCatalogParser.parsePlatformPage(payload, platform)
                val updatedAt = nowMillis()
                val pageGames = page.games.take(maxGamesPerPlatform - gameCount)
                dao.upsertPage(
                    platform = CatalogPlatformEntity(
                        id = platform.id,
                        name = platform.name,
                        theGamesDbId = providerPlatformId,
                        updatedAtMillis = updatedAt,
                    ),
                    games = pageGames.map { game ->
                        CatalogGameEntity(
                            id = game.id.value,
                            title = game.title,
                            normalizedTitle = game.normalizedTitle,
                            platformId = platform.id,
                            region = game.region,
                            releaseDate = game.releaseDate,
                            description = game.description,
                            developer = game.developer,
                            publisher = game.publisher,
                            players = game.players,
                            rating = game.rating,
                            coverUrl = game.media.cover,
                            backgroundUrl = game.media.background,
                            logoUrl = game.media.logo,
                            screenshotsJson = game.media.screenshots.take(12).joinToString("\n").ifBlank { null },
                            favorite = false,
                            updatedAtMillis = updatedAt,
                        )
                    },
                    externalIds = pageGames.mapNotNull { game ->
                        game.externalIds[MetadataProviderId.THE_GAMES_DB]?.let { externalId ->
                            CatalogExternalIdEntity(
                                gameId = game.id.value,
                                provider = MetadataProviderId.THE_GAMES_DB.name,
                                externalId = externalId,
                            )
                        }
                    },
                )
                pageCount += 1
                gameCount += pageGames.size
                if (gameCount >= maxGamesPerPlatform) break
                pageNumber = page.nextPage ?: break
            }
            CatalogSyncResult.Success(platform.id, pageCount, gameCount)
        }.getOrElse { error ->
            CatalogSyncResult.Failed(error.message?.take(200) ?: "Catalog synchronization failed")
        }
    }
}

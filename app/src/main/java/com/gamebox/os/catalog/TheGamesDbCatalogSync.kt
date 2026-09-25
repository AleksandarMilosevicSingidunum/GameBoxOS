package com.gamebox.os.catalog

import com.gamebox.os.data.local.CatalogDiscoveryDao
import com.gamebox.os.data.local.CatalogExternalIdEntity
import com.gamebox.os.data.local.CatalogGameEntity
import com.gamebox.os.data.local.CatalogPlatformEntity
import com.gamebox.os.domain.MetadataProviderId
import com.gamebox.os.domain.normalizeCatalogTitle
import com.gamebox.os.provider.ProviderFailureKind
import com.gamebox.os.provider.ProviderRecoveryPolicy
import java.net.URI

fun interface TheGamesDbCatalogTransport {
    suspend fun get(uri: URI): String
}

sealed interface CatalogSyncResult {
    data class Success(val platformId: String, val pages: Int, val games: Int) : CatalogSyncResult
    data object MissingApiKey : CatalogSyncResult
    data class PlatformNotFound(val requestedName: String) : CatalogSyncResult
    data class Failed(
        val reason: String,
        val kind: ProviderFailureKind = ProviderFailureKind.PERMANENT,
        val retryAfterMillis: Long? = null,
    ) : CatalogSyncResult
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
    private val onHealthChanged: suspend (ProviderHealth) -> Unit = {},
) {
    init {
        require(maxPagesPerRun in 1..1_000)
        require(maxGamesPerPlatform in 1..500)
    }

    suspend fun syncPlatform(platformName: String): CatalogSyncResult {
        val attemptedAt = nowMillis()
        val key = apiKey()?.trim().orEmpty()
        if (key.isEmpty()) {
            onHealthChanged(
                ProviderHealth(
                    status = ProviderHealthStatus.NOT_CONFIGURED,
                    lastAttemptAtMillis = attemptedAt,
                    message = "Add an API key in Settings",
                )
            )
            return CatalogSyncResult.MissingApiKey
        }
        val requested = normalizeCatalogTitle(platformName)
        if (requested.isEmpty()) {
            onHealthChanged(
                ProviderHealth(
                    status = ProviderHealthStatus.DEGRADED,
                    lastAttemptAtMillis = attemptedAt,
                    message = "The requested platform name is empty",
                )
            )
            return CatalogSyncResult.PlatformNotFound(platformName)
        }
        return runCatching {
            val platformsPayload = transport.get(TheGamesDbCatalogRequest.platforms(key))
            val platform = TheGamesDbCatalogParser.parsePlatforms(platformsPayload)
                .firstOrNull { it.matchesRequestedName(requested) }
                ?: run {
                    onHealthChanged(
                        ProviderHealth(
                            status = ProviderHealthStatus.DEGRADED,
                            lastAttemptAtMillis = attemptedAt,
                            latencyMillis = (nowMillis() - attemptedAt).coerceAtLeast(0L),
                            message = "Platform was not found by TheGamesDB",
                        )
                    )
                    return CatalogSyncResult.PlatformNotFound(platformName)
                }
            val providerPlatformId = platform.externalIds[MetadataProviderId.THE_GAMES_DB]
                ?: run {
                    onHealthChanged(
                        ProviderHealth(
                            status = ProviderHealthStatus.DEGRADED,
                            lastAttemptAtMillis = attemptedAt,
                            latencyMillis = (nowMillis() - attemptedAt).coerceAtLeast(0L),
                            message = "Platform has no TheGamesDB identifier",
                        )
                    )
                    return CatalogSyncResult.PlatformNotFound(platformName)
                }

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
                val favoriteIds = if (pageGames.isEmpty()) emptySet() else
                    dao.favoriteGameIds(pageGames.map { it.id.value }).toSet()
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
                            favorite = game.id.value in favoriteIds,
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
            val completedAt = nowMillis()
            onHealthChanged(
                ProviderHealth(
                    status = ProviderHealthStatus.HEALTHY,
                    lastAttemptAtMillis = attemptedAt,
                    lastSuccessAtMillis = completedAt,
                    latencyMillis = (completedAt - attemptedAt).coerceAtLeast(0L),
                    message = "Synchronized $gameCount games",
                )
            )
            CatalogSyncResult.Success(platform.id, pageCount, gameCount)
        }.getOrElse { error ->
            val transportFailure = error as? TheGamesDbTransportException
            val decision = transportFailure?.let {
                ProviderRecoveryPolicy.classify(it.httpStatus, it)
            } ?: ProviderRecoveryPolicy.classify(null, error)
            val reason = (error.message ?: decision.userMessage).take(200)
            val retryAfter = transportFailure?.retryAfterMillis
                ?: decision.delayMillis.takeIf { it > 0L }?.let { nowMillis() + it }
            onHealthChanged(
                ProviderHealth(
                    status = decision.kind.toProviderHealthStatus(),
                    lastAttemptAtMillis = attemptedAt,
                    latencyMillis = (nowMillis() - attemptedAt).coerceAtLeast(0L),
                    retryAfterMillis = retryAfter,
                    message = reason,
                )
            )
            CatalogSyncResult.Failed(reason, decision.kind, retryAfter)
        }
    }

    /**
     * TheGamesDB names Sony platforms with a vendor prefix (for example,
     * "Sony Playstation 2"), while the provider-neutral id intentionally omits
     * that prefix. Accept both forms so UI labels and API platform names resolve
     * to the same numeric provider id.
     */
    private fun com.gamebox.os.domain.CatalogPlatform.matchesRequestedName(requested: String): Boolean {
        val normalizedName = normalizeCatalogTitle(name)
        return id == requested || normalizedName == requested ||
            normalizedName.removePrefix("sony") == requested ||
            id == requested.removePrefix("sony")
    }
}


package com.gamebox.os.catalog

import com.gamebox.os.domain.Game

/** Enriches an authorized catalog without letting metadata failures hide playable entries. */
class MetadataEnrichingCatalogProvider(
    private val base: CatalogProvider,
    private val enrich: suspend (Game) -> Game,
) : CatalogProvider, CatalogFallbackStatus {
    override val capabilities: CatalogProviderCapabilities
        get() = base.capabilities

    override suspend fun testConnection(): CatalogConnectionResult = base.testConnection()

    override suspend fun load(): CatalogSnapshot = enrichSnapshot(base.load())

    override suspend fun refresh(): CatalogSnapshot = enrichSnapshot(base.refresh())

    private suspend fun enrichSnapshot(snapshot: CatalogSnapshot): CatalogSnapshot {
        val enriched = snapshot.games.map { game ->
            try {
                enrich(game)
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                game
            }
        }
        return snapshot.copy(games = enriched)
    }

    override fun consumeFallbackReason(): CatalogFallbackReason =
        (base as? CatalogFallbackStatus)?.consumeFallbackReason()
            ?: CatalogFallbackReason.NONE
}

package com.gamebox.os.catalog

import android.content.Context
import com.gamebox.os.domain.GameId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CatalogProviderCapabilities(
    val refresh: Boolean = true,
    val connectionTest: Boolean = true,
    val sourceResolution: Boolean = true,
)

data class CatalogConnectionResult(
    val success: Boolean,
    val providerId: String? = null,
    val providerName: String? = null,
    val gameCount: Int = 0,
    val message: String,
)

data class CatalogSourceResolution(
    val gameId: GameId,
    val sourceUrl: String?,
    val expectedSha256: String?,
) {
    val downloadable: Boolean
        get() = sourceUrl != null && expectedSha256 != null
}

interface CatalogProvider {
    val capabilities: CatalogProviderCapabilities
        get() = CatalogProviderCapabilities()

    suspend fun load(): CatalogSnapshot

    /** Forces a provider refresh. Implementations with caches must attempt their remote source first. */
    suspend fun refresh(): CatalogSnapshot = load()

    /** Executes the same authenticated, bounded production request used by refresh. */
    suspend fun testConnection(): CatalogConnectionResult = try {
        val snapshot = refresh()
        CatalogConnectionResult(
            success = true,
            providerId = snapshot.providerId,
            providerName = snapshot.providerName,
            gameCount = snapshot.games.size,
            message = "Connected to ${snapshot.providerName} (${snapshot.games.size} games)",
        )
    } catch (error: Exception) {
        if (error is kotlinx.coroutines.CancellationException) throw error
        CatalogConnectionResult(
            success = false,
            message = error.message?.take(180) ?: "Catalog connection failed",
        )
    }

    /** Resolves an authorized binary source without treating metadata-only entries as downloadable. */
    suspend fun resolveSource(gameId: GameId): CatalogSourceResolution {
        val game = load().games.firstOrNull { it.id == gameId }
        return CatalogSourceResolution(gameId, game?.sourceUrl, game?.expectedSha256)
    }
}

class SelectingCatalogProvider(
    private val selected: suspend () -> String,
    providers: Map<String, CatalogProvider>,
) : CatalogProvider {
    private val providers = providers.mapKeys { it.key.uppercase() }

    private suspend fun delegate(): CatalogProvider =
        providers[selected().uppercase()]
            ?: throw IllegalArgumentException("Unsupported catalog transport")

    override val capabilities: CatalogProviderCapabilities
        get() = CatalogProviderCapabilities()

    override suspend fun load(): CatalogSnapshot = delegate().load()
    override suspend fun refresh(): CatalogSnapshot = delegate().refresh()
    override suspend fun testConnection(): CatalogConnectionResult = delegate().testConnection()
    override suspend fun resolveSource(gameId: GameId): CatalogSourceResolution =
        delegate().resolveSource(gameId)
}

enum class CatalogFallbackReason {
    NONE, OFFLINE, REMOTE_FAILURE
}

/**
 * Optional status contract for provider decorators. The reason is consumed after each load
 * so a previous degraded refresh cannot leak into a later successful one.
 */
interface CatalogFallbackStatus {
    fun consumeFallbackReason(): CatalogFallbackReason
}

class AssetCatalogProvider(
    private val context: Context,
    private val parser: CatalogParser = CatalogParser(),
    private val assetPath: String = "catalog/authorized-fixture.json"
) : CatalogProvider {
    override suspend fun load(): CatalogSnapshot = withContext(Dispatchers.IO) {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        parser.parse(text)
    }
}

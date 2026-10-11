package com.gamebox.os.catalog

/** Selects the configured remote catalog while preserving the bundled catalog as fallback. */
class ConfiguredCatalogProvider(
    private val fallback: CatalogProvider,
    private val remote: CatalogProvider,
    private val configuredUrl: suspend () -> String,
    private val networkAvailable: suspend () -> Boolean = { true },
) : CatalogProvider, CatalogFallbackStatus {
    private val fallbackReason = java.util.concurrent.atomic.AtomicReference(CatalogFallbackReason.NONE)

    override suspend fun load(): CatalogSnapshot = loadSelected(forceRefresh = false)

    override suspend fun refresh(): CatalogSnapshot = loadSelected(forceRefresh = true)

    private suspend fun loadSelected(forceRefresh: Boolean): CatalogSnapshot {
        val url = configuredUrl()
        if (url.isBlank()) {
            fallbackReason.set(CatalogFallbackReason.NONE)
            return fallback.load()
        }
        if (!networkAvailable()) {
            fallbackReason.set(CatalogFallbackReason.OFFLINE)
            return fallback.load()
        }
        return try {
            val snapshot = if (forceRefresh) remote.refresh() else remote.load()
            fallbackReason.set(CatalogFallbackReason.NONE)
            snapshot
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            fallbackReason.set(CatalogFallbackReason.REMOTE_FAILURE)
            fallback.load()
        }
    }

    override suspend fun testConnection(): CatalogConnectionResult {
        val url = configuredUrl()
        if (url.isBlank()) {
            return fallback.testConnection().copy(message = "Bundled offline catalog is available")
        }
        if (!networkAvailable()) {
            return CatalogConnectionResult(false, message = "No validated internet connection")
        }
        return remote.testConnection()
    }

    override fun consumeFallbackReason(): CatalogFallbackReason =
        fallbackReason.getAndSet(CatalogFallbackReason.NONE)
}

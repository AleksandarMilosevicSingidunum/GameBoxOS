package com.gamebox.os.catalog

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfiguredCatalogProviderTest {
    private val fallback = CatalogSnapshot("fallback", "Offline", emptyList())
    private val remote = CatalogSnapshot("remote", "Remote", emptyList())

    @Test
    fun offlineUsesFallbackWithoutRemoteRequest() = runBlocking {
        var remoteCalls = 0
        val provider = ConfiguredCatalogProvider(
            fallback = object : CatalogProvider { override suspend fun load() = fallback },
            remote = object : CatalogProvider { override suspend fun load(): CatalogSnapshot { remoteCalls++; return remote } },
            configuredUrl = { "https://catalog.example/games.json" },
            networkAvailable = { false }
        )
        assertEquals(fallback, provider.load())
        assertEquals(0, remoteCalls)
    }

    @Test
    fun onlineUsesConfiguredRemote() = runBlocking {
        val provider = ConfiguredCatalogProvider(
            fallback = object : CatalogProvider { override suspend fun load() = fallback },
            remote = object : CatalogProvider { override suspend fun load() = remote },
            configuredUrl = { "https://catalog.example/games.json" },
            networkAvailable = { true }
        )
        assertEquals(remote, provider.load())
    }
}

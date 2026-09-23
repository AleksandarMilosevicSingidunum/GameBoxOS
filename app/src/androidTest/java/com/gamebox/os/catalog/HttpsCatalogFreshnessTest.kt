package com.gamebox.os.catalog

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class HttpsCatalogFreshnessTest {
    @Test fun cachedBrowseSurvivesFailureButConnectionTestRequiresFreshResponse() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val root = File(app.cacheDir, "catalog-freshness-" + java.util.UUID.randomUUID()).apply { mkdirs() }
        val context = object : ContextWrapper(app) { override fun getFilesDir() = root }
        val manifest = app.assets.open("catalog/authorized-fixture.json").bufferedReader().use { it.readText() }
        var fail = false
        var malformed = false
        var requests = 0
        var url = "https://example.test/catalog.json"
        val provider = HttpsCatalogProvider(context, configuredUrl = { url }, fetchOverride = { _, _ ->
            requests++
            if (fail) error("Catalog request failed with HTTP 401")
            if (malformed) "{}" else manifest
        })
        try {
            assertTrue(provider.testConnection().success)
            fail = true
            assertTrue(provider.load().games.isNotEmpty())
            val rejected = provider.testConnection()
            assertFalse(rejected.success)
            assertTrue(rejected.message.contains("401"))
            fail = false
            malformed = true
            assertFalse(provider.testConnection().success)
            assertTrue(provider.load().games.isNotEmpty()) // Invalid refresh did not replace good cache.
            url = "https://other.test/catalog.json"
            assertTrue(runCatching { provider.load() }.isFailure) // No cross-endpoint cache reuse.
            malformed = false
            assertTrue(provider.testConnection().success)
            assertEquals(7, requests)
        } finally {
            root.deleteRecursively() // Unique test-owned cache directory only.
        }
    }
}

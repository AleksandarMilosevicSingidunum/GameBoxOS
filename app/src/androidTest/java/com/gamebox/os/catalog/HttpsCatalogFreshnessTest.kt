package com.gamebox.os.catalog

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

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

    @Test fun cancellationDisconnectsAnActiveHttpsCatalogConnection() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val root = File(app.cacheDir, "catalog-cancel-" + java.util.UUID.randomUUID()).apply { mkdirs() }
        val context = object : ContextWrapper(app) { override fun getFilesDir() = root }
        val connection = BlockingHttpsConnection()
        val created = CountDownLatch(1)
        val provider = HttpsCatalogProvider(
            context,
            configuredUrl = { "https://example.test/catalog.json" },
            connectionFactory = {
                created.countDown()
                connection
            },
        )
        try {
            val operation = async(Dispatchers.IO) { provider.refresh() }
            assertTrue(created.await(2, TimeUnit.SECONDS))
            operation.cancel()
            assertTrue(connection.disconnectedLatch.await(2, TimeUnit.SECONDS))
            operation.cancelAndJoin()
        } finally {
            root.deleteRecursively()
        }
    }

    private class BlockingHttpsConnection : HttpsURLConnection(URL("https://example.test")) {
        val disconnectedLatch = CountDownLatch(1)
        @Volatile var disconnected = false

        override fun getResponseCode() = HTTP_OK
        override fun getInputStream(): InputStream = object : InputStream() {
            override fun read(): Int {
                while (!disconnected) Thread.sleep(5)
                throw IOException("connection disconnected")
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = read()
        }
        override fun disconnect() {
            disconnected = true
            disconnectedLatch.countDown()
        }
        override fun usingProxy() = false
        override fun connect() = Unit
        override fun getCipherSuite(): String? = null
        override fun getLocalCertificates() = null
        override fun getServerCertificates() = null
        override fun getPeerPrincipal() = null
        override fun getLocalPrincipal() = null
    }
}

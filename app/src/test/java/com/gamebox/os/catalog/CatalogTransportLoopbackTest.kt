package com.gamebox.os.catalog

import com.sun.net.httpserver.HttpServer
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URI
import java.net.URL
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTransportLoopbackTest {
    @Test
    fun fetchesConfiguredWebDavCatalogThroughLoopbackWithBasicAuthentication() = runBlocking {
        val response = "{\"games\":[{\"id\":\"galaxy-patrol\"}]}"
        val methods = Collections.synchronizedList(mutableListOf<String>())
        val authorization = AtomicReference<String?>(null)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val executor = Executors.newSingleThreadExecutor()
        server.executor = executor
        server.createContext("/catalog.json") { exchange ->
            methods += exchange.requestMethod
            authorization.set(exchange.requestHeaders.getFirst("Authorization"))
            exchange.requestBody.use { it.readBytes() }
            val bytes = response.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        server.start()

        try {
            val client = HttpsCatalogTransportClient(
                connectionFactory = {
                    URL("http://127.0.0.1:${server.address.port}/catalog.json")
                        .openConnection() as HttpURLConnection
                },
            )
            val transport = CatalogTransport.WebDav("https://cloud.example.test")

            val result = client.fetch(transport, CatalogCredentials("player", "secret"))

            assertEquals(response, result)
            assertEquals(listOf("GET"), methods)
            assertTrue(authorization.get().orEmpty().startsWith("Basic "))
        } finally {
            server.stop(0)
            executor.shutdownNow()
        }
    }

    @Test
    fun rejectsLoopbackResponseThatEndsBeforeDeclaredLength() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val executor = Executors.newSingleThreadExecutor()
        server.executor = executor
        server.createContext("/catalog.json") { exchange ->
            val bytes = "{}".toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong() + 4)
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        server.start()

        try {
            val client = HttpsCatalogTransportClient(
                maxRetries = 0,
                connectionFactory = {
                    URL("http://127.0.0.1:${server.address.port}/catalog.json")
                        .openConnection() as HttpURLConnection
                },
            )
            val error = runCatching {
                client.fetch(
                    CatalogTransport.Https("https://cloud.example.test/catalog.json"),
                    credentials = null,
                )
            }.exceptionOrNull()

            assertTrue(error is CatalogTransportException)
            assertTrue((error as CatalogTransportException).recovery.retryable)
            assertFalse(error.message.isNullOrBlank())
        } finally {
            server.stop(0)
            executor.shutdownNow()
        }
    }
}

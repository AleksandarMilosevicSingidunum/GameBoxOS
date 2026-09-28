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
    fun fetchesConfiguredS3CatalogThroughLoopbackWithSigV4Headers() = runBlocking {
        val authorization = AtomicReference<String?>(null)
        val amzDate = AtomicReference<String?>(null)
        val payloadHash = AtomicReference<String?>(null)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val executor = Executors.newSingleThreadExecutor()
        server.executor = executor
        server.createContext("/games/catalog.json") { exchange ->
            authorization.set(exchange.requestHeaders.getFirst("Authorization"))
            amzDate.set(exchange.requestHeaders.getFirst("x-amz-date"))
            payloadHash.set(exchange.requestHeaders.getFirst("x-amz-content-sha256"))
            val bytes = "{}".toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        server.start()

        try {
            val client = HttpsCatalogTransportClient(
                connectionFactory = {
                    URL("http://127.0.0.1:${server.address.port}/games/catalog.json")
                        .openConnection() as HttpURLConnection
                },
            )
            val transport = CatalogTransport.S3(
                endpoint = "https://objects.example.test",
                bucket = "games",
                region = "eu-central-1",
            )

            assertEquals("{}", client.fetch(transport, CatalogCredentials(accessKey = "key", secretKey = "secret")))
            assertTrue(authorization.get().orEmpty().startsWith("AWS4-HMAC-SHA256 "))
            assertTrue(amzDate.get().orEmpty().matches(Regex("\\d{8}T\\d{6}Z")))
            assertEquals(64, payloadHash.get().orEmpty().length)
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

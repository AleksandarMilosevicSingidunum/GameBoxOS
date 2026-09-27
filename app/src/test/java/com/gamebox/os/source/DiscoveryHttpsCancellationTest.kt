package com.gamebox.os.source

import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryHttpsCancellationTest {
    @Test
    fun gameBoxJsonTransportDisconnectsWhenCancelled() = runBlocking {
        val connection = BlockingHttpsConnection()
        val created = CountDownLatch(1)
        val transport = HttpsGameBoxJsonDiscoveryTransport(
            connectionFactory = {
                created.countDown()
                connection
            },
        )
        val operation = async(Dispatchers.IO) {
            transport.get(URI("https://example.test/catalog.json"))
        }
        assertTrue(created.await(2, TimeUnit.SECONDS))
        operation.cancel()
        assertTrue(connection.disconnected.await(2, TimeUnit.SECONDS))
        operation.cancelAndJoin()
    }

    @Test
    fun vimmTransportDisconnectsWhenCancelled() = runBlocking {
        val connection = BlockingHttpsConnection()
        val created = CountDownLatch(1)
        val transport = HttpsVimmLairTransport(
            connectionFactory = {
                created.countDown()
                connection
            },
        )
        val operation = async(Dispatchers.IO) {
            transport.get(URI("https://vimm.net/vault/PS2/a"))
        }
        assertTrue(created.await(2, TimeUnit.SECONDS))
        operation.cancel()
        assertTrue(connection.disconnected.await(2, TimeUnit.SECONDS))
        operation.cancelAndJoin()
    }

    private class BlockingHttpsConnection : HttpsURLConnection(URL("https://example.test")) {
        val disconnected = CountDownLatch(1)
        @Volatile private var closed = false

        override fun getResponseCode() = HTTP_OK
        override fun getInputStream(): InputStream = object : InputStream() {
            override fun read(): Int {
                while (!closed) Thread.sleep(5)
                throw IOException("connection disconnected")
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = read()
        }
        override fun disconnect() {
            closed = true
            disconnected.countDown()
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

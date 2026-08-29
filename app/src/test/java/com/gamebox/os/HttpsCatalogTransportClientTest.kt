package com.gamebox.os

import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.catalog.CatalogTransport
import com.gamebox.os.catalog.HttpsCatalogTransportClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test

class HttpsCatalogTransportClientTest {
    @Test
    fun unsignedS3CredentialsFailBeforeNetworkAccess() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { HttpsCatalogTransportClient().fetch(CatalogTransport.S3("https://example.test", "games"), CatalogCredentials(accessKey = "key", secretKey = "secret")) }
        }
    }
}
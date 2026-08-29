package com.gamebox.os

import com.gamebox.os.catalog.CatalogProviderConfig
import com.gamebox.os.catalog.CatalogTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogProviderConfigTest {
    @Test
    fun supportsWebDavAndOutOfBandCredentials() {
        val config = CatalogProviderConfig(CatalogTransport.WebDav("https://example.test/library"), "webdav-main")
        assertEquals("webdav-main", config.credentialKey)
    }

    @Test
    fun rejectsBlankCredentialKeys() {
        assertThrows(IllegalArgumentException::class.java) {
            CatalogProviderConfig(CatalogTransport.S3("https://example.test", "games"), " ")
        }
    }
}
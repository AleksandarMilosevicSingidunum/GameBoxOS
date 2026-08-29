package com.gamebox.os

import com.gamebox.os.catalog.CatalogTransport
import com.gamebox.os.catalog.catalogUri
import com.gamebox.os.catalog.objectUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogTransportUrisTest {
    @Test
    fun encodesWebDavFileNames() {
        val uri = CatalogTransport.WebDav("https://example.test/library").catalogUri("my catalog.json")
        assertEquals("https://example.test/library/my%20catalog.json", uri.toString())
    }

    @Test
    fun buildsS3ObjectUrisWithPrefix() {
        val uri = CatalogTransport.S3("https://s3.example.test", "games", "authorized").objectUri()
        assertEquals("https://s3.example.test/games/authorized/catalog.json", uri.toString())
    }

    @Test
    fun rejectsTraversalNames() {
        assertThrows(IllegalArgumentException::class.java) { CatalogTransport.WebDav("https://example.test").catalogUri("../catalog.json") }
    }
}
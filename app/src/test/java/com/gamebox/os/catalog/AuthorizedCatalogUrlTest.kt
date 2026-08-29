package com.gamebox.os.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthorizedCatalogUrlTest {
    @Test
    fun acceptsHttpsWithoutEmbeddedCredentials() {
        assertEquals(
            "https://catalog.example.com/gamebox/manifest.json",
            validateAuthorizedCatalogUrl(" https://catalog.example.com/gamebox/manifest.json ")
        )
    }

    @Test
    fun rejectsInsecureOrAmbiguousUrls() {
        assertThrows(IllegalArgumentException::class.java) {
            validateAuthorizedCatalogUrl("http://catalog.example.com/manifest.json")
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateAuthorizedCatalogUrl("https://user:secret@catalog.example.com/manifest.json")
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateAuthorizedCatalogUrl("https:///manifest.json")
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateAuthorizedCatalogUrl("https://catalog.example.com/manifest.json#section")
        }
    }
}

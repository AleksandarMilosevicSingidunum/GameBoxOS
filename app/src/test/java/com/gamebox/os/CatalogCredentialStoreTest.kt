package com.gamebox.os

import com.gamebox.os.catalog.CatalogCredentialStore
import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.catalog.InMemoryCatalogCredentialStore
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogCredentialStoreTest {
    @Test
    fun retrievesCredentialsByOpaqueKey() {
        val store: CatalogCredentialStore = InMemoryCatalogCredentialStore(mapOf("primary" to CatalogCredentials(username = "user")))
        assertEquals("user", store.credentials("primary")?.username)
    }
}
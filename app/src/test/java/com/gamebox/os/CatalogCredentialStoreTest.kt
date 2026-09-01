package com.gamebox.os

import com.gamebox.os.catalog.CatalogCredentialStore
import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.catalog.InMemoryCatalogCredentialStore
import com.gamebox.os.catalog.SettingsCatalogCredentialStore
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogCredentialStoreTest {
    @Test
    fun retrievesCredentialsByOpaqueKey() {
        val store: CatalogCredentialStore = InMemoryCatalogCredentialStore(mapOf("primary" to CatalogCredentials(username = "user")))
        assertEquals("user", store.credentials("primary")?.username)
    }

    @Test
    fun settingsAdapterOnlyExposesTheRequestedCredentialKey() {
        val adapter = SettingsCatalogCredentialStore { key ->
            if (key == "catalog-s3") CatalogCredentials(accessKey = "access", secretKey = "secret") else null
        }

        assertEquals("access", adapter.credentials("catalog-s3")?.accessKey)
        assertEquals(null, adapter.credentials("catalog-webdav"))
    }
}


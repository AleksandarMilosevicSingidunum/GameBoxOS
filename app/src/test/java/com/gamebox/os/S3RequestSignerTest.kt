package com.gamebox.os

import com.gamebox.os.catalog.CatalogProviderConfig
import com.gamebox.os.catalog.CatalogTransport
import org.junit.Assert.assertEquals
import org.junit.Test

class S3RequestSignerTest {
    @Test
    fun s3CatalogConfigurationRetainsItsSigningRegion() {
        val config = CatalogProviderConfig(
            CatalogTransport.S3("https://s3.example.test", "games", region = "eu-central-1")
        )

        assertEquals("eu-central-1", (config.transport as CatalogTransport.S3).region)
    }
}


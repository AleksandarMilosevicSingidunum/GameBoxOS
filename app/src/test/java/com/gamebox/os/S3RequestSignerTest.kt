package com.gamebox.os

import com.gamebox.os.catalog.CatalogProviderConfig
import com.gamebox.os.catalog.CatalogTransport
import com.gamebox.os.catalog.canonicalQueryForSigning
import com.gamebox.os.catalog.canonicalUriForSigning
import org.junit.Assert.assertEquals
import org.junit.Test

class S3RequestSignerTest {
    @Test
    fun canonicalPathPreservesEscapesAndEncodesUnescapedObjectKeyBytes() {
        assertEquals(
            "/My%20Saves/game%2Bone/%E2%98%83",
            canonicalUriForSigning("https://s3.example.test/My%20Saves/game%2bone/☃"),
        )
        assertEquals("/a//b", canonicalUriForSigning("https://s3.example.test/a//b"))
    }

    @Test
    fun canonicalQueryEncodesAndSortsKeysAndValues() {
        assertEquals(
            "a=plus%2Bvalue&acl=&z=hello%20world",
            canonicalQueryForSigning("https://s3.example.test/object?z=hello%20world&acl&a=plus+value"),
        )
    }

    @Test
    fun s3CatalogConfigurationRetainsItsSigningRegion() {
        val config = CatalogProviderConfig(
            CatalogTransport.S3("https://s3.example.test", "games", region = "eu-central-1")
        )

        assertEquals("eu-central-1", (config.transport as CatalogTransport.S3).region)
    }
}


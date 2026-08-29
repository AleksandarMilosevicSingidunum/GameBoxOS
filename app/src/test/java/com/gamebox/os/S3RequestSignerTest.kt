package com.gamebox.os

import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.catalog.UnsupportedS3RequestSigner
import org.junit.Assert.assertThrows
import org.junit.Test

class S3RequestSignerTest {
    @Test
    fun unsupportedSignerFailsExplicitly() {
        assertThrows(UnsupportedOperationException::class.java) {
            UnsupportedS3RequestSigner().sign("GET", "https://example.test/object", "hash", CatalogCredentials(accessKey = "a", secretKey = "s"))
        }
    }
}
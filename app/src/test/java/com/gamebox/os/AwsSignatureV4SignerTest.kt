package com.gamebox.os

import com.gamebox.os.catalog.AwsSignatureV4Signer
import com.gamebox.os.catalog.CatalogCredentials
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertTrue
import org.junit.Test

class AwsSignatureV4SignerTest {
    @Test
    fun signsWithExpectedCredentialScope() {
        val signer = AwsSignatureV4Signer("us-east-1", clock = Clock.fixed(Instant.parse("2020-01-02T03:04:05Z"), ZoneOffset.UTC))
        val result = signer.sign("GET", "https://s3.example.test/bucket/catalog.json", "e".repeat(64), CatalogCredentials("AKID", "secret"))
        assertTrue(result.date == "20200102T030405Z")
        assertTrue(result.authorization.startsWith("AWS4-HMAC-SHA256 Credential=AKID/20200102/us-east-1/s3/aws4_request"))
    }
}

package com.gamebox.os.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class ReleaseChannelValidationTest {
    private val bytes = "artifact".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test
    fun infersChannelsFromBlueprintTagConventions() {
        assertEquals(ReleaseChannel.ALPHA, ReleaseReadinessEvaluator.channelForTag("v1.0.0-alpha.1"))
        assertEquals(ReleaseChannel.BETA, ReleaseReadinessEvaluator.channelForTag("v1.0.0-beta.2"))
        assertEquals(ReleaseChannel.PRODUCTION, ReleaseReadinessEvaluator.channelForTag("v1.0.0"))
    }

    @Test
    fun rejectsUnsupportedTags() {
        runCatching { ReleaseReadinessEvaluator.channelForTag("nightly") }
            .onSuccess { throw AssertionError("nightly should be rejected") }
    }

    @Test
    fun validatesManifestTagAndChannel() {
        val manifest = ReleaseArtifactManifest("app.apk", "v1.0.0-alpha.1", ReleaseChannel.ALPHA, hash, bytes.size.toLong(), "v0.9.0-alpha.2")
        assertTrue(ReleaseReadinessEvaluator.validateManifest(manifest, "v1.0.0-alpha.1").allowed)
        assertFalse(ReleaseReadinessEvaluator.validateManifest(manifest.copy(channel = ReleaseChannel.PRODUCTION), "v1.0.0-alpha.1").allowed)
        assertFalse(ReleaseReadinessEvaluator.validateManifest(manifest, "v1.0.0-beta.1").allowed)
    }
}

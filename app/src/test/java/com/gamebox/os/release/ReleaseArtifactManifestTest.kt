package com.gamebox.os.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertFailsWith
import java.security.MessageDigest

class ReleaseArtifactManifestTest {
    private val bytes = "GameBox OS release".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }

    @Test
    fun encodeDecodeAndVerify() {
        val manifest = ReleaseArtifactManifest(
            artifactName = "GameBoxOS-v1.2.3-alpha.1-debug.apk",
            releaseTag = "v1.2.3-alpha.1",
            channel = ReleaseChannel.ALPHA,
            sha256 = hash,
            sizeBytes = bytes.size.toLong(),
            rollbackReleaseTag = "v1.2.2-alpha.4",
        )
        val decoded = ReleaseArtifactManifestCodec.decode(ReleaseArtifactManifestCodec.encode(manifest))
        assertEquals(manifest, decoded)
        assertTrue(ReleaseArtifactManifestCodec.verify(decoded, bytes, "v1.2.3-alpha.1"))
    }

    @Test
    fun rejectsInvalidMetadata() {
        assertFailsWith<IllegalArgumentException> {
            ReleaseArtifactManifest("bad/path.apk", "v1.0.0", ReleaseChannel.ALPHA, hash, 1)
        }
        assertFailsWith<IllegalArgumentException> {
            ReleaseArtifactManifest("app.apk", "release", ReleaseChannel.ALPHA, hash, 1)
        }
        assertFailsWith<IllegalArgumentException> {
            ReleaseArtifactManifest("app.apk", "v1.0.0", ReleaseChannel.ALPHA, "00", 1)
        }
        assertFailsWith<IllegalArgumentException> {
            ReleaseArtifactManifest("app.apk", "v1.0.0", ReleaseChannel.ALPHA, hash, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            ReleaseArtifactManifest("app.apk", "v1.0.0", ReleaseChannel.ALPHA, hash, 1, "v1.0.0")
        }
    }

    @Test
    fun rejectsChangedArtifactOrTag() {
        val manifest = ReleaseArtifactManifest("app.apk", "v1.0.0", ReleaseChannel.ALPHA, hash, bytes.size.toLong())
        assertFalse(ReleaseArtifactManifestCodec.verify(manifest, bytes + 1, "v1.0.0"))
        assertFalse(ReleaseArtifactManifestCodec.verify(manifest, bytes, "v1.0.1"))
    }
}

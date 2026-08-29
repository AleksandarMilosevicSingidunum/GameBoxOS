package com.gamebox.os.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
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
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseArtifactManifest(schemaVersion = 2, artifactName = "app.apk", releaseTag = "v1.0.0", channel = ReleaseChannel.ALPHA, sha256 = hash, sizeBytes = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseArtifactManifest(artifactName = "bad/path.apk", releaseTag = "v1.0.0", channel = ReleaseChannel.ALPHA, sha256 = hash, sizeBytes = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseArtifactManifest(artifactName = "app.apk", releaseTag = "release", channel = ReleaseChannel.ALPHA, sha256 = hash, sizeBytes = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseArtifactManifest(artifactName = "app.apk", releaseTag = "v1.0.0", channel = ReleaseChannel.ALPHA, sha256 = "00", sizeBytes = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseArtifactManifest(artifactName = "app.apk", releaseTag = "v1.0.0", channel = ReleaseChannel.ALPHA, sha256 = hash, sizeBytes = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseArtifactManifest(artifactName = "app.apk", releaseTag = "v1.0.0", channel = ReleaseChannel.ALPHA, sha256 = hash, sizeBytes = 1, rollbackReleaseTag = "v1.0.0")
        }
    }

    @Test
    fun rejectsChangedArtifactOrTag() {
        val manifest = ReleaseArtifactManifest(
            artifactName = "app.apk", releaseTag = "v1.0.0",
            channel = ReleaseChannel.ALPHA, sha256 = hash, sizeBytes = bytes.size.toLong()
        )
        assertFalse(ReleaseArtifactManifestCodec.verify(manifest, bytes + 1, "v1.0.0"))
        assertFalse(ReleaseArtifactManifestCodec.verify(manifest, bytes, "v1.0.1"))
    }
}

package com.gamebox.os.release

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

@Serializable
data class ReleaseArtifactManifest(
    val schemaVersion: Int = 1,
    val artifactName: String,
    val releaseTag: String,
    val channel: ReleaseChannel,
    val sha256: String,
    val sizeBytes: Long,
    val rollbackReleaseTag: String? = null,
) {
    init {
        require(artifactName.matches(Regex("^[A-Za-z0-9._-]+\\.apk$")))
        require(releaseTag.matches(Regex("^v[0-9]+\\.[0-9]+\\.[0-9]+(-[A-Za-z0-9.-]+)?$")))
        require(sha256.matches(Regex("^[a-fA-F0-9]{64}$")))
        require(sizeBytes > 0)
        rollbackReleaseTag?.let {
            require(it.matches(Regex("^v[0-9]+\\.[0-9]+\\.[0-9]+(-[A-Za-z0-9.-]+)?$")))
            require(it != releaseTag)
        }
    }
}

object ReleaseArtifactManifestCodec {
    private val json = Json { encodeDefaults = true; prettyPrint = true }

    fun encode(manifest: ReleaseArtifactManifest): String = json.encodeToString(manifest)

    fun decode(value: String): ReleaseArtifactManifest = json.decodeFromString(value)

    fun verify(manifest: ReleaseArtifactManifest, artifactBytes: ByteArray, expectedTag: String): Boolean {
        if (manifest.releaseTag != expectedTag || manifest.sizeBytes != artifactBytes.size.toLong()) return false
        val actual = MessageDigest.getInstance("SHA-256").digest(artifactBytes)
            .joinToString("") { "%02x".format(it) }
        return actual.equals(manifest.sha256, ignoreCase = true)
    }
}

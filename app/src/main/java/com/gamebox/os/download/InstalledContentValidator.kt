package com.gamebox.os.download

import java.io.File

enum class InstalledContentStatus { VERIFIED, MISSING, ALTERED }

class InstalledContentValidator(
    rootDirectory: File,
    private val verifier: Sha256Verifier = Sha256Verifier()
) {
    private val root = rootDirectory.canonicalFile

    fun validate(relativePath: String, expectedSha256: String): InstalledContentStatus {
        require(relativePath.isNotBlank()) { "Content path cannot be blank" }
        require(!File(relativePath).isAbsolute) { "Content path must be relative" }
        val content = File(root, relativePath).canonicalFile
        require(content.path.startsWith(root.path + File.separator)) {
            "Content path escapes install storage"
        }
        if (!content.isFile) return InstalledContentStatus.MISSING
        return when (content.inputStream().use { verifier.verify(it, expectedSha256) }) {
            VerificationResult.Verified -> InstalledContentStatus.VERIFIED
            is VerificationResult.ChecksumMismatch -> InstalledContentStatus.ALTERED
        }
    }
}

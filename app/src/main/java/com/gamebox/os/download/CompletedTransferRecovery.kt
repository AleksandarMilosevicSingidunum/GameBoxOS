package com.gamebox.os.download

/**
 * Retry the local verification/promotion phase before requesting the source again.
 * A file's existence or byte count alone never establishes installation success.
 */
internal class CompletedTransferRecovery(
    private val verifier: Sha256Verifier = Sha256Verifier(),
) {
    fun recover(
        staging: FileStagingTarget,
        expectedSha256: String,
        maxBytes: Long,
        checkActive: () -> Unit = {},
    ): Long? {
        require(maxBytes > 0) { "Maximum transfer size must be positive" }
        require(expectedSha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Invalid expected SHA-256" }
        checkActive()
        val installed = staging.finalFile
        if (installed.isFile && installed.length() <= maxBytes &&
            installed.inputStream().use { verifier.verify(it, expectedSha256, checkActive) } == VerificationResult.Verified
        ) {
            checkActive()
            return installed.length()
        }
        val partial = staging.stagingFile
        if (partial.isFile && partial.length() <= maxBytes &&
            staging.openInput().use { verifier.verify(it, expectedSha256, checkActive) } == VerificationResult.Verified
        ) {
            checkActive()
            val bytes = partial.length()
            // On promotion failure the verified .part remains for another local retry.
            staging.commit()
            return bytes
        }
        return null
    }
}

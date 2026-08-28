package com.gamebox.os.download

import java.io.InputStream
import java.io.OutputStream

interface TransferSource {
    val totalBytes: Long?
    val expectedSha256: String
    fun openInput(): InputStream
}

interface StagingTarget {
    fun openOutput(): OutputStream
    fun openInput(): InputStream
    fun commit()
    fun discard()
}

sealed interface TransferResult {
    data class Success(val bytesTransferred: Long) : TransferResult
    data class Cancelled(val bytesTransferred: Long) : TransferResult
    data class ChecksumMismatch(val actualSha256: String) : TransferResult
    data class SizeLimitExceeded(val limitBytes: Long) : TransferResult
    data class Failed(val reason: String) : TransferResult
}

data class TransferProgress(val bytesTransferred: Long, val totalBytes: Long?)

class TransferEngine(
    private val verifier: Sha256Verifier = Sha256Verifier(),
    private val bufferSize: Int = 64 * 1024
) {
    init {
        require(bufferSize > 0) { "Buffer size must be positive" }
    }

    fun transfer(
        source: TransferSource,
        staging: StagingTarget,
        maxBytes: Long,
        isCancelled: () -> Boolean = { false },
        onProgress: (TransferProgress) -> Unit = {}
    ): TransferResult {
        require(maxBytes > 0L) { "Maximum transfer size must be positive" }
        val declaredSize = source.totalBytes
        if (declaredSize != null && (declaredSize < 0L || declaredSize > maxBytes)) {
            staging.discard()
            return TransferResult.SizeLimitExceeded(maxBytes)
        }

        var transferred = 0L
        return try {
            var aborted: TransferResult? = null
            source.openInput().use { input ->
                staging.openOutput().use { output ->
                    val buffer = ByteArray(bufferSize)
                    while (aborted == null) {
                        if (isCancelled()) {
                            aborted = TransferResult.Cancelled(transferred)
                            break
                        }
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        transferred += count
                        if (transferred > maxBytes) {
                            aborted = TransferResult.SizeLimitExceeded(maxBytes)
                            break
                        }
                        output.write(buffer, 0, count)
                        onProgress(TransferProgress(transferred, declaredSize))
                    }
                    if (aborted == null) output.flush()
                }
            }

            if (aborted != null) {
                staging.discard()
                aborted
            } else {
                verifyAndCommit(source, staging, transferred)
            }
        } catch (error: Exception) {
            runCatching { staging.discard() }
            TransferResult.Failed(error::class.simpleName ?: "Transfer failure")
        }
    }

    private fun verifyAndCommit(
        source: TransferSource,
        staging: StagingTarget,
        transferred: Long
    ): TransferResult {
        return when (val verification = staging.openInput().use {
            verifier.verify(it, source.expectedSha256)
        }) {
            VerificationResult.Verified -> {
                staging.commit()
                TransferResult.Success(transferred)
            }
            is VerificationResult.ChecksumMismatch -> {
                staging.discard()
                TransferResult.ChecksumMismatch(verification.actualHex)
            }
        }
    }
}

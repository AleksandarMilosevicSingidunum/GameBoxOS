package com.gamebox.os.download

import com.gamebox.os.provider.ProviderRecoveryPolicy

sealed interface ResumableTransferResult {
    data class Success(val bytesTransferred: Long) : ResumableTransferResult
    data class Paused(val bytesTransferred: Long) : ResumableTransferResult
    data class ChecksumMismatch(val actualSha256: String) : ResumableTransferResult
    data class SizeLimitExceeded(val limitBytes: Long) : ResumableTransferResult
    data class Failed(val reason: String, val bytesTransferred: Long, val retryable: Boolean = false) : ResumableTransferResult
}

class ResumableTransferEngine(
    private val verifier: Sha256Verifier = Sha256Verifier(),
    private val bufferSize: Int = 64 * 1024
) {
    init {
        require(bufferSize > 0) { "Buffer size must be positive" }
    }

    fun transfer(
        source: HttpsTransferSource,
        staging: FileStagingTarget,
        maxBytes: Long,
        isPausedOrCancelled: () -> Boolean,
        onProgress: (TransferProgress) -> Unit
    ): ResumableTransferResult {
        require(maxBytes > 0L) { "Maximum transfer size must be positive" }
        var offset = staging.stagedBytes
        if (offset > maxBytes) {
            staging.discard()
            return ResumableTransferResult.SizeLimitExceeded(maxBytes)
        }

        val opened = try {
            source.openInputAt(offset)
        } catch (_: RangeNotSupportedException) {
            staging.discard()
            offset = 0L
            try {
                source.openInputAt(0L)
            } catch (error: Exception) {
                return transferFailure(error, offset)
            }
        } catch (error: Exception) {
            return transferFailure(error, offset)
        }

        val total = opened.totalBytes
        if (total != null && total > maxBytes) {
            opened.input.close()
            staging.discard()
            return ResumableTransferResult.SizeLimitExceeded(maxBytes)
        }

        var transferred = offset
        return try {
            opened.input.use { input ->
                staging.openOutput(append = offset > 0L).use { output ->
                    val buffer = ByteArray(bufferSize)
                    while (true) {
                        if (isPausedOrCancelled()) {
                            output.flush()
                            return ResumableTransferResult.Paused(transferred)
                        }
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        transferred += count
                        if (transferred > maxBytes) {
                            staging.discard()
                            return ResumableTransferResult.SizeLimitExceeded(maxBytes)
                        }
                        output.write(buffer, 0, count)
                        onProgress(TransferProgress(transferred, total))
                    }
                    output.flush()
                }
            }
            if (total != null && transferred < total) {
                return ResumableTransferResult.Failed(
                    "Download interrupted before all bytes arrived; retry to resume", transferred, retryable = true)
            }
            if (total != null && transferred > total) {
                staging.discard()
                return ResumableTransferResult.Failed("Server sent more bytes than declared", transferred)
            }
            when (val verification = staging.openInput().use {
                verifier.verify(it, source.expectedSha256)
            }) {
                VerificationResult.Verified -> {
                    staging.commit()
                    ResumableTransferResult.Success(transferred)
                }
                is VerificationResult.ChecksumMismatch -> {
                    staging.discard()
                    ResumableTransferResult.ChecksumMismatch(verification.actualHex)
                }
            }
        } catch (error: Exception) {
            transferFailure(error, transferred)
        }
    }
}

internal fun transferFailure(error: Exception, bytesTransferred: Long): ResumableTransferResult.Failed {
    if (error is kotlinx.coroutines.CancellationException) throw error
    val recovery = ProviderRecoveryPolicy.classify((error as? DownloadRequestException)?.status, error)
    return ResumableTransferResult.Failed(
        error.message ?: recovery.userMessage, bytesTransferred, recovery.retryable)
}

internal fun shouldRetryTransfer(failure: ResumableTransferResult.Failed, attempt: Int): Boolean =
    failure.retryable && attempt in 0 until 3

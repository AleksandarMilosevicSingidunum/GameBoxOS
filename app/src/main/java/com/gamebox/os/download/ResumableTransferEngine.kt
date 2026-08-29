package com.gamebox.os.download

sealed interface ResumableTransferResult {
    data class Success(val bytesTransferred: Long) : ResumableTransferResult
    data class Paused(val bytesTransferred: Long) : ResumableTransferResult
    data class ChecksumMismatch(val actualSha256: String) : ResumableTransferResult
    data class SizeLimitExceeded(val limitBytes: Long) : ResumableTransferResult
    data class Failed(val reason: String, val bytesTransferred: Long) : ResumableTransferResult
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
                return ResumableTransferResult.Failed(
                    error.message ?: error::class.simpleName.orEmpty(),
                    offset
                )
            }
        } catch (error: Exception) {
            return ResumableTransferResult.Failed(
                error.message ?: error::class.simpleName.orEmpty(),
                offset
            )
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
            ResumableTransferResult.Failed(
                error.message ?: error::class.simpleName.orEmpty(),
                transferred
            )
        }
    }
}

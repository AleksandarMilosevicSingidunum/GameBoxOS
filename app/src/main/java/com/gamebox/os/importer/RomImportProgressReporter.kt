package com.gamebox.os.importer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.CoroutineContext

/** Delivers UI progress without allowing observer errors to fail a valid import. */
internal class RomImportProgressReporter(
    private val coroutineContext: CoroutineContext,
    private val observer: (RomImportProgress) -> Unit,
) {
    fun report(
        stage: RomImportStage,
        bytesProcessed: Long,
        totalBytes: Long?,
        fileIndex: Int,
        fileCount: Int,
    ) {
        coroutineContext.ensureActive()
        try {
            observer(
                RomImportProgress(
                    stage = stage,
                    bytesProcessed = bytesProcessed,
                    totalBytes = totalBytes,
                    fileIndex = fileIndex,
                    fileCount = fileCount,
                ),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Observer failures must not fail a valid local import.
        }
    }
}

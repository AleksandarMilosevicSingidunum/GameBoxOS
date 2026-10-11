package com.gamebox.os.importer

import java.io.File
import java.io.InputStream
import java.nio.file.Files

internal const val MAX_DISC_SET_FILES = 64

/** Stages and verifies one selected source without owning the surrounding import transaction. */
internal class RomImportSourceStager(
    private val maxBytes: Long,
    private val progress: RomImportProgressReporter,
) {
    /** Returns null only when Android can no longer open the selected document. */
    fun stage(
        openInputStream: () -> InputStream?,
        safeName: String,
        sourceSize: Long?,
        sourceIndex: Int,
        sourceCount: Int,
        stagedFileCount: Int,
        bytesAlreadyStaged: Long,
        platform: String?,
        staging: File,
        expectedPrefix: String,
    ): List<ImportedRomFile>? {
        val isArchive = safeName.substringAfterLast('.', "").lowercase() in ARCHIVE_EXTENSIONS
        val remainingBytes = (maxBytes - bytesAlreadyStaged).coerceAtLeast(1L)
        if (isArchive && sourceSize?.let { it > remainingBytes } == true) {
            throw IllegalArgumentException("Archive source exceeds the remaining import limit")
        }
        progress.report(
            if (isArchive) RomImportStage.COPYING_ARCHIVE else RomImportStage.COPYING_AND_VERIFYING,
            bytesProcessed = 0L,
            totalBytes = sourceSize,
            fileIndex = sourceIndex,
            fileCount = sourceCount,
        )
        val input = try {
            openInputStream()
        } catch (_: SecurityException) {
            null
        } ?: return null
        return if (isArchive) {
            stageArchive(
                input = input,
                safeName = safeName,
                sourceSize = sourceSize,
                sourceIndex = sourceIndex,
                sourceCount = sourceCount,
                stagedFileCount = stagedFileCount,
                remainingBytes = remainingBytes,
                platform = platform,
                staging = staging,
                expectedPrefix = expectedPrefix,
            )
        } else {
            stagePlainFile(
                input = input,
                safeName = safeName,
                sourceSize = sourceSize,
                sourceIndex = sourceIndex,
                sourceCount = sourceCount,
                remainingBytes = remainingBytes,
                staging = staging,
                expectedPrefix = expectedPrefix,
            )
        }
    }

    private fun stageArchive(
        input: java.io.InputStream,
        safeName: String,
        sourceSize: Long?,
        sourceIndex: Int,
        sourceCount: Int,
        stagedFileCount: Int,
        remainingBytes: Long,
        platform: String?,
        staging: File,
        expectedPrefix: String,
    ): List<ImportedRomFile> {
        val archiveFile = staging.resolve(".source-$sourceIndex.$safeName")
        input.use { stream ->
            copyBoundedArchiveSource(stream, archiveFile, remainingBytes) { copied ->
                progress.report(
                    RomImportStage.COPYING_ARCHIVE,
                    copied,
                    sourceSize,
                    sourceIndex,
                    sourceCount,
                )
            }
        }
        progress.report(
            RomImportStage.EXTRACTING_ARCHIVE,
            bytesProcessed = 0L,
            totalBytes = null,
            fileIndex = sourceIndex,
            fileCount = sourceCount,
        )
        val extracted = requireNotNull(
            RomArchiveExtractor.extractIfArchive(
                source = archiveFile,
                staging = staging,
                platform = platform,
                maxFiles = MAX_DISC_SET_FILES - stagedFileCount,
                maxBytes = remainingBytes,
                onProgress = { extractedBytes, totalExtractedBytes ->
                    progress.report(
                        RomImportStage.EXTRACTING_ARCHIVE,
                        extractedBytes,
                        totalExtractedBytes,
                        sourceIndex,
                        sourceCount,
                    )
                },
            ),
        )
        var verifiedSourceBytes = 0L
        val imported = extracted.mapIndexed { extractedIndex, item ->
            val destination = staging.resolve(item.displayName)
            require(!destination.exists()) {
                "Selected archives contain duplicate game file name: ${item.displayName}"
            }
            Files.move(item.file.toPath(), destination.toPath())
            val extractedSize = destination.length().takeIf { it > 0L }
            var verifiedBytes = 0L
            progress.report(
                RomImportStage.VERIFYING_EXTRACTED_FILE,
                bytesProcessed = 0L,
                totalBytes = extractedSize,
                fileIndex = extractedIndex + 1,
                fileCount = extracted.size,
            )
            val hashes = destination.inputStream().use { stream ->
                RomHasher.hash(stream, (remainingBytes - verifiedSourceBytes).coerceAtLeast(1L)) { _, count ->
                    verifiedBytes += count
                    progress.report(
                        RomImportStage.VERIFYING_EXTRACTED_FILE,
                        verifiedBytes,
                        extractedSize,
                        extractedIndex + 1,
                        extracted.size,
                    )
                }
            }
            verifiedSourceBytes += hashes.sizeBytes
            ImportedRomFile(
                relativePath = expectedPrefix + item.displayName,
                hashes = hashes,
                mimeType = RomImportPolicy.mimeType(item.displayName),
            )
        }
        archiveFile.delete()
        staging.resolve("archive-content").deleteRecursively()
        return imported
    }

    private fun stagePlainFile(
        input: java.io.InputStream,
        safeName: String,
        sourceSize: Long?,
        sourceIndex: Int,
        sourceCount: Int,
        remainingBytes: Long,
        staging: File,
        expectedPrefix: String,
    ): List<ImportedRomFile> {
        val stagedFile = staging.resolve(safeName)
        var processedBytes = 0L
        val hashes = input.use { stream ->
            stagedFile.outputStream().buffered().use { output ->
                RomHasher.hash(stream, remainingBytes) { buffer, count ->
                    output.write(buffer, 0, count)
                    processedBytes += count
                    progress.report(
                        RomImportStage.COPYING_AND_VERIFYING,
                        processedBytes,
                        sourceSize,
                        sourceIndex,
                        sourceCount,
                    )
                }
            }
        }
        return listOf(
            ImportedRomFile(
                relativePath = expectedPrefix + safeName,
                hashes = hashes,
                mimeType = RomImportPolicy.mimeType(safeName),
            ),
        )
    }

    private companion object {
        val ARCHIVE_EXTENSIONS = setOf("zip", "7z")
    }
}

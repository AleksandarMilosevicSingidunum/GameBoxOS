package com.gamebox.os.importer

import java.io.File
import java.util.zip.ZipFile
import org.apache.commons.compress.archivers.sevenz.SevenZFile

/**
 * Extracts user-owned archives into the current import staging directory.
 * Archive entries are never allowed to escape staging, and only files accepted
 * by the selected console profile are returned to the importer.
 */
object RomArchiveExtractor {
    data class ExtractedFile(val file: File, val displayName: String)

    fun extractIfArchive(
        source: File,
        staging: File,
        platform: String?,
        maxFiles: Int,
        maxBytes: Long,
        onProgress: ((bytesProcessed: Long, totalBytes: Long?) -> Unit)? = null,
    ): List<ExtractedFile>? {
        val extension = source.extension.lowercase()
        if (extension !in setOf("zip", "7z")) return null
        val output = staging.resolve("archive-content").apply { mkdirs() }
        val result = try {
            when (extension) {
                "zip" -> extractZip(source, output, platform, maxFiles, maxBytes, onProgress)
                else -> extractSevenZip(source, output, platform, maxFiles, maxBytes, onProgress)
            }
        } catch (error: Exception) {
            // Do not leave an earlier entry (or a partially written current
            // entry) behind when a later archive entry is corrupt or exceeds
            // the import limits. The parent transaction also cleans staging,
            // but keep this boundary safe for every caller.
            output.deleteRecursively()
            throw error
        }
        return try {
            require(result.isNotEmpty()) {
                "The archive contains no supported ${RomImportPolicy.profileLabel(platform)} game files"
            }
            result
        } catch (error: Exception) {
            output.deleteRecursively()
            throw error
        }
    }

    private fun extractZip(
        source: File,
        output: File,
        platform: String?,
        maxFiles: Int,
        maxBytes: Long,
        onProgress: ((bytesProcessed: Long, totalBytes: Long?) -> Unit)?,
    ): List<ExtractedFile> {
        ZipFile(source).use { zip ->
            val totalBytes = zipSupportedContentSize(zip, platform)
            var total = 0L
            var processed = 0L
            val progressThrottle = ByteProgressThrottle(
                onProgress = { bytes -> onProgress?.invoke(bytes, totalBytes) },
            )
            return zip.entries().asSequence()
                .filterNot { it.isDirectory }
                .mapNotNull { entry ->
                    val safeName = supportedContentName(entry.name, platform) ?: return@mapNotNull null
                    require(resultCount(output) < maxFiles) { "Archive contains too many supported game files" }
                    val target = uniqueTarget(output, safeName)
                    val written = zip.getInputStream(entry).use { input ->
                        writeBounded(
                            readChunk = input::read,
                            target = target,
                            remaining = maxBytes - total,
                        ) { count ->
                            processed += count
                            progressThrottle.onBytesProcessed(processed)
                        }
                    }
                    total += written
                    progressThrottle.finish(processed)
                    ExtractedFile(target, safeName)
                }
                .toList()
        }
    }

    private fun extractSevenZip(
        source: File,
        output: File,
        platform: String?,
        maxFiles: Int,
        maxBytes: Long,
        onProgress: ((bytesProcessed: Long, totalBytes: Long?) -> Unit)?,
    ): List<ExtractedFile> {
        val totalBytes = sevenZipSupportedContentSize(source, platform)
        SevenZFile.builder().setFile(source).get().use { archive ->
            var total = 0L
            var processed = 0L
            val progressThrottle = ByteProgressThrottle(
                onProgress = { bytes -> onProgress?.invoke(bytes, totalBytes) },
            )
            val result = mutableListOf<ExtractedFile>()
            while (true) {
                val entry = archive.nextEntry ?: break
                if (entry.isDirectory) continue
                val safeName = supportedContentName(entry.name, platform) ?: continue
                require(result.size < maxFiles) { "Archive contains too many supported game files" }
                val target = uniqueTarget(output, safeName)
                val written = writeBounded(
                    readChunk = archive::read,
                    target = target,
                    remaining = maxBytes - total,
                ) { count ->
                    processed += count
                    progressThrottle.onBytesProcessed(processed)
                }
                total += written
                progressThrottle.finish(processed)
                result += ExtractedFile(target, safeName)
            }
            return result
        }
    }

    private fun safeEntryName(raw: String): String? {
        val normalized = raw.replace('\\', '/')
        if (normalized.startsWith('/') || normalized.split('/').any { it.isEmpty() || it == "." || it == ".." }) {
            return null
        }
        return normalized.substringAfterLast('/').takeIf { it.isNotBlank() }
    }

    private fun supportedContentName(raw: String, platform: String?): String? {
        val name = safeEntryName(raw) ?: return null
        return runCatching { RomImportPolicy.safeArchiveContentFileName(name, platform) }.getOrNull()
    }

    private fun zipSupportedContentSize(zip: ZipFile, platform: String?): Long? {
        val entries = zip.entries()
        var total = 0L
        var known = true
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.isDirectory || supportedContentName(entry.name, platform) == null) continue
            if (entry.size < 0L) {
                known = false
            } else if (known) {
                val next = total + entry.size
                if (next < total) known = false else total = next
            }
        }
        return total.takeIf { known && it > 0L }
    }

    private fun sevenZipSupportedContentSize(source: File, platform: String?): Long? {
        SevenZFile.builder().setFile(source).get().use { archive ->
            var total = 0L
            var known = true
            while (true) {
                val entry = archive.nextEntry ?: break
                if (entry.isDirectory || supportedContentName(entry.name, platform) == null) continue
                if (entry.size < 0L) {
                    known = false
                } else if (known) {
                    val next = total + entry.size
                    if (next < total) known = false else total = next
                }
            }
            return total.takeIf { known && it > 0L }
        }
    }

    private fun writeBounded(
        readChunk: (ByteArray) -> Int,
        target: File,
        remaining: Long,
        onChunk: (Long) -> Unit = {},
    ): Long {
        require(remaining > 0) { "Archive exceeds the configured import limit" }
        var total = 0L
        target.outputStream().buffered().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val bytesRead = readChunk(buffer)
                if (bytesRead < 0) break
                total += bytesRead
                require(total <= remaining) { "Archive exceeds the configured import limit" }
                output.write(buffer, 0, bytesRead)
                onChunk(bytesRead.toLong())
            }
        }
        return total
    }

    private fun resultCount(output: File): Int = output.listFiles()?.count { it.isFile } ?: 0

    private fun uniqueTarget(output: File, name: String): File {
        val first = output.resolve(name)
        if (!first.exists()) return first
        val stem = name.substringBeforeLast('.', name)
        val extension = name.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
        var index = 2
        while (true) {
            val candidate = output.resolve("$stem-$index$extension")
            if (!candidate.exists()) return candidate
            index++
        }
    }
}

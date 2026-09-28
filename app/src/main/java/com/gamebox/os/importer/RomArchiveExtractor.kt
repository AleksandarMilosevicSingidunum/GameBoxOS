package com.gamebox.os.importer

import java.io.File
import java.io.InputStream
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
    ): List<ExtractedFile>? {
        val extension = source.extension.lowercase()
        if (extension !in setOf("zip", "7z")) return null
        val output = staging.resolve("archive-content").apply { mkdirs() }
        val result = when (extension) {
            "zip" -> extractZip(source, output, platform, maxFiles, maxBytes)
            else -> extractSevenZip(source, output, platform, maxFiles, maxBytes)
        }
        require(result.isNotEmpty()) {
            "The archive contains no supported ${RomImportPolicy.profileLabel(platform)} game files"
        }
        return result
    }

    private fun extractZip(
        source: File,
        output: File,
        platform: String?,
        maxFiles: Int,
        maxBytes: Long,
    ): List<ExtractedFile> {
        ZipFile(source).use { zip ->
            var total = 0L
            return zip.entries().asSequence()
                .filterNot { it.isDirectory }
                .mapNotNull { entry ->
                    val name = safeEntryName(entry.name) ?: return@mapNotNull null
                    val safeName = runCatching { RomImportPolicy.safeFileName(name, platform) }.getOrNull()
                        ?: return@mapNotNull null
                    require(resultCount(output) < maxFiles) { "Archive contains too many supported game files" }
                    val target = uniqueTarget(output, safeName)
                    val written = zip.getInputStream(entry).use { input -> writeBounded(input, target, maxBytes - total) }
                    total += written
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
    ): List<ExtractedFile> {
        SevenZFile(source).use { archive ->
            var total = 0L
            val result = mutableListOf<ExtractedFile>()
            while (true) {
                val entry = archive.nextEntry ?: break
                if (entry.isDirectory) continue
                val name = safeEntryName(entry.name) ?: continue
                val safeName = runCatching { RomImportPolicy.safeFileName(name, platform) }.getOrNull() ?: continue
                require(result.size < maxFiles) { "Archive contains too many supported game files" }
                val target = uniqueTarget(output, safeName)
                val written = writeBounded(archive, target, maxBytes - total)
                total += written
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

    private fun writeBounded(input: InputStream, target: File, remaining: Long): Long {
        require(remaining > 0) { "Archive exceeds the configured import limit" }
        var total = 0L
        target.outputStream().buffered().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= remaining) { "Archive exceeds the configured import limit" }
                output.write(buffer, 0, read)
            }
        }
        return total
    }

    private fun writeBounded(input: SevenZFile, target: File, remaining: Long): Long {
        require(remaining > 0) { "Archive exceeds the configured import limit" }
        var total = 0L
        target.outputStream().buffered().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= remaining) { "Archive exceeds the configured import limit" }
                output.write(buffer, 0, read)
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

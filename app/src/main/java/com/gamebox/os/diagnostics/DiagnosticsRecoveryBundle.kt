package com.gamebox.os.diagnostics

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

const val MAX_DIAGNOSTICS_BUNDLE_BYTES: Int = 2 * 1024 * 1024

fun buildDiagnosticsRecoveryBundle(
    report: String,
    events: List<DiagnosticEvent>,
    maxBytes: Int = MAX_DIAGNOSTICS_BUNDLE_BYTES,
): ByteArray {
    require(maxBytes > 0) { "Bundle limit must be positive" }
    val eventsText = buildString {
        appendLine("GameBox OS diagnostic events")
        events.forEach { event ->
            appendLine("${event.timestampMillis} ${event.level.name} ${sanitizeDiagnosticToken(event.code)}: ${sanitizeDiagnosticToken(event.message)}")
        }
    }
    require(report.toByteArray(StandardCharsets.UTF_8).size + eventsText.toByteArray(StandardCharsets.UTF_8).size <= maxBytes) {
        "Diagnostics recovery bundle exceeds the size limit"
    }
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        fun add(name: String, value: String) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(value.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
        add("diagnostics.txt", report)
        add("events.txt", eventsText)
    }
    return output.toByteArray().also {
        require(it.size <= maxBytes) { "Diagnostics recovery bundle exceeds the size limit" }
    }
}

internal fun sanitizeDiagnosticToken(value: String): String = value
    .replace(Regex("https?://\\S+"), "<url>")
    .replace(
        Regex("(?i)(password|secret|token|api[_-]?key|access[_-]?key|secret[_-]?key)\\s*[:=]\\s*[^\\s,;]+")
    ) { match ->
        match.value.substringBefore("=").substringBefore(":").trimEnd() + "=<redacted>"
    }
    .replace(Regex("(?i)\\b(?:authorization\\s*:\\s*)?bearer\\s+[^\\s,;]+"), "Bearer <redacted>")
    .replace(Regex("(?i)\\b[a-f0-9]{64}\\b"), "<checksum>")
    .replace(Regex("(?i)(?:[a-z]:\\\\|/)(?:[^\\s]+)"), "<path>")

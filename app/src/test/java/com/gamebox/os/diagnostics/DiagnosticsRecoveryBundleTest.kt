package com.gamebox.os.diagnostics

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsRecoveryBundleTest {
    @Test
    fun bundleContainsReportAndSanitizedEvents() {
        val report = "Games installed: 1"
        val events = listOf(
            DiagnosticEvent(
                DiagnosticLevel.ERROR,
                "download_failed",
                "https://user:secret@example.test/private/file.bin checksum=" + "a".repeat(64) + " path=C:\\Users\\player\\save.dat",
                42L,
            )
        )

        val bytes = buildDiagnosticsRecoveryBundle(report, events)
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }

        assertTrue(entries["diagnostics.txt"]!!.contains(report))
        val eventText = entries["events.txt"]!!
        assertTrue(eventText.contains("<url>"))
        assertTrue(eventText.contains("<checksum>"))
        assertTrue(eventText.contains("<path>"))
        assertFalse(eventText.contains("secret"))
        assertFalse(eventText.contains("C:\\Users"))
    }

    @Test
    fun redactsBearerAndCommonCredentialFieldForms() {
        val sanitized = sanitizeDiagnosticToken(
            "Authorization: Bearer abc123 api_key=xyz access_key: qwerty password = hidden"
        )

        assertFalse(sanitized.contains("abc123"))
        assertFalse(sanitized.contains("xyz"))
        assertFalse(sanitized.contains("qwerty"))
        assertFalse(sanitized.contains("hidden"))
        assertTrue(sanitized.contains("<redacted>"))
    }

    @Test
    fun rejectsOversizedRawBundle() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            buildDiagnosticsRecoveryBundle("x".repeat(100), emptyList(), maxBytes = 32)
        }
    }
}

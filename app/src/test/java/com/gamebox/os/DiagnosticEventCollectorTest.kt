package com.gamebox.os

import com.gamebox.os.diagnostics.DiagnosticEventCollector
import com.gamebox.os.diagnostics.DiagnosticLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticEventCollectorTest {
    @Test
    fun sanitizesUrlsAndSecrets() {
        val collector = DiagnosticEventCollector { 42L }
        collector.record(DiagnosticLevel.ERROR, "download", "failed https://example.test/x?token=abc password=secret")
        val event = collector.snapshot().single()
        assertFalse(event.message.contains("https://"))
        assertFalse(event.message.contains("secret"))
        assertTrue(event.message.contains("<redacted>"))
    }
}
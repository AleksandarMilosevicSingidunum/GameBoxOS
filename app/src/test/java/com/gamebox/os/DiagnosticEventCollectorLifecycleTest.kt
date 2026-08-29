package com.gamebox.os

import com.gamebox.os.diagnostics.DiagnosticEventCollector
import com.gamebox.os.diagnostics.DiagnosticLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticEventCollectorLifecycleTest {
    @Test
    fun evictsOldestAndClears() {
        val collector = DiagnosticEventCollector(maxEvents = 2, nowMillis = { 1L })
        collector.record(DiagnosticLevel.INFO, "one", "first")
        collector.record(DiagnosticLevel.INFO, "two", "second")
        collector.record(DiagnosticLevel.INFO, "three", "third")
        assertEquals(2, collector.size())
        assertEquals("two", collector.snapshot().first().code)
        collector.clear()
        assertEquals(0, collector.size())
    }
}

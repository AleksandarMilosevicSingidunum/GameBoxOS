package com.gamebox.os.diagnostics

enum class DiagnosticLevel { INFO, WARNING, ERROR }

data class DiagnosticEvent(val level: DiagnosticLevel, val code: String, val message: String, val timestampMillis: Long)

class DiagnosticEventCollector(
    private val maxEvents: Int = 500,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    init { require(maxEvents > 0) { "maxEvents must be positive" } }

    private val events = mutableListOf<DiagnosticEvent>()

    fun record(level: DiagnosticLevel, code: String, message: String) {
        events += DiagnosticEvent(level, sanitize(code), sanitize(message), nowMillis())
        if (events.size > maxEvents) events.removeAt(0)
    }

    fun snapshot(): List<DiagnosticEvent> = events.toList()

    fun size(): Int = events.size

    fun clear() { events.clear() }

    private fun sanitize(message: String): String = sanitizeDiagnosticToken(message)
}

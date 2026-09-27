package com.gamebox.os.companion

/** Escapes a string value for the small JSON responses emitted by the companion protocol. */
internal fun String.companionJsonEscape(): String = buildString {
    for (character in this@companionJsonEscape) {
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            else -> if (character.code < 0x20) append("\\u%04x".format(character.code)) else append(character)
        }
    }
}

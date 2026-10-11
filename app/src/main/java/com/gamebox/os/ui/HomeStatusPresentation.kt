package com.gamebox.os.ui

internal enum class HomeStatusTone {
    HEALTHY,
    ATTENTION,
}

internal fun homeStatusTone(label: String, value: String): HomeStatusTone? = when (label) {
    "Network" -> when {
        value.equals("Offline", ignoreCase = true) ||
            value.equals("Not connected", ignoreCase = true) ||
            value.contains("disconnected", ignoreCase = true) -> HomeStatusTone.ATTENTION
        value.equals("Internet connected", ignoreCase = true) ||
            value.startsWith("Local network", ignoreCase = true) -> HomeStatusTone.HEALTHY
        else -> null
    }
    "Controller" -> when {
        value.equals("Not connected", ignoreCase = true) -> HomeStatusTone.ATTENTION
        value.isNotBlank() -> HomeStatusTone.HEALTHY
        else -> null
    }
    else -> null
}

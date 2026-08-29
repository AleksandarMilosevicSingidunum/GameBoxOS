package com.gamebox.os.launch

enum class MoonlightConnectivity {
    OFFLINE,
    LOCAL_NETWORK,
    INTERNET
}

data class MoonlightStatus(
    val connectivity: MoonlightConnectivity,
    val moonlightInstalled: Boolean,
    val recentSessions: List<String> = emptyList()
)

fun classifyMoonlightConnectivity(
    hasNetwork: Boolean,
    hasLocalTransport: Boolean
): MoonlightConnectivity = when {
    !hasNetwork -> MoonlightConnectivity.OFFLINE
    hasLocalTransport -> MoonlightConnectivity.LOCAL_NETWORK
    else -> MoonlightConnectivity.INTERNET
}

fun addRecentMoonlightSession(
    existing: List<String>,
    sessionLabel: String,
    maxEntries: Int = 5
): List<String> {
    require(maxEntries > 0) { "Maximum recent sessions must be positive" }
    val label = sessionLabel.trim()
    if (label.isEmpty()) return existing.take(maxEntries)
    return (listOf(label) + existing.filterNot { it == label }).take(maxEntries)
}

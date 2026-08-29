package com.gamebox.os.ui

fun visibleShortcutPackageNames(
    configuredPackageNames: List<String>,
    installedPackageNames: Set<String>,
    showUnavailable: Boolean
): List<String> {
    val uniqueConfigured = configuredPackageNames
        .filter { it.isNotBlank() }
        .distinct()
    return if (showUnavailable) {
        uniqueConfigured
    } else {
        uniqueConfigured.filter { it in installedPackageNames }
    }
}

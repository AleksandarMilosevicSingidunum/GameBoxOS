package com.gamebox.os.settings

private const val MAX_HIDDEN_SHORTCUTS = 64

internal fun encodeHiddenShortcutPackages(packages: Set<String>): String =
    packages.asSequence()
        .filter(::isSafeShortcutPackage)
        .sorted()
        .take(MAX_HIDDEN_SHORTCUTS)
        .joinToString("\n")

internal fun decodeHiddenShortcutPackages(value: String?): Set<String> =
    value.orEmpty().lineSequence()
        .map(String::trim)
        .filter(::isSafeShortcutPackage)
        .distinct()
        .take(MAX_HIDDEN_SHORTCUTS)
        .toSet()

internal fun updateShortcutVisibility(
    hiddenPackages: Set<String>,
    packageName: String,
    visible: Boolean,
): Set<String> {
    require(isSafeShortcutPackage(packageName)) { "Invalid app package name" }
    return if (visible) hiddenPackages - packageName
    else (hiddenPackages + packageName).take(MAX_HIDDEN_SHORTCUTS).toSet()
}

private fun isSafeShortcutPackage(value: String): Boolean =
    value.isNotEmpty() && value.length <= 255 &&
        value.all { it.isLetterOrDigit() || it == '.' || it == '_' }

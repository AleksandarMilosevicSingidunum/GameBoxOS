package com.gamebox.os.settings

data class ShortcutLaunchRecord(
    val packageName: String,
    val launchedAtEpochMs: Long,
)

private const val MAX_SHORTCUT_HISTORY = 12

internal fun addShortcutLaunch(
    current: List<ShortcutLaunchRecord>,
    packageName: String,
    launchedAtEpochMs: Long,
): List<ShortcutLaunchRecord> {
    val normalized = packageName.trim()
    require(normalized.isNotEmpty() && normalized.length <= 255)
    require(normalized.all { it.isLetterOrDigit() || it == '.' || it == '_' })
    require(launchedAtEpochMs > 0L)
    return (listOf(ShortcutLaunchRecord(normalized, launchedAtEpochMs)) +
        current.filterNot { it.packageName == normalized })
        .sortedByDescending(ShortcutLaunchRecord::launchedAtEpochMs)
        .take(MAX_SHORTCUT_HISTORY)
}

internal fun encodeShortcutLaunchHistory(records: List<ShortcutLaunchRecord>): String =
    records.take(MAX_SHORTCUT_HISTORY).joinToString("\n") {
        it.packageName + "\t" + it.launchedAtEpochMs
    }

internal fun decodeShortcutLaunchHistory(value: String?): List<ShortcutLaunchRecord> =
    value.orEmpty().lineSequence().mapNotNull { line ->
        val separator = line.indexOf('\t')
        if (separator <= 0 || separator == line.lastIndex) return@mapNotNull null
        val packageName = line.substring(0, separator)
        val epochMs = line.substring(separator + 1).toLongOrNull() ?: return@mapNotNull null
        if (packageName.length > 255 ||
            packageName.any { !it.isLetterOrDigit() && it != '.' && it != '_' } ||
            epochMs <= 0L
        ) null else ShortcutLaunchRecord(packageName, epochMs)
    }.distinctBy(ShortcutLaunchRecord::packageName)
        .sortedByDescending(ShortcutLaunchRecord::launchedAtEpochMs)
        .take(MAX_SHORTCUT_HISTORY)
        .toList()

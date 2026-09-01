package com.gamebox.os.ui

enum class SettingsSection(val label: String) {
    STORAGE("Storage"), CONTROLLERS("Controllers"), DOWNLOADS("Downloads"),
    EMULATORS("Emulators"), DISPLAY("Display"), AUDIO("Audio"), NETWORK("Network"),
    SAVES_CLOUD("Saves & Cloud Sync"), SYSTEM("System"),
}

object SettingsNavigationPolicy {
    fun selectedSection(
        scrollOffset: Int,
        sectionOffsets: Map<SettingsSection, Int>,
        viewportLead: Int = 24,
    ): SettingsSection {
        val threshold = (scrollOffset + viewportLead).coerceAtLeast(0)
        return SettingsSection.entries
            .mapNotNull { section -> sectionOffsets[section]?.let { section to it } }
            .filter { (_, offset) -> offset <= threshold }
            .maxByOrNull { (_, offset) -> offset }
            ?.first
            ?: SettingsSection.entries.firstOrNull { it in sectionOffsets }
            ?: SettingsSection.STORAGE
    }
}


package com.gamebox.os.settings

enum class DeveloperLayoutMode {
    AUTO,
    WIDE,
    COMPACT;

    companion object {
        fun fromStored(value: String?): DeveloperLayoutMode =
            entries.firstOrNull { it.name == value } ?: AUTO
    }
}

enum class CatalogFailureSimulation {
    LIVE,
    OFFLINE_FALLBACK,
    REMOTE_FALLBACK,
    ERROR;

    companion object {
        fun fromStored(value: String?): CatalogFailureSimulation =
            entries.firstOrNull { it.name == value } ?: LIVE
    }
}

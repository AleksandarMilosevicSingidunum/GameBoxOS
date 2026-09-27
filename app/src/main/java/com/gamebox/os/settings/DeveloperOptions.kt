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

internal fun effectiveCatalogRefreshState(
    liveState: com.gamebox.os.domain.CatalogRefreshState,
    simulation: CatalogFailureSimulation,
    allowSimulation: Boolean,
): com.gamebox.os.domain.CatalogRefreshState =
    if (!allowSimulation) {
        liveState
    } else {
        when (simulation) {
            CatalogFailureSimulation.LIVE -> liveState
            CatalogFailureSimulation.OFFLINE_FALLBACK -> com.gamebox.os.domain.CatalogRefreshState.OFFLINE_FALLBACK
            CatalogFailureSimulation.REMOTE_FALLBACK -> com.gamebox.os.domain.CatalogRefreshState.REMOTE_FALLBACK
            CatalogFailureSimulation.ERROR -> com.gamebox.os.domain.CatalogRefreshState.ERROR
        }
    }

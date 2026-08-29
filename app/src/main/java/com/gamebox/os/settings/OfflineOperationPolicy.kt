package com.gamebox.os.settings

enum class OfflineAction { USE_CACHED_CATALOG, OPEN_LOCAL_LIBRARY, REQUIRE_NETWORK }

class OfflineOperationPolicy(private val offline: () -> Boolean) {
    fun actionFor(requiresNetwork: Boolean, hasCachedData: Boolean): OfflineAction = when {
        !offline() -> if (requiresNetwork) OfflineAction.REQUIRE_NETWORK else OfflineAction.OPEN_LOCAL_LIBRARY
        requiresNetwork -> if (hasCachedData) OfflineAction.USE_CACHED_CATALOG else OfflineAction.REQUIRE_NETWORK
        else -> OfflineAction.OPEN_LOCAL_LIBRARY
    }
}
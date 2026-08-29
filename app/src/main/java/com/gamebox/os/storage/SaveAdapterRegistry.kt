package com.gamebox.os.storage

class SaveAdapterRegistry(adapters: Map<String, SaveAdapter>) {
    private val adaptersByPlatform = adapters.mapKeys { (platform, _) -> normalize(platform) }

    init {
        require(adapters.keys.none { it.isBlank() }) { "Save adapter platform cannot be blank" }
        require(adaptersByPlatform.size == adapters.size) { "Duplicate normalized save adapter platform" }
    }

    fun adapterFor(platform: String): SaveAdapter? = adaptersByPlatform[normalize(platform)]

    fun inspect(platform: String, gameId: String): SaveSummary {
        val adapter = adapterFor(platform)
            ?: return SaveSummary(
                gameId = gameId,
                presence = SavePresence.ERROR,
                message = "Save discovery is not configured for platform: " + platform,
            )
        return SaveInspectionService(adapter).inspect(gameId)
    }

    private companion object {
        fun normalize(platform: String): String = platform.trim().lowercase()
    }
}

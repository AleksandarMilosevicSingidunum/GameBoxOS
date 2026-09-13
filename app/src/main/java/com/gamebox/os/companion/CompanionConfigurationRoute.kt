package com.gamebox.os.companion

import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.flow.first

internal data class CompanionConfiguration(
    val reducedMotion: Boolean,
    val showUnavailableGames: Boolean,
    val showUnavailableShortcuts: Boolean,
    val downloadsUnmeteredOnly: Boolean,
)

internal interface CompanionConfigurationStore {
    suspend fun read(): CompanionConfiguration
    suspend fun write(configuration: CompanionConfiguration)
}

internal class SettingsCompanionConfigurationStore(
    private val repository: SettingsRepository,
) : CompanionConfigurationStore {
    override suspend fun read(): CompanionConfiguration {
        val settings = repository.settings.first()
        return CompanionConfiguration(
            reducedMotion = settings.reducedMotion,
            showUnavailableGames = settings.showUnavailableGames,
            showUnavailableShortcuts = settings.showUnavailableShortcuts,
            downloadsUnmeteredOnly = settings.downloadsUnmeteredOnly,
        )
    }

    override suspend fun write(configuration: CompanionConfiguration) {
        repository.setCompanionManagedPreferences(
            reducedMotion = configuration.reducedMotion,
            showUnavailableGames = configuration.showUnavailableGames,
            showUnavailableShortcuts = configuration.showUnavailableShortcuts,
            downloadsUnmeteredOnly = configuration.downloadsUnmeteredOnly,
        )
    }
}

internal object CompanionConfigurationRoute {
    const val PATH = "/v1/config"

    suspend fun handle(
        request: CompanionHttpRequest,
        pairingSecret: String?,
        store: CompanionConfigurationStore,
        nowUnixTimeSeconds: Long,
    ): CompanionHttpResponse {
        if (request.path != PATH || request.method !in setOf("GET", "PUT") ||
            request.bodyLength != 0L) {
            return CompanionHttpResponse(404, """{"error":"not_found"}""")
        }
        if (pairingSecret.isNullOrBlank() || !CompanionProtocol.verifyAuthorization(
                pairingSecret, request.method, request.path, request.authorization,
                nowUnixTimeSeconds,
            )) {
            return CompanionHttpResponse(401, """{"error":"unauthorized"}""")
        }
        return when (request.method) {
            "GET" -> runCatching { store.read() }.fold(
                onSuccess = { CompanionHttpResponse(200, it.toJson()) },
                onFailure = { CompanionHttpResponse(400, """{"error":"config_unavailable"}""") },
            )
            "PUT" -> {
                val encoded = request.fileName
                val configuration = runCatching { parseConfiguration(encoded) }.getOrNull()
                    ?: return CompanionHttpResponse(400, """{"error":"config_invalid"}""")
                runCatching { store.write(configuration) }.fold(
                    onSuccess = { CompanionHttpResponse(200, configuration.toJson()) },
                    onFailure = { CompanionHttpResponse(400, """{"error":"config_rejected"}""") },
                )
            }
            else -> CompanionHttpResponse(404, """{"error":"not_found"}""")
        }
    }

    private fun CompanionConfiguration.toJson(): String =
        "{\"protocolVersion\":" + CompanionProtocol.VERSION +
            ",\"reducedMotion\":" + reducedMotion +
            ",\"showUnavailableGames\":" + showUnavailableGames +
            ",\"showUnavailableShortcuts\":" + showUnavailableShortcuts +
            ",\"downloadsUnmeteredOnly\":" + downloadsUnmeteredOnly + "}"

    /**
     * A body-free PUT carries exactly four bits as an ASCII header value, in JSON field order.
     * This keeps configuration requests authenticated before any body is read.
     */
    private fun parseConfiguration(value: String?): CompanionConfiguration {
        require(value != null && value.matches(Regex("^[01]{4}$"))) {
            "Configuration flags are invalid"
        }
        return CompanionConfiguration(
            reducedMotion = value[0] == '1',
            showUnavailableGames = value[1] == '1',
            showUnavailableShortcuts = value[2] == '1',
            downloadsUnmeteredOnly = value[3] == '1',
        )
    }
}

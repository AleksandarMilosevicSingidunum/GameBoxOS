package com.gamebox.os.catalog

import com.gamebox.os.provider.ProviderFailureKind

enum class ProviderHealthStatus {
    NOT_CONFIGURED,
    HEALTHY,
    AUTHENTICATION_REQUIRED,
    RATE_LIMITED,
    UNREACHABLE,
    DEGRADED,
}

data class ProviderHealth(
    val provider: String = "TheGamesDB",
    val status: ProviderHealthStatus = ProviderHealthStatus.NOT_CONFIGURED,
    val lastAttemptAtMillis: Long? = null,
    val lastSuccessAtMillis: Long? = null,
    val latencyMillis: Long? = null,
    val retryAfterMillis: Long? = null,
    val message: String? = null,
) {
    init {
        require(provider.isNotBlank() && provider.length <= 80)
        require(lastAttemptAtMillis == null || lastAttemptAtMillis >= 0)
        require(lastSuccessAtMillis == null || lastSuccessAtMillis >= 0)
        require(latencyMillis == null || latencyMillis >= 0)
        require(retryAfterMillis == null || retryAfterMillis >= 0)
        require(message == null || message.length <= 200)
    }
}

internal fun ProviderFailureKind.toProviderHealthStatus(): ProviderHealthStatus = when (this) {
    ProviderFailureKind.AUTHENTICATION -> ProviderHealthStatus.AUTHENTICATION_REQUIRED
    ProviderFailureKind.RATE_LIMITED -> ProviderHealthStatus.RATE_LIMITED
    ProviderFailureKind.TRANSIENT_NETWORK -> ProviderHealthStatus.UNREACHABLE
    ProviderFailureKind.NOT_FOUND,
    ProviderFailureKind.UNSUPPORTED,
    ProviderFailureKind.PERMANENT -> ProviderHealthStatus.DEGRADED
}

fun providerHealthSummary(health: ProviderHealth, nowMillis: Long = System.currentTimeMillis()): String {
    val state = when (health.status) {
        ProviderHealthStatus.NOT_CONFIGURED -> "API key required"
        ProviderHealthStatus.HEALTHY -> "Healthy"
        ProviderHealthStatus.AUTHENTICATION_REQUIRED -> "Authentication required"
        ProviderHealthStatus.RATE_LIMITED -> "Rate limited"
        ProviderHealthStatus.UNREACHABLE -> "Temporarily unreachable"
        ProviderHealthStatus.DEGRADED -> "Needs attention"
    }
    val details = buildList {
        health.latencyMillis?.let { add("${it} ms") }
        health.lastSuccessAtMillis?.let { success ->
            val ageMinutes = ((nowMillis - success).coerceAtLeast(0L) / 60_000L)
            add(
                when {
                    ageMinutes < 1 -> "last success now"
                    ageMinutes < 60 -> "last success ${ageMinutes}m ago"
                    else -> "last success ${ageMinutes / 60}h ago"
                }
            )
        }
        health.retryAfterMillis?.takeIf { it > nowMillis }?.let { retry ->
            val waitMinutes = ((retry - nowMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
            add("retry in ${waitMinutes}m")
        }
    }
    return (listOf(state) + details).joinToString(" • ")
}

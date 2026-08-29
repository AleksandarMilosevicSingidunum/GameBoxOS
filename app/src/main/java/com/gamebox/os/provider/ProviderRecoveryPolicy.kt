package com.gamebox.os.provider

enum class ProviderFailureKind { AUTHENTICATION, RATE_LIMITED, TRANSIENT_NETWORK, NOT_FOUND, UNSUPPORTED, PERMANENT }

data class ProviderRecoveryDecision(val kind: ProviderFailureKind, val retryable: Boolean, val delayMillis: Long, val userMessage: String)

object ProviderRecoveryPolicy {
    fun classify(httpStatus: Int?, throwable: Throwable? = null, attempt: Int = 0): ProviderRecoveryDecision {
        require(attempt >= 0) { "attempt must not be negative" }
        val boundedAttempt = attempt.coerceAtMost(6)
        val kind = when {
            httpStatus == 401 || httpStatus == 403 -> ProviderFailureKind.AUTHENTICATION
            httpStatus == 404 -> ProviderFailureKind.NOT_FOUND
            httpStatus == 408 || httpStatus == 425 || httpStatus == 429 -> ProviderFailureKind.RATE_LIMITED
            httpStatus != null && httpStatus in 500..599 -> ProviderFailureKind.TRANSIENT_NETWORK
            throwable is java.io.IOException -> ProviderFailureKind.TRANSIENT_NETWORK
            httpStatus == null -> ProviderFailureKind.PERMANENT
            else -> ProviderFailureKind.UNSUPPORTED
        }
        val retryable = kind == ProviderFailureKind.TRANSIENT_NETWORK || kind == ProviderFailureKind.RATE_LIMITED
        val delay = if (retryable) (1L shl boundedAttempt) * 1_000L else 0L
        val message = when (kind) {
            ProviderFailureKind.AUTHENTICATION -> "Provider credentials are invalid or expired"
            ProviderFailureKind.RATE_LIMITED -> "Provider requested a retry later"
            ProviderFailureKind.TRANSIENT_NETWORK -> "Provider is temporarily unreachable"
            ProviderFailureKind.NOT_FOUND -> "Requested provider content was not found"
            ProviderFailureKind.UNSUPPORTED -> "Provider response is not supported"
            ProviderFailureKind.PERMANENT -> "Provider request failed"
        }
        return ProviderRecoveryDecision(kind, retryable, delay, message)
    }
}

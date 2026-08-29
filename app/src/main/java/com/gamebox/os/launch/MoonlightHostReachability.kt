package com.gamebox.os.launch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

enum class HostReachability { REACHABLE, UNREACHABLE, INVALID }

data class HostReachabilityResult(val state: HostReachability, val host: String, val port: Int, val message: String)

/** Bounded TCP reachability probe for a user-configured PC host. It never sends credentials or payload data. */
suspend fun probeMoonlightHost(host: String, port: Int = 47984, timeoutMillis: Int = 1_500): HostReachabilityResult = withContext(Dispatchers.IO) {
    val normalized = host.trim()
    if (normalized.isBlank() || normalized.length > 253 || port !in 1..65_535 || timeoutMillis !in 100..5_000) {
        return@withContext HostReachabilityResult(HostReachability.INVALID, normalized, port, "Enter a valid host and port")
    }
    runCatching {
        Socket().use { socket -> socket.connect(InetSocketAddress(normalized, port), timeoutMillis) }
    }.fold(
        onSuccess = { HostReachabilityResult(HostReachability.REACHABLE, normalized, port, "PC host is reachable") },
        onFailure = { HostReachabilityResult(HostReachability.UNREACHABLE, normalized, port, "PC host is not reachable on this network") }
    )
}

package com.gamebox.os.companion

import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.GameId

/** Authenticated, idempotent metadata management for the paired Android library. */
internal object CompanionLibraryManagementRoute {
    const val PREFIX = "/v1/library/"
    private val gameIdPattern = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,95}")

    fun handle(
        request: CompanionHttpRequest,
        pairingSecret: String?,
        games: GameRepository,
        nowUnixTimeSeconds: Long,
    ): CompanionHttpResponse {
        val segments = request.path.removePrefix(PREFIX).split('/')
        if (!request.path.startsWith(PREFIX) || segments.size != 3 ||
            segments[1] != "favorite" || !gameIdPattern.matches(segments[0]) ||
            segments[2] !in setOf("on", "off")) {
            return CompanionHttpResponse(404, """{"error":"not_found"}""")
        }
        if (request.method != "PUT" || request.bodyLength != 0L) {
            return CompanionHttpResponse(400, """{"error":"method_not_supported"}""")
        }
        if (pairingSecret.isNullOrBlank() || !CompanionProtocol.verifyAuthorization(
                pairingSecret, request.method, request.path, request.authorization,
                nowUnixTimeSeconds,
            )) {
            return CompanionHttpResponse(401, """{"error":"unauthorized"}""")
        }
        val id = GameId(segments[0])
        val game = games.game(id) ?: return CompanionHttpResponse(404, """{"error":"game_not_found"}""")
        val favorite = segments[2] == "on"
        return runCatching {
            games.setFavorite(id, favorite)
            CompanionHttpResponse(
                200,
                "{\"protocolVersion\":" + CompanionProtocol.VERSION +
                    ",\"gameId\":\"" + game.id.value +
                    "\",\"favorite\":" + favorite + "}",
            )
        }.getOrElse { CompanionHttpResponse(400, """{"error":"management_rejected"}""") }
    }
}

package com.gamebox.os.companion

/** Minimal, read-only library data suitable for a paired companion device. */
data class CompanionLibraryItem(
    val id: String,
    val title: String,
    val platform: String,
    val installState: String,
    val favorite: Boolean,
    val minutesPlayed: Int,
    val savePresent: Boolean,
)

object CompanionLibraryRoute {
    const val PATH = "/v1/library"

    fun handle(
        method: String,
        path: String,
        authorization: String?,
        pairingSecret: String?,
        library: List<CompanionLibraryItem>,
        nowUnixTimeSeconds: Long,
    ): CompanionHttpResponse {
        if (method.uppercase() != "GET" || path != PATH) return CompanionHttpResponse(404, """{"error":"not_found"}""")
        if (pairingSecret.isNullOrBlank() ||
            !CompanionProtocol.verifyAuthorization(pairingSecret, method, path, authorization, nowUnixTimeSeconds)
        ) return CompanionHttpResponse(401, """{"error":"unauthorized"}""")
        return CompanionHttpResponse(200, """{"protocolVersion":1,"games":[${library.joinToString(",") { it.toJson() }}]}""")
    }

    private fun CompanionLibraryItem.toJson() =
        """{"id":"${id.jsonEscape()}","title":"${title.jsonEscape()}","platform":"${platform.jsonEscape()}","installState":"${installState.jsonEscape()}","favorite":$favorite,"minutesPlayed":${minutesPlayed.coerceAtLeast(0)},"savePresent":$savePresent}"""

    private fun String.jsonEscape() = buildString {
        this@jsonEscape.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) append("\\u%04x".format(character.code)) else append(character)
            }
        }
    }
}

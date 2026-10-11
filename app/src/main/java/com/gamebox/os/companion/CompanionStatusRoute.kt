package com.gamebox.os.companion

/** Pure authenticated status route used by the Android companion LAN service. */
data class CompanionHttpResponse(val status: Int, val body: String)

object CompanionStatusRoute {
    const val PATH = "/v1/status"

    fun handle(
        method: String,
        path: String,
        authorization: String?,
        pairingSecret: String?,
        deviceName: String,
        nowUnixTimeSeconds: Long,
    ): CompanionHttpResponse {
        if (method.uppercase() != "GET" || path != PATH) {
            return CompanionHttpResponse(404, """{"error":"not_found"}""")
        }
        if (pairingSecret.isNullOrBlank() ||
            !CompanionProtocol.verifyAuthorization(
                pairingSecret, method, path, authorization, nowUnixTimeSeconds,
            )
        ) {
            return CompanionHttpResponse(401, """{"error":"unauthorized"}""")
        }
        val safeName = deviceName.companionJsonEscape()
        return CompanionHttpResponse(
            200,
            """{"protocolVersion":${CompanionProtocol.VERSION},"deviceName":"$safeName","status":"ready"}""",
        )
    }
}

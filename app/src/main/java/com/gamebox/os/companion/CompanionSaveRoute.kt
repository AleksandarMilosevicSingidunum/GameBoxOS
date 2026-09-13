package com.gamebox.os.companion

import java.util.Base64

internal object CompanionSaveRoute {
    const val PREFIX = "/v1/saves/"

    suspend fun handle(
        request: CompanionHttpRequest,
        pairingSecret: String?,
        store: CompanionSaveTransferStore,
        nowUnixTimeSeconds: Long,
    ): CompanionHttpResponse {
        val gameId = request.path.removePrefix(PREFIX)
        if (!request.path.startsWith(PREFIX) || gameId.contains('/') ||
            runCatching { CompanionSaveTransferStore.requireGameId(gameId) }.isFailure) {
            return CompanionHttpResponse(404, """{"error":"not_found"}""")
        }
        if (pairingSecret == null || !CompanionProtocol.verifyAuthorization(
                pairingSecret,
                request.method,
                request.path,
                request.authorization,
                nowUnixTimeSeconds,
            )
        ) {
            return CompanionHttpResponse(401, """{"error":"unauthorized"}""")
        }
        return when (request.method) {
            "GET" -> runCatching { store.export(gameId) }.fold(
                onSuccess = { payload ->
                    if (payload == null) CompanionHttpResponse(404, """{"error":"save_not_found"}""")
                    else CompanionHttpResponse(
                        200,
                        """{"protocolVersion":""" + CompanionProtocol.VERSION +
                            ""","gameId":"""" + payload.gameId +
                            """","updatedAtMillis":""" + payload.updatedAtMillis +
                            ""","sha256":"""" + payload.sha256 +
                            """","payloadBase64":"""" +
                            Base64.getEncoder().encodeToString(payload.bytes) + """"}""",
                    )
                },
                onFailure = { CompanionHttpResponse(400, """{"error":"save_unavailable"}""") },
            )
            "PUT" -> runCatching { store.import(gameId, request.body) }.fold(
                onSuccess = { result ->
                    CompanionHttpResponse(
                        200,
                        """{"protocolVersion":""" + CompanionProtocol.VERSION +
                            ""","gameId":"""" + result.gameId +
                            """","updatedAtMillis":""" + result.updatedAtMillis +
                            ""","sha256":"""" + result.sha256 +
                            """","conflictPreserved":""" + result.conflictPreserved + "}",
                    )
                },
                onFailure = { CompanionHttpResponse(400, """{"error":"save_rejected"}""") },
            )
            else -> CompanionHttpResponse(400, """{"error":"method_not_supported"}""")
        }
    }
}

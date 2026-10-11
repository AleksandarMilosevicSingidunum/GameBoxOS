package com.gamebox.os.net

import com.gamebox.os.catalog.CatalogCredentials
import java.util.Base64

/** Builds a standards-compatible Basic authorization value for complete credentials only. */
internal fun basicAuthorizationHeader(credentials: CatalogCredentials): String? {
    if (!credentials.hasBasicAuth()) return null
    val username = requireNotNull(credentials.username)
    val password = requireNotNull(credentials.password)
    val token = Base64.getEncoder().encodeToString("$username:$password".toByteArray(Charsets.UTF_8))
    return "Basic $token"
}

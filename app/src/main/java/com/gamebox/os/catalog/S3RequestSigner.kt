package com.gamebox.os.catalog

data class SignedRequest(val authorization: String, val date: String)

interface S3RequestSigner {
    fun sign(method: String, uri: String, payloadSha256: String, credentials: CatalogCredentials): SignedRequest
}


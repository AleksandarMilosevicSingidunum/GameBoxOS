package com.gamebox.os.catalog

data class SignedRequest(val authorization: String, val date: String)

interface S3RequestSigner {
    fun sign(method: String, uri: String, payloadSha256: String, credentials: CatalogCredentials): SignedRequest
}

class UnsupportedS3RequestSigner : S3RequestSigner {
    override fun sign(method: String, uri: String, payloadSha256: String, credentials: CatalogCredentials): SignedRequest {
        throw UnsupportedOperationException("AWS Signature V4 signing is not implemented")
    }
}
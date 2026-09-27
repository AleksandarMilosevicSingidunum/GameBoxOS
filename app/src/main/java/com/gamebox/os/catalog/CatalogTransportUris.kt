package com.gamebox.os.catalog

import java.net.URI
import java.net.URLEncoder

fun CatalogTransport.WebDav.catalogUri(fileName: String = "catalog.json"): URI {
    require(
        fileName.isNotBlank() &&
            !fileName.contains('\\') &&
            !fileName.contains('?') &&
            !fileName.contains('#') &&
            fileName != "." &&
            fileName != ".." &&
            !fileName.contains('/')
    ) { "invalid catalog file name" }
    return URI(baseUrl.trimEnd('/') + "/" + URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"))
}

fun CatalogTransport.S3.objectUri(objectName: String = "catalog.json"): URI {
    require(
        objectName.isNotBlank() &&
            !objectName.contains('\\') &&
            !objectName.contains('?') &&
            !objectName.contains('#') &&
            objectName.split('/').none { it == "." || it == ".." }
    ) { "invalid S3 object name" }
    val path = (prefix.trim('/') + "/" + objectName).trimStart('/')
    return URI(endpoint.trimEnd('/') + "/" + bucket + "/" + path.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") })
}

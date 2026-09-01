package com.gamebox.os.save

import java.net.URI

enum class CloudSaveProvider { WEBDAV, S3 }

object CloudSaveEndpointPolicy {
    fun objectUri(baseUrl: String, gameId: String): URI {
        require(gameId.matches(Regex("^[a-z0-9][a-z0-9-]{0,95}$"))) { "Cloud save game id is invalid" }
        val base = URI(baseUrl.trim())
        require(base.scheme.equals("https", ignoreCase = true) && !base.host.isNullOrBlank()) {
            "Cloud save endpoint must be absolute HTTPS"
        }
        require(base.userInfo == null && base.query == null && base.fragment == null) {
            "Cloud save endpoint must not embed credentials, query, or fragment"
        }
        require(base.rawPath.orEmpty().split('/').none { it == ".." || it.equals("%2e%2e", true) }) {
            "Cloud save endpoint must not contain traversal segments"
        }
        val basePath = base.rawPath.orEmpty().trimEnd('/')
        val objectPath = "$basePath/$gameId.gamebox-save"
        return URI(base.scheme.lowercase() + "://" + base.rawAuthority + objectPath)
    }

    fun requireRegion(provider: CloudSaveProvider, region: String): String {
        if (provider == CloudSaveProvider.WEBDAV) return ""
        val normalized = region.trim().lowercase()
        require(normalized.matches(Regex("^[a-z]{2}(?:-gov)?-[a-z]+-\d$"))) { "S3 region is invalid" }
        return normalized
    }
}

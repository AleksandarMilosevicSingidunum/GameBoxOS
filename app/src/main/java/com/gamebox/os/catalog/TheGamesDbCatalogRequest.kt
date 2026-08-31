package com.gamebox.os.catalog

import java.net.URI
import java.net.URLEncoder

object TheGamesDbCatalogRequest {
    private const val BASE = "https://api.thegamesdb.net/v1/"

    fun platforms(apiKey: String): URI = endpoint(
        path = "Platforms",
        apiKey = apiKey,
        parameters = emptyMap(),
    )

    fun gamesByPlatform(apiKey: String, platformId: String, page: Int): URI {
        require(platformId.matches(Regex("[0-9]{1,12}"))) { "Invalid TheGamesDB platform id" }
        require(page in 1..10_000) { "Invalid catalog page" }
        return endpoint(
            path = "Games/ByPlatformID",
            apiKey = apiKey,
            parameters = linkedMapOf(
                "id" to platformId,
                "fields" to "players,publishers,genres,overview,rating",
                "include" to "boxart",
                "page" to page.toString(),
            ),
        )
    }

    private fun endpoint(path: String, apiKey: String, parameters: Map<String, String>): URI {
        val key = apiKey.trim()
        require(key.isNotEmpty()) { "TheGamesDB API key is required" }
        val query = linkedMapOf("apikey" to key).apply { putAll(parameters) }
            .entries.joinToString("&") { (name, value) ->
                encode(name) + "=" + encode(value)
            }
        return URI(BASE + path + "?" + query).also {
            require(it.scheme == "https" && it.host == "api.thegamesdb.net" && it.userInfo == null)
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}

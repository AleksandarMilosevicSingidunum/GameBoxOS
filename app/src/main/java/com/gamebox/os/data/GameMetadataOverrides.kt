package com.gamebox.os.data

import com.gamebox.os.domain.GameMetadataOverrides
import java.net.URI

internal fun normalizeMetadataOverrides(value: GameMetadataOverrides): GameMetadataOverrides {
    fun optionalText(input: String?, limit: Int, label: String): String? =
        input?.trim()?.takeIf(String::isNotEmpty)?.also {
            require(it.length <= limit && it.none(Char::isISOControl)) { "$label is invalid" }
        }

    val artwork = optionalText(value.artworkUrl, 2_048, "Artwork URL")?.also { raw ->
        val uri = runCatching { URI(raw) }.getOrNull()
        require(
            uri != null &&
                uri.isAbsolute &&
                uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.fragment == null
        ) { "Artwork must use a credential-free HTTPS URL" }
    }
    val year = value.year?.also {
        require(it in 1900..2100) { "Year must be between 1900 and 2100" }
    }
    return GameMetadataOverrides(
        title = optionalText(value.title, 120, "Title"),
        year = year,
        genre = optionalText(value.genre, 80, "Genre"),
        artworkUrl = artwork,
        description = optionalText(value.description, 4_000, "Description"),
    )
}

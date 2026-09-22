package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.gamebox.os.domain.Game

@Composable
internal fun MetadataFilterControls(
    games: List<Game>,
    genre: String?,
    onGenre: (String?) -> Unit,
    region: String?,
    onRegion: (String?) -> Unit,
    language: String?,
    onLanguage: (String?) -> Unit,
    extraRegions: List<String> = emptyList(),
) {
    Column {
        MetadataFilterControl("Genre", games.map { it.genre }, genre, onGenre)
        MetadataFilterControl("Region", games.mapNotNull { it.region } + extraRegions, region, onRegion)
        MetadataFilterControl("Language", games.mapNotNull { it.language }, language, onLanguage)
    }
}

internal fun metadataFilterOptions(values: List<String>): List<String> =
    values.filter(String::isNotBlank).distinctBy { it.lowercase(java.util.Locale.ROOT) }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)

/** All -> each known value -> All; a removed selection always has a way back to All. */
internal fun nextMetadataFilterValue(current: String?, values: List<String>): String? {
    val options = metadataFilterOptions(values)
    if (current == null) return options.firstOrNull()
    val index = options.indexOfFirst { it.equals(current, ignoreCase = true) }
    return if (index < 0) null else options.getOrNull(index + 1)
}

@Composable
internal fun MetadataFilterControl(
    label: String,
    values: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit,
) {
    TextButton(
        onClick = { onSelected(nextMetadataFilterValue(selected, values)) },
        enabled = selected != null || values.any(String::isNotBlank),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "$label: " + (selected ?: "All"),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

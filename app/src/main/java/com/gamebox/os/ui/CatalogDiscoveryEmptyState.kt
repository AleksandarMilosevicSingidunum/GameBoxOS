package com.gamebox.os.ui

internal fun catalogDiscoveryEmptyMessage(
    favoritesOnly: Boolean,
    query: String,
    hasConfiguredJsonSource: Boolean,
): String = when {
    favoritesOnly || query.isNotBlank() -> "No discovered games match these filters."
    hasConfiguredJsonSource ->
        "No catalog matches yet. Try a different title, or search one of your configured game sources below."
    else -> "No catalog titles match this console yet. Try a different console or search a configured source below."
}

package com.gamebox.os.ui

import androidx.compose.foundation.focusable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned

internal fun shouldAutofocusCatalogAction(
    emptyCatalog: Boolean,
    hasControllerNavigationRail: Boolean,
): Boolean = emptyCatalog && !hasControllerNavigationRail

internal fun shouldShowEmptyCatalogAction(
    compact: Boolean,
    discoveryGamesEmpty: Boolean,
    hasSelectedDiscovery: Boolean,
    hasSelectedConfiguredResult: Boolean,
    configuredResultsEmpty: Boolean,
    syncing: Boolean,
): Boolean = !compact && discoveryGamesEmpty && !hasSelectedDiscovery &&
    !hasSelectedConfiguredResult && configuredResultsEmpty && !syncing

internal fun Modifier.claimInitialCatalogActionFocus(
    enabled: Boolean,
    focusRequester: FocusRequester?,
    focusClaimed: MutableState<Boolean>,
): Modifier {
    if (!enabled || focusRequester == null) return this

    return focusRequester(focusRequester).focusable().onGloballyPositioned {
        if (!focusClaimed.value) {
            focusClaimed.value = runCatching { focusRequester.requestFocus() }.getOrDefault(false)
        }
    }
}

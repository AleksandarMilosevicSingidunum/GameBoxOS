package com.gamebox.os.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch

/** Scrolls controller-focused controls into the visible viewport. */
internal fun Modifier.bringFocusedControlIntoView(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    bringIntoViewRequester(requester).onFocusChanged { state ->
        if (state.isFocused) scope.launch { requester.bringIntoView() }
    }
}

/** Remembers a screen's vertical position in the current saved UI state. */
@Composable
internal fun restoredScrollState(key: String): ScrollState {
    val uiState = LocalGameBoxUiState.current
    val initial = uiState?.screenValue("scroll.$key")?.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val scrollState = rememberScrollState(initial = initial)
    LaunchedEffect(uiState, key, scrollState) {
        snapshotFlow { scrollState.value }.collect { value ->
            uiState?.rememberScreenValue("scroll.$key", value.toString())
        }
    }
    return scrollState
}

package com.gamebox.os.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.GameSourceProviderType

@Composable
internal fun ConfiguredDiscoverySourceButton(
    source: GameSourceConfig,
    enabled: Boolean,
    onClick: () -> Unit,
    downFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    iconSize: Dp = 15.dp,
    labelFontSize: TextUnit = TextUnit.Unspecified,
    labelModifier: Modifier = Modifier.padding(start = 6.dp),
) {
    val jsonSource = source.type == GameSourceProviderType.GAMEBOX_JSON
    val vimmSource = source.type == GameSourceProviderType.VIMM_LAIR
    val activate = rememberUiClickAction { if (enabled) onClick() }
    OutlinedButton(
        enabled = enabled,
        onClick = activate,
        modifier = modifier
            .focusProperties {
                canFocus = enabled
                downFocusRequester?.let { down = it }
            }
            .blueprintButtonFocusRing(ButtonDefaults.shape)
            .onBlueprintActivation(activate)
            .semantics {
                contentDescription = when {
                    jsonSource -> "Sync ${source.name} configured metadata source"
                    vimmSource -> "Search ${source.name} configured discovery source"
                    else -> "Open ${source.name} configured web source"
                }
            },
        contentPadding = contentPadding,
    ) {
        Icon(
            imageVector = when {
                jsonSource -> Icons.Rounded.Sync
                vimmSource -> Icons.Rounded.Search
                else -> Icons.AutoMirrored.Rounded.OpenInNew
            },
            contentDescription = null,
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = (when {
                jsonSource -> "Sync "
                vimmSource -> "Search "
                else -> ""
            }) + source.name,
            modifier = labelModifier,
            fontSize = labelFontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

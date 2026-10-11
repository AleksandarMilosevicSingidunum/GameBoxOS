package com.gamebox.os.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One controller-aware link affordance shared by legal and configured web sources. */
@Composable
internal fun DiscoveryExternalLinkButton(label: String, onClick: () -> Unit) {
    val activate = rememberUiClickAction(onClick)
    OutlinedButton(
        onClick = activate,
        modifier = Modifier.blueprintButtonFocusRing(ButtonDefaults.shape)
            .onBlueprintActivation(activate),
    ) {
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(15.dp))
        Text(label, Modifier.padding(start = 7.dp), fontSize = 11.sp)
    }
}

internal fun openExternalDiscoveryLink(
    context: Context,
    uri: Uri,
    label: String,
    onFailure: (String) -> Unit,
) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        onFailure("No browser is available to open $label")
    }
}

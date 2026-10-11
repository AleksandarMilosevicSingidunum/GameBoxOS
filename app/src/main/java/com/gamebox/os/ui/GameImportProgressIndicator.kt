package com.gamebox.os.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.gamebox.os.importer.RomImportProgress
import kotlin.math.roundToInt

/** Accessible import progress shown while a local copy is copied, extracted, and verified. */
@Composable
internal fun GameImportProgressIndicator(
    modifier: Modifier = Modifier,
    progress: RomImportProgress? = null,
) {
    val fraction = progress?.fraction
    val modifierWithProgressSemantics = modifier.semantics {
        contentDescription = "Importing and verifying game copy"
        stateDescription = progress?.let { state ->
            val percent = fraction?.let { " · ${(it * 100).roundToInt()}%" }.orEmpty()
            state.description + percent
        } ?: "Import in progress"
        liveRegion = LiveRegionMode.Polite
    }
    if (fraction != null) {
        LinearProgressIndicator(progress = { fraction }, modifier = modifierWithProgressSemantics)
    } else {
        LinearProgressIndicator(modifier = modifierWithProgressSemantics)
    }
}

@Composable
internal fun GameImportProgressActions(
    modifier: Modifier = Modifier,
    progress: RomImportProgress?,
    onCancel: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameImportProgressIndicator(Modifier.weight(1f), progress)
        BlueprintTextButton(
            onClick = onCancel,
            modifier = Modifier.widthIn(min = 96.dp),
        ) {
            Text("Cancel")
        }
    }
}

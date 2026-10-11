package com.gamebox.os.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId

@Composable
internal fun DefaultHomeSetupBanner(
    onChooseHome: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "GameBox is not the default Home app"
        },
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("Finish console mode", fontWeight = FontWeight.SemiBold)
                Text(
                    "Choose GameBox as Android's default Home app so Home and normal launcher startup return here after unlock.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
            BlueprintButton(
                modifier = focusRequester?.let(Modifier::focusRequester) ?: Modifier,
                onClick = onChooseHome,
            ) {
                Text("Choose Home")
            }
        }
    }
}

@Composable
internal fun HomeScreen(
    games: List<Game>,
    restoreGameId: GameId?,
    onFocused: (GameId) -> Unit,
    compact: Boolean,
    hostWindowHasFocus: Boolean = true,
    openPc: () -> Unit,
    openStore: () -> Unit,
    openSearch: () -> Unit,
    openProfileSwitcher: () -> Unit,
    allowDefaultHomeSetupPrompt: Boolean = true,
    open: (Game) -> Unit,
    openStorageSettings: () -> Unit = {},
    openControllerSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val storageCapacity = rememberStorageCapacitySnapshot(context)
    var defaultHome by remember(context) { mutableStateOf(isGameBoxDefaultHome(context)) }
    val emptyHomeSetupFocusRequester = remember { FocusRequester() }
    val defaultHomeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        defaultHome = isGameBoxDefaultHome(context)
    }
    val controllerActions = LocalControllerActions.current
    DisposableEffect(controllerActions) {
        val lease = controllerActions?.configure(
            xLabel = "Search",
            onX = openSearch,
            yLabel = "Switch Profile",
            onY = openProfileSwitcher,
        )
        onDispose {
            val owner = controllerActions
            if (owner != null && lease != null) owner.clear(lease)
        }
    }
    LaunchedEffect(defaultHome, games.size, allowDefaultHomeSetupPrompt) {
        if (games.isEmpty()) {
            androidx.compose.runtime.withFrameNanos { }
            runCatching { emptyHomeSetupFocusRequester.requestFocus() }
        }
    }
    HomeDashboardContent(
        games = games,
        presentation = homePresentationState(games, restoreGameId, compact),
        compact = compact,
        hostWindowHasFocus = hostWindowHasFocus,
        showDefaultHomeSetupBanner = shouldShowDefaultHomeSetupBanner(defaultHome, allowDefaultHomeSetupPrompt),
        networkLabel = LocalRuntimeDeviceStatus.current.networkLabel,
        storagePercent = storageCapacity?.usedFraction?.times(100f)?.toInt(),
        deviceModel = Build.MODEL,
        controllerLabel = connectedControllerLabel(),
        onChooseHome = { defaultHomeLauncher.launch(defaultHomeSelectionIntent(context)) },
        emptyHomeSetupFocusRequester = emptyHomeSetupFocusRequester,
        openPc = openPc,
        openStore = openStore,
        openStorageSettings = openStorageSettings,
        openControllerSettings = openControllerSettings,
        onFocused = onFocused,
        open = open,
    )
}

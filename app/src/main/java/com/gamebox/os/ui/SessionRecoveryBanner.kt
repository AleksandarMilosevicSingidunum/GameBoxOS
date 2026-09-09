package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gamebox.os.launch.GameLaunchController
import com.gamebox.os.launch.LaunchUiState

@Composable
internal fun SessionRecoveryBanner(controller: GameLaunchController) {
    val state by controller.observeState().collectAsState()
    if (state.status == LaunchUiState.Status.SESSION_ERROR) {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer) {
            Column(Modifier.padding(12.dp)) {
                Text(state.message ?: "Session history needs recovery",
                    color = MaterialTheme.colorScheme.onErrorContainer)
                Button(onClick = controller::onHostResumed) { Text("Retry session recovery") }
            }
        }
    }
}

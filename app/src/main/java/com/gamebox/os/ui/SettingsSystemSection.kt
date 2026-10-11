package com.gamebox.os.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SettingsSystemSection(
    modifier: Modifier = Modifier,
    onOpenHomeSettings: () -> Unit,
    onOpenStorageSettings: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    SettingsSectionHeader("System", modifier)
    Text("Console startup", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    Text(
        "On a dedicated GameBox phone, choose GameBox as Android's default Home app so it becomes the normal surface after boot and unlock. Android still owns the lock screen and first unlock after restart.",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 6.dp),
    )
    SettingsActionRow("Choose default Home app", Icons.Rounded.Home, onClick = onOpenHomeSettings)
    SettingsActionRow("App storage", Icons.Rounded.Storage, onClick = onOpenStorageSettings)
    SettingsActionRow("GameBox app details", Icons.Rounded.Info, onClick = onOpenAppDetails)
    SettingsActionRow("Android system settings", Icons.Rounded.Settings, onClick = onOpenSystemSettings)
}

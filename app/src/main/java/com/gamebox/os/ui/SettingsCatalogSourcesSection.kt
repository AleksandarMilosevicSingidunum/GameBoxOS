package com.gamebox.os.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.catalog.CatalogProvider
import com.gamebox.os.settings.GameBoxSettings
import com.gamebox.os.settings.SettingsRepository

@Composable
internal fun SettingsCatalogSourcesSection(
    modifier: Modifier = Modifier,
    currentSettings: GameBoxSettings,
    settingsRepository: SettingsRepository,
    catalogProvider: CatalogProvider,
    messageState: MutableState<String?>,
) {
    SettingsSectionHeader("Sources", modifier)
    SettingsAuthorizedCatalogProviderSection(
        currentSettings = currentSettings,
        settingsRepository = settingsRepository,
        catalogProvider = catalogProvider,
        messageState = messageState,
    )
    SettingsGameDiscoverySourcesSection(currentSettings, settingsRepository)
    Spacer(Modifier.height(12.dp))
    BlueprintPanel(Modifier.fillMaxWidth()) {
        Text("Automatic metadata and artwork", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "GameBox uses exact-title Wikidata matches for release details and the Libretro thumbnail archive for console box art and screenshots when available. No API key is required; coverage varies by game and system.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
        Text(
            "Correct a match or set custom artwork from a game's Details screen.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "Runtime providers and emulator profiles remain intentionally scoped to their dedicated screens.",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
    )
}

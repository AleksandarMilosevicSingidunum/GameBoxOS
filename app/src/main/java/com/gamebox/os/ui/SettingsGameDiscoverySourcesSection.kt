package com.gamebox.os.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.settings.GameBoxSettings
import com.gamebox.os.settings.SettingsRepository
import com.gamebox.os.source.*
import kotlinx.coroutines.launch

@Composable
internal fun SettingsGameDiscoverySourcesSection(
    currentSettings: GameBoxSettings,
    settingsRepository: SettingsRepository,
) {
    val scope = rememberCoroutineScope()
    var sourceType by remember { mutableStateOf(GameSourceProviderType.EXTERNAL_WEB) }
    var sourceName by remember { mutableStateOf("") }
    var sourceBaseUrl by remember { mutableStateOf("") }
    var sourceSearchTemplate by remember { mutableStateOf("") }
    var sourcePlatforms by remember { mutableStateOf("") }
    var sourceMessage by remember { mutableStateOf<String?>(null) }
    var sourceTestingId by remember { mutableStateOf<String?>(null) }
    var sourceMutationBusy by remember { mutableStateOf(false) }
    var sourceMessageIsError by remember { mutableStateOf(false) }

    Spacer(Modifier.height(18.dp))
    Text("Game discovery sources", fontWeight = FontWeight.Bold)
    Text(
        "Add HTTPS website searches or GameBox JSON metadata feeds. Discovery sources never become installable binaries unless a trusted catalog separately provides a verified source and checksum.",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        fontSize = 12.sp,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BlueprintFilterChip(
            selected = sourceType == GameSourceProviderType.EXTERNAL_WEB,
            onClick = { sourceType = GameSourceProviderType.EXTERNAL_WEB },
            label = { Text("External web") },
        )
        BlueprintFilterChip(
            selected = sourceType == GameSourceProviderType.GAMEBOX_JSON,
            onClick = { sourceType = GameSourceProviderType.GAMEBOX_JSON },
            label = { Text("GameBox JSON") },
        )
        BlueprintFilterChip(
            selected = sourceType == GameSourceProviderType.VIMM_LAIR,
            onClick = {
                sourceType = GameSourceProviderType.VIMM_LAIR
                sourceName = "Vimm's Lair"
                sourceBaseUrl = "https://vimm.net/vault"
                sourceSearchTemplate = ""
                sourcePlatforms = vimmLairDefaultPresetPlatforms.joinToString(", ")
            },
            label = { Text("Vimm's Lair") },
        )
    }
    currentSettings.gameSources.forEach { source ->
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        ) {
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(source.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        when (source.type) {
                            GameSourceProviderType.GAMEBOX_JSON -> "GameBox JSON metadata"
                            GameSourceProviderType.VIMM_LAIR -> "Vimm's Lair discovery"
                            else -> "External web search"
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                    )
                    Text(
                        source.baseUrl,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (source.type == GameSourceProviderType.VIMM_LAIR) {
                    if (source.platforms != vimmLairDefaultPresetPlatforms.toSet()) {
                        BlueprintTextButton(
                            enabled = !sourceMutationBusy && sourceTestingId == null,
                            onClick = {
                                scope.launch {
                                    runSettingsUiAction(
                                        failureMessage = "${source.name} preset could not be applied",
                                        onBusyChanged = { sourceMutationBusy = it },
                                        onFailure = {
                                            sourceMessage = it
                                            sourceMessageIsError = true
                                        },
                                    ) {
                                        settingsRepository.upsertGameSource(
                                            source.copy(platforms = vimmLairDefaultPresetPlatforms.toSet())
                                        )
                                        sourceMessage = "Applied console preset to ${source.name}"
                                        sourceMessageIsError = false
                                    }
                                }
                            },
                        ) { Text("Apply preset") }
                    }
                    BlueprintTextButton(
                        enabled = source.enabled && sourceTestingId == null && !sourceMutationBusy,
                        onClick = {
                            scope.launch {
                                runSettingsUiAction(
                                    failureMessage = "${source.name} test failed",
                                    onBusyChanged = { busy -> sourceTestingId = if (busy) source.id else null },
                                    onFailure = {
                                        sourceMessage = "${source.name} test failed: ${it.take(180)}"
                                        sourceMessageIsError = true
                                    },
                                ) {
                                    sourceMessage = "Testing ${source.name}…"
                                    sourceMessageIsError = false
                                    val probe = VimmLairDiscoverySource(source).probe()
                                    sourceMessage = "${source.name} reachable · ${probe.platform} · ${probe.vaultLinks} vault links detected"
                                }
                            }
                        },
                    ) {
                        Text(if (sourceTestingId == source.id) "Testing…" else "Test")
                    }
                }
                BlueprintSwitch(
                    checked = source.enabled,
                    enabled = !sourceMutationBusy && sourceTestingId == null,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            runSettingsUiAction(
                                failureMessage = "${source.name} could not be updated",
                                onBusyChanged = { sourceMutationBusy = it },
                                onFailure = {
                                    sourceMessage = it
                                    sourceMessageIsError = true
                                },
                            ) {
                                settingsRepository.upsertGameSource(source.copy(enabled = enabled))
                                sourceMessage = "Updated ${source.name}"
                                sourceMessageIsError = false
                            }
                        }
                    },
                )
                BlueprintTextButton(
                    enabled = !sourceMutationBusy && sourceTestingId == null,
                    onClick = {
                        scope.launch {
                            runSettingsUiAction(
                                failureMessage = "${source.name} could not be removed",
                                onBusyChanged = { sourceMutationBusy = it },
                                onFailure = {
                                    sourceMessage = it
                                    sourceMessageIsError = true
                                },
                            ) {
                                settingsRepository.removeGameSource(source.id)
                                sourceMessage = "Removed ${source.name}"
                                sourceMessageIsError = false
                            }
                        }
                    },
                ) { Text("Remove") }
            }
        }
    }
    OutlinedTextField(
        value = sourceName,
        onValueChange = { sourceName = it },
        enabled = !sourceMutationBusy,
        label = { Text("Source name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    OutlinedTextField(
        value = sourceBaseUrl,
        onValueChange = { sourceBaseUrl = it },
        enabled = !sourceMutationBusy,
        label = {
            Text(
                when (sourceType) {
                    GameSourceProviderType.GAMEBOX_JSON -> "HTTPS JSON manifest URL"
                    GameSourceProviderType.VIMM_LAIR -> "Vimm vault base URL"
                    else -> "HTTPS base URL"
                },
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    )
    when (sourceType) {
        GameSourceProviderType.EXTERNAL_WEB -> OutlinedTextField(
            value = sourceSearchTemplate,
            onValueChange = { sourceSearchTemplate = it },
            enabled = !sourceMutationBusy,
            label = { Text("Optional search URL template") },
            supportingText = { Text("Supported placeholders: {query}, {title}, {platform}") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        GameSourceProviderType.GAMEBOX_JSON -> Text(
            "JSON schema 1 imports metadata only. Required fields per game: id, title and platform.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        GameSourceProviderType.VIMM_LAIR -> {
            Text(
                "Searches Vimm vault listings, hydrates selected title metadata inside GameBox, and keeps the exact vault page available as an external link. GameBox does not extract media IDs or download game binaries from this source.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            BlueprintOutlinedButton(
                enabled = !sourceMutationBusy,
                onClick = {
                    sourceName = "Vimm's Lair"
                    sourceBaseUrl = "https://vimm.net/vault"
                    sourceSearchTemplate = ""
                    sourcePlatforms = vimmLairDefaultPresetPlatforms.joinToString(", ")
                },
                modifier = Modifier.padding(top = 6.dp),
            ) { Text("Use Vimm's Lair preset") }
        }
        else -> Unit
    }
    OutlinedTextField(
        value = sourcePlatforms,
        onValueChange = { sourcePlatforms = it },
        enabled = !sourceMutationBusy,
        label = { Text("Optional consoles") },
        supportingText = {
            Text(
                if (sourceType == GameSourceProviderType.VIMM_LAIR)
                    "3DS and Switch are saved in the preset but skipped in Vimm search: no verified vault mapping."
                else "Comma-separated, for example: PS2, PSP, GameCube"
            )
        },
        minLines = 2,
        maxLines = 3,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    )
    BlueprintButton(
        enabled = !sourceMutationBusy && sourceTestingId == null,
        onClick = {
            val existingIds = currentSettings.gameSources.map { it.id }.toSet()
            val newSource = runCatching {
                val trimmedName = sourceName.trim()
                GameSourceConfig(
                    id = nextGameSourceId(trimmedName, existingIds),
                    name = trimmedName,
                    type = sourceType,
                    baseUrl = sourceBaseUrl.trim(),
                    platforms = sourcePlatforms.split(',').map(String::trim).filter(String::isNotEmpty).toSet(),
                    searchUrlTemplate = sourceSearchTemplate.trim().takeIf {
                        sourceType == GameSourceProviderType.EXTERNAL_WEB && it.isNotEmpty()
                    },
                )
            }
            if (newSource.isFailure) {
                sourceMessage = newSource.exceptionOrNull()?.message ?: "Invalid game source"
                sourceMessageIsError = true
            } else {
                val source = newSource.getOrThrow()
                scope.launch {
                    runSettingsUiAction(
                        failureMessage = "${source.name} could not be added",
                        onBusyChanged = { sourceMutationBusy = it },
                        onFailure = {
                            sourceMessage = it
                            sourceMessageIsError = true
                        },
                    ) {
                        settingsRepository.upsertGameSource(source)
                        sourceName = ""
                        sourceBaseUrl = ""
                        sourceSearchTemplate = ""
                        sourcePlatforms = ""
                        sourceMessage = "Added ${source.name}"
                        sourceMessageIsError = false
                    }
                }
            }
        },
        modifier = Modifier.padding(top = 8.dp),
    ) { Text("Add source") }
    sourceMessage?.let { message ->
        Text(
            message,
            color = if (sourceMessageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

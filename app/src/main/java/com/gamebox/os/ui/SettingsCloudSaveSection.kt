package com.gamebox.os.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.save.CloudSaveEndpointPolicy
import com.gamebox.os.save.CloudSaveProvider
import com.gamebox.os.settings.GameBoxSettings
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.launch

@Composable
internal fun SettingsCloudSaveSection(
    modifier: Modifier = Modifier,
    currentSettings: GameBoxSettings,
    settingsRepository: SettingsRepository,
) {
    val scope = rememberCoroutineScope()
    var cloudProvider by remember(currentSettings.cloudSaveProvider) {
        mutableStateOf(currentSettings.cloudSaveProvider.uppercase())
    }
    var cloudEndpoint by remember(currentSettings.cloudSaveEndpoint) {
        mutableStateOf(currentSettings.cloudSaveEndpoint)
    }
    var cloudRegion by remember(currentSettings.cloudSaveRegion) {
        mutableStateOf(currentSettings.cloudSaveRegion)
    }
    var cloudIdentity by remember { mutableStateOf("") }
    var cloudSecret by remember { mutableStateOf("") }
    var cloudCredentialsConfigured by remember { mutableStateOf(false) }
    var cloudMessage by remember { mutableStateOf<String?>(null) }
    var cloudMessageIsError by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(settingsRepository, cloudProvider) {
        cloudCredentialsConfigured = settingsRepository.hasCloudSaveCredentials(cloudProvider)
    }
        SettingsSectionHeader("Saves & Cloud Sync", modifier)
        Text("Authenticated cloud backup", fontWeight = FontWeight.Bold)
        Text(
            "Optional. GameBox encrypts credentials with Android Keystore and transfers only checksum-protected save envelopes over HTTPS.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontSize = 12.sp,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            listOf("WEBDAV" to "WebDAV", "S3" to "S3-compatible").forEach { (value, label) ->
                if (cloudProvider == value) {
                    BlueprintButton(enabled = !saving, onClick = { cloudMessage = null }) { Text(label) }
                } else {
                    BlueprintOutlinedButton(
                        enabled = !saving,
                        onClick = {
                            cloudProvider = value
                            cloudIdentity = ""
                            cloudSecret = ""
                            cloudMessage = null
                        },
                    ) { Text(label) }
                }
            }
        }
        OutlinedTextField(
            value = cloudEndpoint,
            onValueChange = { cloudEndpoint = it },
            label = { Text(if (cloudProvider == "S3") "HTTPS bucket/prefix endpoint" else "HTTPS WebDAV collection") },
            supportingText = { Text("GameBox appends a game-scoped .gamebox-save object name") },
            singleLine = true,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
        )
        if (cloudProvider == "S3") {
            OutlinedTextField(
                value = cloudRegion,
                onValueChange = { cloudRegion = it },
                label = { Text("S3 signing region") },
                singleLine = true,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
        OutlinedTextField(
            value = cloudIdentity,
            onValueChange = { cloudIdentity = it },
            label = { Text(if (cloudProvider == "S3") "Access key" else "Username") },
            singleLine = true,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        OutlinedTextField(
            value = cloudSecret,
            onValueChange = { cloudSecret = it },
            label = { Text(if (cloudProvider == "S3") "Secret key" else "Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).semantics {
                contentDescription = "Cloud save secret, hidden"
            },
        )
        Text(
            if (cloudCredentialsConfigured) "Credentials configured securely; leave both fields blank to keep them."
            else "Credentials are not configured for this provider.",
            color = if (cloudCredentialsConfigured) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 5.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            BlueprintButton(enabled = !saving, onClick = {
                val provider = runCatching { CloudSaveProvider.valueOf(cloudProvider) }.getOrNull()
                val validation = runCatching {
                    requireNotNull(provider) { "Choose a supported cloud provider" }
                    CloudSaveEndpointPolicy.objectUri(cloudEndpoint, "galaxy-patrol")
                    CloudSaveEndpointPolicy.requireRegion(provider, cloudRegion)
                    val hasNewIdentity = cloudIdentity.isNotBlank()
                    val hasNewSecret = cloudSecret.isNotBlank()
                    require(hasNewIdentity == hasNewSecret) { "Enter both credential fields or leave both blank" }
                }
                if (validation.isFailure) {
                    cloudMessage = validation.exceptionOrNull()?.message ?: "Cloud configuration is invalid"
                    cloudMessageIsError = true
                } else {
                    scope.launch {
                        runSettingsUiAction(
                            failureMessage = "Cloud save configuration could not be saved",
                            onBusyChanged = { saving = it },
                            onFailure = {
                                cloudMessage = it
                                cloudMessageIsError = true
                            },
                        ) {
                            val hasNewCredentials = cloudIdentity.isNotBlank()
                            val hasStoredCredentials = settingsRepository.hasCloudSaveCredentials(cloudProvider)
                            if (!hasNewCredentials && !hasStoredCredentials) {
                                cloudMessage = "Cloud credentials are required"
                                cloudMessageIsError = true
                            } else {
                                settingsRepository.setCloudSaveConfiguration(cloudProvider, cloudEndpoint, cloudRegion)
                                if (hasNewCredentials) {
                                    settingsRepository.setCloudSaveCredentials(cloudProvider, cloudIdentity, cloudSecret)
                                    cloudIdentity = ""
                                    cloudSecret = ""
                                    cloudCredentialsConfigured = true
                                }
                                cloudMessage = "Cloud save configuration stored securely"
                                cloudMessageIsError = false
                            }
                        }
                    }
                }
            }) { Text(if (saving) "Saving…" else "Save cloud settings") }
            BlueprintOutlinedButton(
                enabled = cloudCredentialsConfigured && !saving,
                onClick = {
                    scope.launch {
                        runSettingsUiAction(
                            failureMessage = "Cloud save credentials could not be removed",
                            onBusyChanged = { saving = it },
                            onFailure = {
                                cloudMessage = it
                                cloudMessageIsError = true
                            },
                        ) {
                            settingsRepository.clearCloudSaveCredentials()
                            cloudIdentity = ""
                            cloudSecret = ""
                            cloudCredentialsConfigured = false
                            cloudMessage = "Cloud save credentials removed"
                            cloudMessageIsError = false
                        }
                    }
                },
            ) { Text("Clear credentials") }
        }
        if (saving) LinearProgressIndicator(Modifier.fillMaxWidth())
        cloudMessage?.let {
            Text(
                it,
                color = if (cloudMessageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
}

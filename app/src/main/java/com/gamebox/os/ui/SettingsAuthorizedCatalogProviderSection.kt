package com.gamebox.os.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.catalog.*
import com.gamebox.os.settings.GameBoxSettings
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.launch

@Composable
internal fun SettingsAuthorizedCatalogProviderSection(
    currentSettings: GameBoxSettings,
    settingsRepository: SettingsRepository,
    catalogProvider: CatalogProvider,
    messageState: MutableState<String?>,
) {
    val scope = rememberCoroutineScope()
    var message by messageState
    var catalogUrl by remember(currentSettings.catalogUrl) { mutableStateOf(currentSettings.catalogUrl) }
    var testing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var transport by remember(currentSettings.catalogTransport) { mutableStateOf(currentSettings.catalogTransport) }
    var bucket by remember(currentSettings.catalogBucket) { mutableStateOf(currentSettings.catalogBucket) }
    var prefix by remember(currentSettings.catalogPrefix) { mutableStateOf(currentSettings.catalogPrefix) }
    var region by remember(currentSettings.catalogRegion) { mutableStateOf(currentSettings.catalogRegion) }
    var identity by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var credentialsConfigured by remember { mutableStateOf(false) }

    LaunchedEffect(settingsRepository, transport) {
        credentialsConfigured = settingsRepository.hasCatalogCredentials(transport)
    }

    Text("Authorized catalog provider", fontWeight = FontWeight.Bold)
    Text(
        "Choose HTTPS, WebDAV, or S3-compatible storage. Credentials are encrypted with Android Keystore.",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        listOf("HTTPS", "WEBDAV", "S3").forEach { option ->
            BlueprintFilterChip(
                selected = transport == option,
                onClick = {
                    transport = option
                    message = null
                },
                label = { Text(if (option == "WEBDAV") "WebDAV" else option) },
            )
        }
    }
    OutlinedTextField(
        value = catalogUrl,
        onValueChange = { catalogUrl = it },
        label = {
            Text(
                when (transport) {
                    "WEBDAV" -> "WebDAV collection URL"
                    "S3" -> "S3-compatible endpoint"
                    else -> "HTTPS catalog URL"
                },
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (transport == "S3") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = bucket,
                onValueChange = { bucket = it },
                label = { Text("Bucket") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = region,
                onValueChange = { region = it },
                label = { Text("Region") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = prefix,
            onValueChange = { prefix = it },
            label = { Text("Optional prefix") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Text(
        if (credentialsConfigured) "Credentials configured securely" else
            if (transport == "HTTPS") "Optional Basic authentication" else "Credentials required for authenticated catalogs",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        fontSize = 12.sp,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = identity,
            onValueChange = { identity = it },
            label = { Text(if (transport == "S3") "Access key" else "Username") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = secret,
            onValueChange = { secret = it },
            label = { Text(if (transport == "S3") "Secret key" else "Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.weight(1f).semantics {
                contentDescription = "Catalog secret, hidden"
            },
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        BlueprintButton(
            enabled = !saving && !testing,
            onClick = {
                val trimmed = catalogUrl.trim()
                val validationError = runCatching {
                    if (trimmed.isNotEmpty()) {
                        val selectedTransport = when (transport) {
                            "WEBDAV" -> CatalogTransport.WebDav(trimmed)
                            "S3" -> CatalogTransport.S3(
                                trimmed,
                                bucket.trim(),
                                prefix.trim(),
                                region.trim().ifEmpty { "us-east-1" },
                            )
                            else -> CatalogTransport.Https(trimmed)
                        }
                        CatalogProviderConfig(selectedTransport)
                    }
                    require(
                        (identity.isBlank() && secret.isBlank()) ||
                            (identity.isNotBlank() && secret.isNotBlank()),
                    ) { "Enter both credential fields or leave both blank" }
                }.exceptionOrNull()
                if (validationError != null) {
                    message = validationError.message ?: "Invalid catalog configuration"
                } else {
                    scope.launch {
                        runSettingsUiAction(
                            failureMessage = "Catalog configuration could not be saved",
                            onBusyChanged = { saving = it },
                            onFailure = { message = it },
                        ) {
                            settingsRepository.setCatalogConfiguration(transport, trimmed, bucket, prefix, region)
                            if (identity.isNotBlank()) {
                                settingsRepository.setCatalogCredentials(transport, identity, secret)
                                identity = ""
                                secret = ""
                                credentialsConfigured = true
                            }
                            message = if (trimmed.isEmpty())
                                "Bundled offline catalog selected" else "Catalog configuration saved"
                        }
                    }
                }
            },
        ) { Text(if (saving) "Saving…" else "Save source") }
        BlueprintOutlinedButton(
            enabled = !saving && !testing,
            onClick = {
                scope.launch {
                    runSettingsUiAction(
                        failureMessage = "Catalog connection test failed",
                        onBusyChanged = { testing = it },
                        onFailure = { message = it },
                    ) {
                        settingsRepository.setCatalogConfiguration(transport, catalogUrl, bucket, prefix, region)
                        if (identity.isNotBlank() && secret.isNotBlank()) {
                            settingsRepository.setCatalogCredentials(transport, identity, secret)
                            identity = ""
                            secret = ""
                            credentialsConfigured = true
                        }
                        message = catalogProvider.testConnection().message
                    }
                }
            },
        ) { Text(if (testing) "Testing…" else "Save & test") }
        if (credentialsConfigured) {
            BlueprintOutlinedButton(
                enabled = !saving && !testing,
                onClick = {
                    scope.launch {
                        runSettingsUiAction(
                            failureMessage = "Catalog credentials could not be cleared",
                            onBusyChanged = { saving = it },
                            onFailure = { message = it },
                        ) {
                            settingsRepository.clearCatalogCredentials()
                            credentialsConfigured = false
                            identity = ""
                            secret = ""
                            message = "Catalog credentials cleared"
                        }
                    }
                },
            ) { Text("Clear credentials") }
        }
    }
    Text(
        "Capabilities: refresh, connection test, authorized source resolution",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        fontSize = 12.sp,
    )
    message?.let {
        Text(
            it,
            color = if (it.startsWith("Connected") || it.contains("available") || it.contains("saved"))
                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

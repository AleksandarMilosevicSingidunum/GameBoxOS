@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gamebox.os.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.relocation.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.gamebox.os.catalog.*
import com.gamebox.os.content.*
import com.gamebox.os.data.*
import com.gamebox.os.diagnostics.*
import com.gamebox.os.display.*
import com.gamebox.os.domain.*
import com.gamebox.os.download.*
import com.gamebox.os.importer.*
import com.gamebox.os.launch.*
import com.gamebox.os.navigation.*
import com.gamebox.os.save.*
import com.gamebox.os.settings.*
import com.gamebox.os.source.*
import com.gamebox.os.storage.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
internal fun SettingsScreen(
    compact: Boolean,
    settingsRepository: SettingsRepository,
    gameRepository: GameRepository,
    downloadRepository: DownloadRepository,
    catalogProvider: CatalogProvider,
    requestedSection: SettingsSection? = null,
    onSectionRequestConsumed: (SettingsSection) -> Unit = {},
) {
    val context = LocalContext.current
    val storageCapacity = rememberStorageCapacitySnapshot(context)
    val controllerActions = LocalControllerActions.current
    val currentSettings by settingsRepository.settings.collectAsState(initial = com.gamebox.os.settings.GameBoxSettings())
    val diagnosticGames by gameRepository.observeGames().collectAsState()
    val diagnosticDownloads by downloadRepository.observeJobs().collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = restoredScrollState("settings")
    val sectionOffsets = remember { mutableStateMapOf<SettingsSection, Int>() }
    var selectedSection by remember { mutableStateOf(requestedSection ?: SettingsSection.STORAGE) }
    var focusSection by remember { mutableStateOf(requestedSection ?: SettingsSection.STORAGE) }
    val firstWideSettingsSection = remember { FocusRequester() }
    val settingsMessage = remember { mutableStateOf<String?>(null) }
    val storageManagement = rememberSettingsStorageManagement(
        context = context,
        settingsRepository = settingsRepository,
        externalLibraryUri = currentSettings.externalLibraryUri,
        emulatorSaveRootUri = currentSettings.emulatorSaveRootUri,
        onMessage = { settingsMessage.value = it },
    )
    val externalStorageController = storageManagement.externalController
    val emulatorSaveStorageController = storageManagement.emulatorSaveController
    val externalStorageStatus = storageManagement.externalStatus
    val emulatorSaveStorageStatus = storageManagement.emulatorSaveStatus
    val migrationState = rememberSettingsMigrationState(context, diagnosticGames)
    val migrationPlan = migrationState.plan
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        settingsMessage.value = if (granted) "Download notifications enabled"
        else "Notification permission was not granted"
    }
    val runtimeStatus = LocalRuntimeDeviceStatus.current
    val totalStorage = storageCapacity?.totalBytes ?: 0L
    val usableStorage = storageCapacity?.availableBytes ?: 0L
    val connectedControllers = runtimeStatus.controllerNames.size
    val activeDownloads = diagnosticDownloads.count {
        it.status !in setOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)
    }

    fun launchSystemSettings(action: String) {
        try {
            val intent = if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                Intent(action, "package:${context.packageName}".toUri())
            } else Intent(action)
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun sectionAnchor(section: SettingsSection): Modifier = Modifier.onGloballyPositioned { coordinates ->
        // The section header is a child of the scrollable Column. Its parent-relative
        // position is already its stable content offset; adding the current scroll
        // position makes rail targets drift every time the user scrolls.
        sectionOffsets[section] = coordinates.positionInParent().y.roundToInt()
    }

    LaunchedEffect(scrollState, sectionOffsets) {
        snapshotFlow {
            SettingsNavigationPolicy.selectedSection(scrollState.value, sectionOffsets)
        }.collect { section ->
            selectedSection = section
        }
    }
    LaunchedEffect(requestedSection) {
        val section = requestedSection ?: return@LaunchedEffect
        focusSection = section
        val target = snapshotFlow { sectionOffsets[section] }.filterNotNull().first()
        selectedSection = section
        if (currentSettings.reducedMotion) scrollState.scrollTo(target)
        else scrollState.animateScrollTo(target)
        onSectionRequestConsumed(section)
    }
    LaunchedEffect(compact, focusSection) {
        if (!compact) {
            androidx.compose.runtime.withFrameNanos { }
            firstWideSettingsSection.requestFocus()
        }
    }
    if (migrationState.showConfirmation && externalStorageStatus != null && migrationPlan != null) {
        MigrationConfirmationDialog(
            plan = migrationPlan,
            storageStatus = externalStorageStatus,
            previousResult = migrationState.previousResult,
            onConfirm = {
                scope.launch {
                    val result = migrationState.confirm(currentSettings.externalLibraryUri.toUri())
                    settingsMessage.value = "Migration copied " + result.copiedCount + " item(s), " + result.retryableCount + " retryable, " + result.failedCount + " failed"
                }
            },
            onRetry = storageManagement.chooseExternalLibrary,
            onDismiss = migrationState::dismissConfirmation,
        )
    }
    val settingsContent: @Composable ColumnScope.() -> Unit = {
        Text("Settings", fontSize = if (compact) 28.sp else 17.sp, fontWeight = FontWeight.Bold)
        Text(
            "GameBox configuration and safe Android system shortcuts",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            fontSize = if (compact) 14.sp else 11.sp,
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("App storage", fontWeight = FontWeight.Bold)
                    Text(
                        formatBytes(usableStorage) + " free of " + formatBytes(totalStorage),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    LinearProgressIndicator(
                        progress = { if (totalStorage > 0L) 1f - usableStorage.toFloat() / totalStorage.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        SettingsStorageSection(
            compact = compact,
            currentSettings = currentSettings,
            externalStorageStatus = externalStorageStatus,
            emulatorSaveStorageStatus = emulatorSaveStorageStatus,
            migrationPlan = migrationPlan,
            migrationScanFailed = migrationState.scanFailed,
            modifier = sectionAnchor(SettingsSection.STORAGE),
            onChooseExternalLibrary = storageManagement.chooseExternalLibrary,
            onForgetExternalLibrary = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        externalStorageController.forgetTree(currentSettings.externalLibraryUri)
                    }
                    settingsMessage.value = "External library permission removed"
                }
            },
            onMigrateInstalledContent = {
                if (externalStorageStatus != null) migrationState.requestConfirmation()
            },
            onRetryMigrationScan = { scope.launch { migrationState.refreshPlan() } },
            onChooseEmulatorSaveRoot = storageManagement.chooseEmulatorSaveRoot,
            onForgetEmulatorSaveRoot = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        emulatorSaveStorageController.forgetTree(currentSettings.emulatorSaveRootUri)
                    }
                    settingsMessage.value = "Emulator save permission removed"
                }
            },
        )
        SettingsDeviceAndDownloadSections(
            connectedControllers = connectedControllers,
            runtimeStatus = runtimeStatus,
            activeDownloads = activeDownloads,
            currentSettings = currentSettings,
            onUnmeteredDownloadsChange = { enabled ->
                scope.launch { settingsRepository.setDownloadsUnmeteredOnly(enabled) }
            },
            onEnableNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onOpenBluetoothSettings = { launchSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS) },
            onOpenEmulatorSettings = { launchSystemSettings(Settings.ACTION_APPLICATION_SETTINGS) },
            modifier = sectionAnchor(SettingsSection.CONTROLLERS),
            downloadsModifier = sectionAnchor(SettingsSection.DOWNLOADS),
            emulatorsModifier = sectionAnchor(SettingsSection.EMULATORS),
        )
        SettingsDisplaySection(
            compact = compact,
            currentSettings = currentSettings,
            runtimeStatus = runtimeStatus,
            modifier = sectionAnchor(SettingsSection.DISPLAY),
            onSafeAreaChange = { percent -> scope.launch { settingsRepository.setSafeAreaPercent(percent) } },
            onOpenAndroidDisplaySettings = { launchSystemSettings(Settings.ACTION_DISPLAY_SETTINGS) },
        )
        SettingsAudioSection(
            outputLabel = runtimeStatus.audioOutputLabel,
            modifier = sectionAnchor(SettingsSection.AUDIO),
            onOpenAndroidSoundSettings = { launchSystemSettings(Settings.ACTION_SOUND_SETTINGS) },
        )
        Spacer(Modifier.height(18.dp))
        SettingsNetworkSection(
            currentSettings = currentSettings,
            settingsRepository = settingsRepository,
            runtimeStatus = runtimeStatus,
            modifier = sectionAnchor(SettingsSection.NETWORK),
            onOpenAndroidNetworkSettings = { launchSystemSettings(Settings.ACTION_WIRELESS_SETTINGS) },
        )
        SettingsCloudSaveSection(
            currentSettings = currentSettings,
            settingsRepository = settingsRepository,
            modifier = sectionAnchor(SettingsSection.SAVES_CLOUD),
        )
        SettingsSystemSection(
            modifier = sectionAnchor(SettingsSection.SYSTEM),
            onOpenHomeSettings = { launchSystemSettings(Settings.ACTION_HOME_SETTINGS) },
            onOpenStorageSettings = { launchSystemSettings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS) },
            onOpenAppDetails = { launchSystemSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) },
            onOpenSystemSettings = { launchSystemSettings(Settings.ACTION_SETTINGS) },
        )
        SettingsInterfaceSection(currentSettings, settingsRepository)
        Spacer(Modifier.height(18.dp))
        SettingsCatalogSourcesSection(
            modifier = sectionAnchor(SettingsSection.SOURCES),
            currentSettings = currentSettings,
            settingsRepository = settingsRepository,
            catalogProvider = catalogProvider,
            messageState = settingsMessage,
        )
        Spacer(Modifier.height(18.dp))
        SettingsDeveloperDiagnosticsSection(
            currentSettings = currentSettings,
            settingsRepository = settingsRepository,
            games = diagnosticGames,
            downloads = diagnosticDownloads,
            runtimeStatus = runtimeStatus,
            storageCapacity = storageCapacity,
            modifier = sectionAnchor(SettingsSection.DEVELOPER),
            messageState = settingsMessage,
        )
    }
    val navigateToSection: (SettingsSection) -> Unit = { section ->
        selectedSection = section
        scope.launch {
            val target = sectionOffsets[section] ?: 0
            if (currentSettings.reducedMotion) scrollState.scrollTo(target)
            else scrollState.animateScrollTo(target)
        }
    }
    DisposableEffect(controllerActions, selectedSection, sectionOffsets.size) {
        val lease = controllerActions?.configure(
            xLabel = "Next section",
            onX = { navigateToSection(adjacentSettingsSection(selectedSection, 1)) },
            yLabel = "Previous section",
            onY = { navigateToSection(adjacentSettingsSection(selectedSection, -1)) },
        )
        onDispose {
            val owner = controllerActions
            if (owner != null && lease != null) owner.clear(lease)
        }
    }
    if (compact) {
        Column(Modifier.fillMaxSize()) {
            SettingsSectionStrip(selectedSection, navigateToSection)
            Column(Modifier.weight(1f).verticalScroll(scrollState), content = settingsContent)
        }
    } else {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsWideSectionRail(
                selected = selectedSection,
                storageFocusRequester = firstWideSettingsSection,
                onSelect = navigateToSection,
                initialFocusSection = focusSection,
            )
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .75f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(scrollState).padding(18.dp),
                    content = settingsContent,
                )
            }
        }
    }
}

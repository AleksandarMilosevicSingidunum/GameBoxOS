package com.gamebox.os.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.data.DownloadRepository
import com.gamebox.os.data.GameRepository
import com.gamebox.os.download.DownloadTelemetryTracker
import com.gamebox.os.download.RemoteDownloadController
import com.gamebox.os.download.assessDownloadCapacity
import com.gamebox.os.download.formatCapacityWarning
import com.gamebox.os.download.formatDownloadTelemetry
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.InstallState
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.importer.BrowserDownloadRecord
import com.gamebox.os.importer.BrowserDownloadStatus

@Composable
internal fun DownloadsScreen(
    repository: GameRepository,
    downloadRepository: DownloadRepository,
    remoteDownloadController: RemoteDownloadController,
    compact: Boolean,
    onStore: () -> Unit,
    onSettings: () -> Unit,
    browserDownloadRecords: List<BrowserDownloadRecord>? = null,
) {
    val jobs by downloadRepository.observeJobs().collectAsState()
    val context = LocalContext.current
    val browserDownloadStore = remember(context) {
        (context.applicationContext as GameBoxApplication).browserDownloadStore
    }
    val liveBrowserDownloads by browserDownloadStore.records.collectAsState()
    val browserDownloads = browserDownloadRecords ?: liveBrowserDownloads
    val browserDownloadSections = remember(browserDownloads) {
        browserDownloadSections(browserDownloads)
    }
    val browserDownloadsInManager = browserDownloadSections.active
    val completedBrowserDownloads = browserDownloadSections.completed
    val completedJobs = remember(jobs) { jobs.filter { it.status == DownloadStatus.COMPLETED } }
    val queuedJobs = remember(jobs) { jobs.filter { it.status != DownloadStatus.COMPLETED } }
    var showingCompleted by rememberSaveable { mutableStateOf(false) }
    val latestBrowserDownloadId = browserDownloads.firstOrNull()?.downloadId
    LaunchedEffect(
        latestBrowserDownloadId,
        browserDownloadsInManager.size,
        completedBrowserDownloads.size,
    ) {
        // Keep a newly queued transfer in Active; once a fast background import
        // finishes, surface its history instead of leaving the user on an empty queue.
        showingCompleted = shouldShowCompletedDownloadsByDefault(browserDownloadSections)
    }
    val visibleBrowserDownloads = if (showingCompleted) completedBrowserDownloads else browserDownloadsInManager
    val visibleJobs = if (showingCompleted) completedJobs else queuedJobs
    val storageCapacity = rememberStorageCapacitySnapshot(context)
    val controllerActions = LocalControllerActions.current
    val focusManager = LocalFocusManager.current
    val emptyDownloadsFocusRequester = remember { FocusRequester() }
    val activeDownloadsFocusRequester = remember { FocusRequester() }
    var emptyDownloadsFocusClaimed by remember { mutableStateOf(false) }

    LaunchedEffect(visibleJobs.isEmpty(), visibleBrowserDownloads.isEmpty(), showingCompleted) {
        if (visibleJobs.isEmpty() && visibleBrowserDownloads.isEmpty() && !showingCompleted) {
            repeat(8) {
                if (runCatching { emptyDownloadsFocusRequester.requestFocus() }.getOrDefault(false)) {
                    return@LaunchedEffect
                }
                withFrameNanos { }
            }
            kotlinx.coroutines.delay(300)
            focusManager.clearFocus(force = true)
            runCatching { emptyDownloadsFocusRequester.requestFocus() }
        }
    }

    val actionable = remember(jobs, repository) {
        jobs.mapNotNull { job ->
            repository.game(job.gameId)
                ?.takeIf { it.sourceUrl != null && it.expectedSha256 != null }
                ?.let { game -> job to game }
        }
    }
    val currentActionable by rememberUpdatedState(actionable)
    val firstActionableJob = actionable.firstOrNull { (job, _) ->
        job.status in setOf(
            DownloadStatus.QUEUED,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.PAUSED,
            DownloadStatus.FAILED,
            DownloadStatus.CANCELLED,
        )
    }?.first

    LaunchedEffect(firstActionableJob?.id, firstActionableJob?.status) {
        if (firstActionableJob != null) {
            repeat(8) {
                val focused = runCatching {
                    focusManager.clearFocus(force = true)
                    activeDownloadsFocusRequester.requestFocus()
                }.getOrDefault(false)
                if (focused) return@LaunchedEffect
                withFrameNanos { }
            }
            kotlinx.coroutines.delay(300)
            focusManager.clearFocus(force = true)
            runCatching { activeDownloadsFocusRequester.requestFocus() }
        }
    }

    val actionPlan = downloadControllerActionPlan(actionable.map { it.first.status })
    DisposableEffect(controllerActions, actionPlan) {
        val lease = controllerActions?.configure(
            xLabel = actionPlan.xLabel,
            onX = {
                when (actionPlan.xAction) {
                    DownloadControllerAction.RETRY_FAILED -> currentActionable
                        .filter { it.first.status == DownloadStatus.FAILED || it.first.status == DownloadStatus.CANCELLED }
                        .forEach { remoteDownloadController.install(it.second) }
                    else -> onStore()
                }
            },
            yLabel = actionPlan.yLabel,
            onY = {
                when (actionPlan.yAction) {
                    DownloadControllerAction.PAUSE_ACTIVE -> currentActionable
                        .filter { it.first.status == DownloadStatus.DOWNLOADING }
                        .forEach { remoteDownloadController.pause(it.second) }
                    DownloadControllerAction.RESUME_PAUSED -> currentActionable
                        .filter { it.first.status == DownloadStatus.PAUSED }
                        .forEach { remoteDownloadController.resume(it.second) }
                    else -> onSettings()
                }
            },
        )
        onDispose {
            val owner = controllerActions
            if (owner != null && lease != null) owner.clear(lease)
        }
    }

    val telemetryTracker = remember { DownloadTelemetryTracker() }
    Column(Modifier.fillMaxSize().verticalScroll(restoredScrollState("downloads"))) {
        Text(
            if (compact) "Downloads" else "Download Manager",
            fontSize = if (compact) 28.sp else 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Manage active transfers, completed games, and verified installation",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        )
        DownloadManagerSectionTabs(
            queueCount = queuedJobs.size + browserDownloadsInManager.size,
            completedCount = completedJobs.size + completedBrowserDownloads.size,
            showingCompleted = showingCompleted,
            onSelectCompleted = { showingCompleted = it },
        )
        Spacer(Modifier.height(14.dp))
        if (!compact) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DownloadMetricCard(Icons.Rounded.Downloading, (jobs.count { it.status == DownloadStatus.DOWNLOADING } + browserDownloadsInManager.count { it.status == BrowserDownloadStatus.DOWNLOADING }).toString(), "Active downloads", Modifier.weight(1f))
                DownloadMetricCard(Icons.Rounded.Schedule, (jobs.count { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.PAUSED } + browserDownloadsInManager.count { it.status in setOf(BrowserDownloadStatus.QUEUED, BrowserDownloadStatus.PAUSED) }).toString(), "Queued or paused", Modifier.weight(1f))
                DownloadMetricCard(Icons.Rounded.Verified, (jobs.count { it.status == DownloadStatus.COMPLETED } + browserDownloads.count { it.status == BrowserDownloadStatus.IMPORTED }).toString(), "Completed", Modifier.weight(1f))
                DownloadMetricCard(
                    Icons.Rounded.Storage,
                    storageCapacity?.let { formatBytes(it.availableBytes) } ?: "Checking…",
                    "Free space",
                    Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        val downloadListContent: @Composable ColumnScope.() -> Unit = {
            if (visibleJobs.isEmpty() && visibleBrowserDownloads.isEmpty()) {
                val browseStoreModifier = Modifier
                    .focusRequester(emptyDownloadsFocusRequester)
                    .bringFocusedControlIntoView()
                    .onGloballyPositioned {
                        if (!emptyDownloadsFocusClaimed) {
                            val focused = runCatching {
                                focusManager.clearFocus(force = true)
                                emptyDownloadsFocusRequester.requestFocus()
                            }.getOrDefault(false)
                            if (focused) emptyDownloadsFocusClaimed = true
                        }
                    }
                BlueprintPanel(
                    Modifier.fillMaxWidth().heightIn(min = if (compact) 0.dp else 424.dp),
                ) {
                    if (compact) {
                        Icon(
                            if (showingCompleted) Icons.Rounded.Verified else Icons.Rounded.Downloading,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(if (showingCompleted) "Nothing completed yet" else "No active downloads", fontWeight = FontWeight.Bold)
                        Text(
                            if (showingCompleted) "Finished and imported games will appear here."
                            else "Games you queue from authorized sources will appear here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                        BlueprintButton(
                            onClick = { if (showingCompleted) showingCompleted = false else onStore() },
                            modifier = browseStoreModifier,
                        ) { Text(if (showingCompleted) "View Download Queue" else "Browse Store") }
                    } else {
                        Box(
                            Modifier.fillMaxWidth().height(398.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                ) {
                                    Icon(
                                        Icons.Rounded.Downloading,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(16.dp).size(32.dp),
                                    )
                                }
                                Text(
                                    if (showingCompleted) "Nothing completed yet" else "Your queue is clear",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                )
                                Text(
                                    if (showingCompleted) "Completed transfers and imported games will appear here."
                                    else "Transfers started from a game's page appear here and continue in the background. Completed files are verified and added to your Library.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.widthIn(max = 560.dp),
                                )
                                if (!showingCompleted) {
                                    Row(
                                        Modifier.fillMaxWidth().widthIn(max = 860.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        DownloadEmptyFlowStep(
                                            number = "01",
                                            title = "Choose a game",
                                            description = "Open a title from Store",
                                            modifier = Modifier.weight(1f),
                                        )
                                        DownloadEmptyFlowStep(
                                            number = "02",
                                            title = "Download",
                                            description = "Tracking continues in background",
                                            modifier = Modifier.weight(1f),
                                        )
                                        DownloadEmptyFlowStep(
                                            number = "03",
                                            title = "Ready to play",
                                            description = "Verified copy joins your Library",
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                                BlueprintButton(
                                    onClick = { if (showingCompleted) showingCompleted = false else onStore() },
                                    modifier = browseStoreModifier,
                                ) {
                                    Text(if (showingCompleted) "View Download Queue" else "Browse Store", fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (visibleBrowserDownloads.isNotEmpty()) {
                Text(if (showingCompleted) "Completed browser downloads" else "Browser downloads", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                visibleBrowserDownloads.forEach { browserDownload ->
                    BrowserDownloadQueueItem(
                        record = browserDownload,
                        onRetry = browserDownload.downloadUrl?.takeIf {
                            browserDownload.status in setOf(
                                BrowserDownloadStatus.FAILED,
                                BrowserDownloadStatus.PAUSED,
                            )
                        }?.let {
                            {
                                (context.applicationContext as GameBoxApplication)
                                    .browserDownloadCoordinator.retry(browserDownload.downloadId)
                            }
                        },
                    )
                }
            }

            visibleJobs.forEach { job ->
                val telemetry = telemetryTracker.sample(job, System.currentTimeMillis())
                val capacityWarning = storageCapacity?.let { capacity ->
                    assessDownloadCapacity(job, capacity.availableBytes)
                }
                    ?.let(::formatCapacityWarning)
                val primaryActionFocusRequester = activeDownloadsFocusRequester
                    .takeIf { job.id == firstActionableJob?.id }
                val remoteGame = repository.game(job.gameId)
                    ?.takeIf { it.sourceUrl != null && it.expectedSha256 != null }
                DownloadQueueItem(
                    job = job,
                    remoteGame = remoteGame,
                    remoteDownloadController = remoteDownloadController,
                    telemetryDescription = telemetry?.let(::formatDownloadTelemetry),
                    capacityWarning = capacityWarning,
                    primaryActionFocusRequester = primaryActionFocusRequester,
                    onDismissUnavailable = {
                        downloadRepository.cancel(job.gameId)
                        if (repository.game(job.gameId)?.state in setOf(
                                InstallState.QUEUED,
                                InstallState.DOWNLOADING,
                                InstallState.PAUSED,
                                InstallState.VERIFYING,
                                InstallState.INSTALLING,
                            )
                        ) repository.setInstallState(job.gameId, InstallState.FAILED)
                    },
                )
            }
        }

        if (compact) {
            downloadListContent()
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), content = downloadListContent)
                Column(Modifier.width(224.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BlueprintPanel(Modifier.fillMaxWidth()) {
                        Text("Network", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        HomeStatusItem(Icons.Rounded.Wifi, "Connection", "Automatic")
                        Text(
                            "Bandwidth is shared with streaming and catalog sync.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                    BlueprintPanel(Modifier.fillMaxWidth()) {
                        Text("Storage", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "${storageCapacity?.let { formatBytes(it.availableBytes) } ?: "Checking…"} free",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                        )
                        LinearProgressIndicator(
                            progress = { storageCapacity?.usedFraction ?: 0f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    BlueprintPanel(Modifier.fillMaxWidth()) {
                        Text("Recent Completed", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val completedTitles = (completedBrowserDownloads.map { it.title } + completedJobs.map { it.title }).take(3)
                        if (completedTitles.isEmpty()) {
                            Text("Nothing completed yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        completedTitles.forEach { HomeStatusItem(Icons.Rounded.CheckCircle, it, "Ready") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadEmptyFlowStep(
    number: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(number, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(
                    description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
internal fun DownloadManagerSectionTabs(
    queueCount: Int,
    completedCount: Int,
    showingCompleted: Boolean,
    onSelectCompleted: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BlueprintFilterChip(
            selected = !showingCompleted,
            onClick = { onSelectCompleted(false) },
            label = { Text("Download Queue · $queueCount") },
            modifier = Modifier,
        )
        BlueprintFilterChip(
            selected = showingCompleted,
            onClick = { onSelectCompleted(true) },
            label = { Text("Completed · $completedCount") },
            modifier = Modifier,
        )
    }
}

@Composable
internal fun BrowserDownloadQueueItem(record: BrowserDownloadRecord, onRetry: (() -> Unit)? = null) {
    BlueprintPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(record.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    "${record.platformName} · ${record.displayName}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            Text(
                when (record.status) {
                    BrowserDownloadStatus.QUEUED -> "Queued"
                    BrowserDownloadStatus.DOWNLOADING -> "Downloading"
                    BrowserDownloadStatus.PAUSED -> "Paused by Android"
                    BrowserDownloadStatus.IMPORTING -> "Verifying & importing"
                    BrowserDownloadStatus.IMPORTED -> "Added to Library"
                    BrowserDownloadStatus.FAILED -> "Needs attention"
                },
                color = if (record.status == BrowserDownloadStatus.FAILED) {
                    MaterialTheme.colorScheme.error
                } else MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        when {
            record.status in setOf(BrowserDownloadStatus.DOWNLOADING, BrowserDownloadStatus.PAUSED) ->
                TrackedBrowserDownloadProgress(record)
            record.status == BrowserDownloadStatus.IMPORTING ->
                TrackedBrowserImportProgress(record)
            record.status in setOf(BrowserDownloadStatus.QUEUED, BrowserDownloadStatus.DOWNLOADING, BrowserDownloadStatus.PAUSED) ->
                LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        record.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        if (record.status in setOf(BrowserDownloadStatus.FAILED, BrowserDownloadStatus.PAUSED) && onRetry != null) {
            BlueprintButton(onClick = onRetry) {
                Text(if (record.status == BrowserDownloadStatus.PAUSED) "Restart download" else "Retry download or import")
            }
        }
    }
}

@Composable
private fun DownloadMetricCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.70f)),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gamebox.os.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.importer.AuthorizedRomImporter
import com.gamebox.os.importer.RomImportPolicy
import com.gamebox.os.importer.BrowserDownloadStatus
import com.gamebox.os.importer.cancelManualRomImport
import com.gamebox.os.importer.enqueueManualRomImport
import com.gamebox.os.importer.DiscoveryImportWorkflow
import com.gamebox.os.TrackedSourceBrowserActivity
import com.gamebox.os.catalog.legalSourceLinks
import com.gamebox.os.display.externalDisplayActivityResultOptions
import com.gamebox.os.source.GameSourceConfig

@Composable
internal fun DiscoveryDetailsScreen(
    game: DiscoveryGame,
    platformName: String,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    importer: AuthorizedRomImporter,
    repository: GameRepository,
    gameSources: List<GameSourceConfig> = emptyList(),
    favoriteEnabled: Boolean = true,
    directSource: ConfiguredSourceLink? = null,
    sourceNotice: String? = null,
    onOpenDownloads: () -> Unit = {},
) {
    val detail = remember(game, platformName) { GameDetailPresentation.from(game, platformName) }
    val heroDescription = remember(detail.description, directSource) {
        if (directSource?.tracksBrowserDownload == true) {
            "Download from ${directSource.label} in GameBox, or import a copy you own."
        } else detail.description
    }
    val context = LocalContext.current
    val primaryActionFocusRequester = remember(game.id) { FocusRequester() }
    val documentPickerOptions = remember(context) { externalDisplayActivityResultOptions(context) }
    val importWorkflow = remember(context, importer, repository, game, platformName) {
        DiscoveryImportWorkflow(context, importer, repository, game, platformName)
    }
    val browserDownloadStore = remember(context) {
        (context.applicationContext as com.gamebox.os.GameBoxApplication).browserDownloadStore
    }
    val browserDownloadRecords by browserDownloadStore.records.collectAsState()
    val latestManualImport = remember(browserDownloadRecords, game.id) {
        browserDownloadRecords.filter { it.manualImport && it.gameId == game.id.value }
            .maxByOrNull(com.gamebox.os.importer.BrowserDownloadRecord::createdAtMillis)
    }
    val activeManualImport = latestManualImport?.takeIf {
        it.status in setOf(BrowserDownloadStatus.QUEUED, BrowserDownloadStatus.IMPORTING)
    }
    val legalSources = remember(game.title, game.platformId) {
        legalSourceLinks(game.title, game.platformId)
    }
    val configuredSources = remember(game.title, platformName, gameSources, directSource) {
        configuredDetailsSourceLinks(game.title, platformName, gameSources, directSource)
    }
    val importPlatformLabel = remember(platformName) {
        RomImportPolicy.profileLabel(platformName)
    }
    val importFormats = remember(platformName) {
        RomImportPolicy.supportedExtensionsLabel(platformName)
    }
    var importMessage by remember(game.id) { mutableStateOf<String?>(null) }
    fun enqueueManualImport(uris: List<Uri>, grantFlags: Int) {
        try {
            enqueueManualRomImport(
                context,
                browserDownloadStore,
                game,
                platformName,
                uris,
                uris.map(importWorkflow::displayName),
                grantFlags,
            )
            importMessage = "Import queued. Progress is available in Download Manager."
        } catch (error: Exception) {
            importMessage = "Could not queue import: ${error.message?.take(140) ?: "choose the files again"}"
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        OpenGameFilesContract(allowMultiple = false)
    ) { selection ->
        val uris = selection.uris
        val uri = selection.uris.singleOrNull()
        if (uri == null) {
            importMessage = "No file selected"
        } else {
            enqueueManualImport(selection.uris, selection.grantFlags)
        }
    }
    val importSetLauncher = rememberLauncherForActivityResult(
        OpenGameFilesContract(allowMultiple = true)
    ) { selection ->
        val uris = selection.uris
        if (uris.isEmpty()) {
            importMessage = "No files selected"
        } else {
            enqueueManualImport(uris, selection.grantFlags)
        }
    }

    val browserDownloadRecord = remember(browserDownloadRecords, game.id) {
        browserDownloadRecords.firstOrNull { !it.manualImport && it.gameId == game.id.value }
    }
    val trackingBrowserDownload = browserDownloadRecord?.status in setOf(
        com.gamebox.os.importer.BrowserDownloadStatus.QUEUED,
        com.gamebox.os.importer.BrowserDownloadStatus.DOWNLOADING,
        com.gamebox.os.importer.BrowserDownloadStatus.PAUSED,
        com.gamebox.os.importer.BrowserDownloadStatus.IMPORTING,
    )
    val browserDownloadFailed = browserDownloadRecord?.status ==
        com.gamebox.os.importer.BrowserDownloadStatus.FAILED
    val browserDownloadImported = browserDownloadRecord?.status ==
        com.gamebox.os.importer.BrowserDownloadStatus.IMPORTED
    LaunchedEffect(browserDownloadRecord?.status, browserDownloadRecord?.error) {
        browserDownloadRecord?.let { record ->
            importMessage = when (record.status) {
                com.gamebox.os.importer.BrowserDownloadStatus.QUEUED ->
                    "Download queued in GameBox. It continues if you leave this page."
                com.gamebox.os.importer.BrowserDownloadStatus.DOWNLOADING ->
                    "Downloading ${record.displayName} in the background."
                com.gamebox.os.importer.BrowserDownloadStatus.PAUSED ->
                    "${record.displayName} is paused. Open Downloads to restart it."
                com.gamebox.os.importer.BrowserDownloadStatus.IMPORTING ->
                    "Download complete; verifying and importing ${record.title}…"
                com.gamebox.os.importer.BrowserDownloadStatus.IMPORTED ->
                    "${record.title} was verified and added to your Library. The temporary download was moved."
                com.gamebox.os.importer.BrowserDownloadStatus.FAILED ->
                    "Download needs attention: ${record.error ?: "try importing your copy manually"}"
                com.gamebox.os.importer.BrowserDownloadStatus.CANCELLED ->
                    "The tracked download was cancelled."
            }
        }
    }
    val trackedBrowserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        val startError = data?.getStringExtra(TrackedSourceBrowserActivity.EXTRA_DOWNLOAD_ERROR)
        val id = data?.getLongExtra(TrackedSourceBrowserActivity.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
        val stagedName = data?.getStringExtra(TrackedSourceBrowserActivity.EXTRA_STAGED_NAME)
        val displayName = data?.getStringExtra(TrackedSourceBrowserActivity.EXTRA_DISPLAY_NAME)
        if (startError != null) {
            importMessage = "Download was not queued: $startError. Open Downloads for details, or import your own copy."
        } else if (result.resultCode != Activity.RESULT_OK || id < 0 || stagedName == null || displayName == null ||
            browserDownloadStore.get(id) == null
        ) {
            importMessage = "Returned without starting a download. You can still import your copy manually."
        } else {
            importMessage = "Download added to GameBox. It will continue if you leave this page and import when complete."
            onOpenDownloads()
        }
    }
    fun openAndTrackBrowserDownload() {
        val source = directSource ?: return
        trackedBrowserLauncher.launch(
            Intent(context, TrackedSourceBrowserActivity::class.java).apply {
                putExtra(TrackedSourceBrowserActivity.EXTRA_SOURCE_URL, source.url)
                putExtra(TrackedSourceBrowserActivity.EXTRA_SOURCE_LABEL, source.label)
                putExtra(TrackedSourceBrowserActivity.EXTRA_PLATFORM, platformName)
                putExtra(TrackedSourceBrowserActivity.EXTRA_GAME_ID, game.id.value)
                putExtra(TrackedSourceBrowserActivity.EXTRA_GAME_TITLE, game.title)
                putExtra(TrackedSourceBrowserActivity.EXTRA_PLATFORM_ID, game.platformId)
                putExtra(TrackedSourceBrowserActivity.EXTRA_GAME_REGION, game.region)
                putExtra(TrackedSourceBrowserActivity.EXTRA_GAME_RELEASE_DATE, game.releaseDate)
                putExtra(TrackedSourceBrowserActivity.EXTRA_GAME_DESCRIPTION, game.description)
                putExtra(TrackedSourceBrowserActivity.EXTRA_GAME_PLAYERS, game.players)
                game.rating?.let { putExtra(TrackedSourceBrowserActivity.EXTRA_GAME_RATING, it) }
                putExtra(TrackedSourceBrowserActivity.EXTRA_GAME_COVER, game.coverUrl)
                putExtra(TrackedSourceBrowserActivity.EXTRA_GAME_BACKGROUND, game.backgroundUrl)
                putExtra(TrackedSourceBrowserActivity.EXTRA_GAME_LOGO, game.logoUrl)
            },
            documentPickerOptions,
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val narrow = maxWidth < 700.dp
        val showWideCover = !narrow && !detail.coverUrl.isNullOrBlank()
        Column(Modifier.fillMaxSize().verticalScroll(restoredScrollState("discovery." + game.id.value)), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GameArtworkHeroFrame(
                artworkUrl = detail.heroUrl,
                fallbackKey = detail.title,
                modifier = Modifier.fillMaxWidth().height(if (narrow) 320.dp else 300.dp),
                borderColor = MaterialTheme.colorScheme.outline,
            ) {
                Column(
                    Modifier.fillMaxSize().padding(if (narrow) 18.dp else 24.dp)
                        .padding(end = if (showWideCover) 176.dp else 0.dp),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConsoleBrandMark(detail.platform, true, Modifier.size(23.dp))
                        Text(detail.platform.uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(detail.title, fontSize = if (narrow) 28.sp else 38.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(if (narrow) 1f else .75f))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        detail.releaseLabel?.let { DetailMetric(Icons.Rounded.CalendarMonth, it) }
                        detail.playersLabel?.let { DetailMetric(Icons.Rounded.Groups, it) }
                        detail.ratingLabel?.let { DetailMetric(Icons.Rounded.Star, it) }
                    }
                    Text(heroDescription,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 540.dp).padding(top = 9.dp))
                    Spacer(Modifier.height(12.dp))
                    DiscoveryDetailsHeroActions(
                        compact = narrow,
                        primaryActionFocusRequester = primaryActionFocusRequester,
                        importing = activeManualImport != null,
                        downloadAvailable = directSource?.tracksBrowserDownload == true,
                        downloadWaiting = trackingBrowserDownload,
                        downloadFailed = browserDownloadFailed,
                        downloadImported = browserDownloadImported,
                        favoriteEnabled = favoriteEnabled,
                        favorited = detail.favorite,
                        onImport = { importLauncher.launch(arrayOf("*/*"), documentPickerOptions) },
                        onDownload = ::openAndTrackBrowserDownload,
                        onOpenDownloads = onOpenDownloads,
                        onFavorite = onFavorite,
                        onBack = onBack,
                    )
                }
                if (showWideCover) {
                    GameCoverPoster(
                        title = detail.title,
                        artworkUrl = detail.coverUrl,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)
                            .width(134.dp).height(202.dp),
                    )
                }
            }
            DiscoveryImportSection(
                platformLabel = importPlatformLabel,
                acceptedFormats = importFormats,
                importing = activeManualImport != null,
                progress = activeManualImport?.let(::manualImportProgress),
                message = importMessage ?: latestManualImport?.let { record ->
                    when (record.status) {
                        BrowserDownloadStatus.IMPORTED -> "Your copy was verified and added to your Library."
                        BrowserDownloadStatus.FAILED, BrowserDownloadStatus.CANCELLED -> record.error
                        else -> null
                    }
                },
                supportsDiscSet = RomImportPolicy.supportsMultiFile(platformName),
                showCopyImport = directSource?.tracksBrowserDownload == true,
                onCancel = {
                    activeManualImport?.let { cancelManualRomImport(context, browserDownloadStore, it) }
                    importMessage = "Cancelling import…"
                },
                onImportCopy = { importLauncher.launch(arrayOf("*/*"), documentPickerOptions) },
                onImportDiscSet = { importSetLauncher.launch(arrayOf("*/*"), documentPickerOptions) },
            )
            when (browserDownloadRecord?.status) {
                com.gamebox.os.importer.BrowserDownloadStatus.DOWNLOADING,
                com.gamebox.os.importer.BrowserDownloadStatus.PAUSED ->
                    TrackedBrowserDownloadProgress(browserDownloadRecord)
                com.gamebox.os.importer.BrowserDownloadStatus.QUEUED,
                com.gamebox.os.importer.BrowserDownloadStatus.IMPORTING ->
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                else -> Unit
            }
            sourceNotice?.let { notice ->
                Surface(
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "Source notice: $notice"
                        liveRegion = LiveRegionMode.Assertive
                    },
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                    shape = MaterialTheme.shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Extra source details unavailable", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(
                                notice,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                            )
                        }
                    }
                }
            }
            GameScreenshotGallery(detail, narrow)
            if (legalSources.isNotEmpty()) {
                Text("Find a legal copy", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    legalSources.forEach { source ->
                        DiscoveryExternalLinkButton(source.label) {
                            openExternalDiscoveryLink(context, source.url.toUri(), source.label) { message ->
                                importMessage = message
                            }
                        }
                    }
                }
            }
            if (configuredSources.isNotEmpty()) {
                Text("Configured sources", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    configuredSourceGuidance(directSource?.tracksBrowserDownload == true),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    configuredSources.forEach { source ->
                        DiscoveryExternalLinkButton(source.label) {
                            openExternalDiscoveryLink(context, source.url.toUri(), source.label) { message ->
                                importMessage = message
                            }
                        }
                    }
                }
            }
            game.description?.takeIf { it.isNotBlank() }?.let {
                BlueprintPanel(Modifier.fillMaxWidth()) {
                    Text("About this game", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

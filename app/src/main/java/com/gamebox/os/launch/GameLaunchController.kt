package com.gamebox.os.launch

import android.app.Activity
import android.app.ActivityOptions
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.Display
import com.gamebox.os.display.toGameBoxFullscreenLaunchBundle
import androidx.core.content.FileProvider
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.LocalContentFile
import com.gamebox.os.download.Sha256Verifier
import com.gamebox.os.download.VerificationResult
import com.gamebox.os.storage.ExternalLibraryContentStore
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

enum class GatewayResult {
    LAUNCHED, EMULATOR_UNAVAILABLE, CONTENT_MISSING, VERIFICATION_FAILED, HANDOFF_REJECTED
}

internal fun hasRetroArchCoreContract(capability: EmulatorCapability): Boolean =
    !capability.packageName.startsWith("com.retroarch") ||
        (!capability.requiredCore.isNullOrBlank() &&
            capability.retroArchCoreFileName?.matches(Regex("[a-z0-9_]+_libretro_android\\.so")) == true)

internal fun externalDisplayLaunchId(displayId: Int?): Int? =
    displayId?.takeIf { it > Display.DEFAULT_DISPLAY }

internal fun startIntentOnDisplay(
    context: Context,
    intent: Intent,
    displayId: Int?,
    sourceActivity: Activity? = null,
) {
    val targetDisplayId = externalDisplayLaunchId(displayId)
    val activity = sourceActivity?.takeUnless { it.isFinishing || it.isDestroyed }
    if (activity != null) {
        // This path is called from the preparation IO dispatcher. Start from the
        // live TV Activity on its main thread and keep the emulator in that task;
        // HONOR otherwise promotes NEW_TASK launches to freeform and Dolphin's
        // delayed game Activity start is rejected during the task hand-off.
        intent.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startFromHostActivity(activity, intent, targetDisplayId)
    } else if (targetDisplayId == null) {
        context.startActivity(intent)
    } else {
        val options = ActivityOptions.makeBasic().apply {
            launchDisplayId = targetDisplayId
        }
        context.startActivity(intent, options.toGameBoxFullscreenLaunchBundle())
    }
}

private fun startFromHostActivity(activity: Activity, intent: Intent, targetDisplayId: Int?) {
    fun start() {
        check(!activity.isFinishing && !activity.isDestroyed) { "GameBox host Activity is no longer available" }
        if (targetDisplayId == null) {
            activity.startActivity(intent)
        } else {
            val options = ActivityOptions.makeBasic().apply {
                launchDisplayId = targetDisplayId
            }
            activity.startActivity(intent, options.toGameBoxFullscreenLaunchBundle())
        }
    }

    if (Looper.myLooper() == Looper.getMainLooper()) {
        start()
        return
    }
    val completed = CountDownLatch(1)
    var failure: Throwable? = null
    Handler(Looper.getMainLooper()).post {
        try {
            start()
        } catch (error: Throwable) {
            failure = error
        } finally {
            completed.countDown()
        }
    }
    check(completed.await(10, TimeUnit.SECONDS)) { "Timed out starting the emulator from the TV Activity" }
    failure?.let { throw it }
}
interface PackageGateway {
    fun launch(capability: EmulatorCapability, preparation: LaunchPreparation = LaunchPreparation()): GatewayResult
    fun launchOnDisplay(
        capability: EmulatorCapability,
        preparation: LaunchPreparation = LaunchPreparation(),
        displayId: Int? = null,
    ): GatewayResult = launch(capability, preparation)
    fun launchOnDisplayFromActivity(
        capability: EmulatorCapability,
        preparation: LaunchPreparation,
        displayId: Int?,
        sourceActivity: Activity,
    ): GatewayResult = launchOnDisplay(capability, preparation, displayId)
    fun cleanupLaunchedContent() = Unit
}

class AndroidPackageGateway(
    private val context: Context,
    private val verifier: Sha256Verifier = Sha256Verifier(),
    private val externalContent: ExternalLibraryContentStore? = null,
) : PackageGateway {
    private val retroArchContentPublisher = RetroArchContentPublisher(context)

    override fun cleanupLaunchedContent() = retroArchContentPublisher.cleanup()

    override fun launch(capability: EmulatorCapability, preparation: LaunchPreparation): GatewayResult =
        launchInternal(capability, preparation, displayId = null)

    override fun launchOnDisplay(
        capability: EmulatorCapability,
        preparation: LaunchPreparation,
        displayId: Int?,
    ): GatewayResult = launchInternal(capability, preparation, displayId, sourceActivity = null)

    override fun launchOnDisplayFromActivity(
        capability: EmulatorCapability,
        preparation: LaunchPreparation,
        displayId: Int?,
        sourceActivity: Activity,
    ): GatewayResult = launchInternal(capability, preparation, displayId, sourceActivity)

    private fun launchInternal(
        capability: EmulatorCapability,
        preparation: LaunchPreparation,
        displayId: Int?,
        sourceActivity: Activity? = null,
    ): GatewayResult {
        preparation.checkActive()
        // A previous emulator session may have been interrupted before the
        // host resumed, so its temporary MediaStore export was never removed.
        // Reconcile those app-owned exports before publishing the next one;
        // otherwise every retry creates another duplicate in Downloads.
        cleanupLaunchedContent()
        val resolvedPackage = resolvePackageName(capability.packageName)
            ?: return GatewayResult.EMULATOR_UNAVAILABLE
        val externalContentStore = externalContent
        val launcherIntent = context.packageManager.getLaunchIntentForPackage(resolvedPackage)
            ?: return GatewayResult.EMULATOR_UNAVAILABLE
        // RetroActivityFuture starts native code immediately. Launching it without
        // a known core contract can crash or strand the external display on black.
        if (resolvedPackage.startsWith("com.retroarch") && !hasRetroArchCoreContract(capability)) {
            return GatewayResult.HANDOFF_REJECTED
        }
        val installRoot = context.filesDir.resolve(capability.contentRoot.directoryName).canonicalFile
        val rootPrefix = installRoot.path + File.separator
        val approvedFiles = listOf(
            LocalContentFile(
                capability.contentRelativePath,
                capability.expectedSha256,
                capability.mimeType,
            )
        ) + capability.companionFiles
        val contentUris = mutableListOf<android.net.Uri>()
        val retroArchSources = mutableListOf<RetroArchContentSource>()
        approvedFiles.forEach { approved ->
            val local = File(installRoot, approved.relativePath).canonicalFile
            if (!local.path.startsWith(rootPrefix)) return GatewayResult.CONTENT_MISSING
            val external = if (!local.isFile && capability.contentRoot == EmulatorContentRoot.INSTALLED) {
                externalContentStore?.content(capability.gameId.value, approved.relativePath)
            } else null
            val verified = when {
                local.isFile -> local.inputStream().use {
                    verifier.verify(it, approved.sha256, preparation::checkActive)
                }
                external != null -> (externalContentStore ?: return GatewayResult.CONTENT_MISSING).open(external).use {
                    verifier.verify(it, approved.sha256, preparation::checkActive)
                }
                else -> return GatewayResult.CONTENT_MISSING
            }
            if (verified != VerificationResult.Verified) return GatewayResult.VERIFICATION_FAILED
            val contentUri = if (local.isFile) {
                FileProvider.getUriForFile(context, context.packageName + ".files", local)
            } else {
                requireNotNull(external).uri
            }
            contentUris += contentUri
            val sourceOpener: () -> java.io.InputStream = if (local.isFile) {
                { local.inputStream() }
            } else {
                val content = requireNotNull(external)
                val contentStore = externalContentStore ?: return GatewayResult.CONTENT_MISSING
                { contentStore.open(content) }
            }
            retroArchSources += RetroArchContentSource(
                relativePath = approved.relativePath,
                mimeType = approved.mimeType,
                openSource = sourceOpener,
            )
        }
        val uri = contentUris.first()
        val plan = EmulatorIntentPolicy.plan(
            packageName = resolvedPackage,
            contentUri = uri.toString(),
            graphicsProfile = capability.graphicsProfile,
            retroArchCorePath = capability.retroArchCoreFileName?.let { coreFileName ->
                // RetroArch's externally launched activity resolves an absolute core path
                // from its app-private data directory. Using the /data/user/0 alias is
                // not reliable across current Android/RetroArch combinations and can
                // leave RetroActivityFuture on a black surface when the core cannot open.
                retroArchCorePath(resolvedPackage, coreFileName)
            },
        )
        // RetroArch's native launcher expects a filesystem path. Publish every
        // verified file in a descriptor-backed disc set together: CUE/GDI files
        // refer to their tracks by sibling filename, so exporting only the
        // descriptor can make RetroArch hang while resolving absent tracks.
        val retroArchRom = if (resolvedPackage.startsWith("com.retroarch")) {
            retroArchContentPublisher.publishSet(retroArchSources, capability.contentRelativePath)
                ?: return GatewayResult.HANDOFF_REJECTED
        } else {
            uri.toString()
        }
        val intent = (when (plan.style) {
            EmulatorIntentStyle.ACTION_VIEW -> Intent(Intent.ACTION_VIEW)
                // PPSSPP accepts scoped ACTION_VIEW URIs, but its Android
                // resolver rejects a wildcard MIME for a content URI even
                // though its manifest advertises */*. Preserve the verified
                // game MIME so ISO/CSO/PBP paths resolve on real builds.
                // Azahar's official vanilla APK advertises only
                // application/octet-stream for content:// ACTION_VIEW.
                .setDataAndType(
                    uri,
                    if (resolvedPackage == "org.azahar_emu.azahar") {
                        "application/octet-stream"
                    } else capability.mimeType,
                )
                .setPackage(resolvedPackage)
                .apply {
                    plan.activityClassName?.let { setClassName(resolvedPackage, it) }
                }
            EmulatorIntentStyle.LAUNCHER_EXTRAS -> {
                val baseIntent = plan.activityClassName?.let { activityClassName ->
                    Intent().setClassName(resolvedPackage, activityClassName)
                } ?: launcherIntent
                Intent(baseIntent).apply {
                    plan.stringExtras.forEach { (key, value) -> putExtra(key, value) }
                    if (resolvedPackage.startsWith("com.retroarch")) {
                        putExtra(EmulatorIntentPolicy.RETROARCH_ROM, retroArchRom)
                    }
                    plan.stringArrayExtras.forEach { (key, values) -> putExtra(key, values.toTypedArray()) }
                }
            }
        })
            .apply {
                if (resolvedPackage.startsWith("com.retroarch")) {
                    val target = context.packageManager.getApplicationInfo(resolvedPackage, 0)
                    EmulatorIntentPolicy.retroArchRuntimeExtras(
                        dataDir = target.dataDir,
                        apkPath = target.sourceDir,
                        externalDir = Environment.getExternalStorageDirectory().path +
                            "/Android/data/$resolvedPackage/files",
                        storageRoot = Environment.getExternalStorageDirectory().path,
                    ).forEach { (key, value) -> putExtra(key, value) }
                }
            }
            .putExtra("gamebox.graphics_profile", capability.graphicsProfile)
            .putExtra("gamebox.graphics_profile_applied", plan.graphicsProfileApplied)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        intent.clipData = contentClipData(uri, contentUris.drop(1))
        if (intent.resolveActivity(context.packageManager) == null) {
            return GatewayResult.HANDOFF_REJECTED
        }
        return preparation.dispatch { runCatching {
            startIntentOnDisplay(context, intent, displayId, sourceActivity)
            GatewayResult.LAUNCHED
        }.getOrElse {
            // Some RetroArch Android builds expose a launcher activity but reject
            // launcher extras. Retry once with the standard scoped ACTION_VIEW contract.
            if (resolvedPackage.startsWith("com.retroarch")) {
                val fallback = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    setPackage(resolvedPackage)
                    clipData = contentClipData(uri, contentUris.drop(1))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching {
                    startIntentOnDisplay(context, fallback, displayId, sourceActivity)
                    GatewayResult.LAUNCHED
                }.getOrDefault(GatewayResult.HANDOFF_REJECTED)
            } else {
                GatewayResult.HANDOFF_REJECTED
            }
        }
    }

    }

    private fun contentClipData(primary: android.net.Uri, companions: List<android.net.Uri>): ClipData =
        ClipData.newRawUri("GameBox content", primary).apply {
            companions.forEach { companionUri -> addItem(ClipData.Item(companionUri)) }
        }

    /**
     * Publishes only the built-in MIT fixture. MediaStore needs no broad storage
     * permission on Android 10+, and produces the conventional path RetroArch needs.
     */
    internal fun publishGalaxyPatrolForRetroArch(
        source: File,
        relativePath: String = "Download/GameBox"
    ): String? = retroArchContentPublisher.publish(
        source = source,
        fileName = "galaxy-patrol.nes",
        mimeType = "application/x-nes-rom",
        relativePath = relativePath,
    )

    private fun resolvePackageName(requested: String): String? {
        return emulatorPackageCandidates(requested).firstOrNull {
            context.packageManager.getLaunchIntentForPackage(it) != null
        }
    }
}

package com.gamebox.os.launch

import java.net.URI

enum class EmulatorIntentStyle { ACTION_VIEW, LAUNCHER_EXTRAS }

data class EmulatorIntentPlan(
    val style: EmulatorIntentStyle,
    val stringExtras: Map<String, String> = emptyMap(),
    val stringArrayExtras: Map<String, List<String>> = emptyMap(),
    val graphicsProfileApplied: Boolean = false,
    val activityClassName: String? = null,
)

object EmulatorIntentPolicy {
    const val PPSSPP_PACKAGE = "org.ppsspp.ppsspp"
    const val DOLPHIN_PACKAGE = "org.dolphinemu.dolphinemu"
    const val PPSSPP_ARGS = "org.ppsspp.ppsspp.Args"
    const val DOLPHIN_AUTO_START_FILES = "AutoStartFiles"
    const val RETROARCH_ROM = "ROM"
    const val RETROARCH_CORE = "LIBRETRO"
    const val RETROARCH_CONFIG = "CONFIGFILE"
    const val RETROARCH_DATA = "DATADIR"
    const val RETROARCH_APK = "APK"
    const val RETROARCH_SDCARD = "SDCARD"
    const val RETROARCH_EXTERNAL = "EXTERNAL"
    const val RETROARCH_ACTIVITY = "com.retroarch.browser.retroactivity.RetroActivityFuture"
    private const val RETROARCH_PACKAGE = "com.retroarch"

    fun plan(
        packageName: String,
        contentUri: String,
        graphicsProfile: String,
        retroArchCorePath: String? = null,
    ): EmulatorIntentPlan {
        require(packageName.isNotBlank()) { "Emulator package must not be blank" }
        val uri = runCatching { URI(contentUri) }.getOrNull()
        require(
            uri != null &&
                uri.scheme.equals("content", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.query == null &&
                uri.fragment == null
        ) { "Emulator content must use an absolute scoped content URI" }
        return when {
            packageName == PPSSPP_PACKAGE -> ppssppPlan(contentUri, graphicsProfile)
            packageName == RETROARCH_PACKAGE ||
                packageName.startsWith(RETROARCH_PACKAGE + ".") -> EmulatorIntentPlan(
                    style = EmulatorIntentStyle.LAUNCHER_EXTRAS,
                    stringExtras = buildMap {
                        put(RETROARCH_ROM, contentUri)
                        retroArchCorePath?.trim()?.takeIf { it.isNotEmpty() }?.let {
                            put(RETROARCH_CORE, it)
                        }
                    },
                    activityClassName = RETROARCH_ACTIVITY,
                )
            packageName == DOLPHIN_PACKAGE -> EmulatorIntentPlan(
                style = EmulatorIntentStyle.LAUNCHER_EXTRAS,
                stringArrayExtras = mapOf(DOLPHIN_AUTO_START_FILES to listOf(contentUri)),
            )
            else -> EmulatorIntentPlan(EmulatorIntentStyle.ACTION_VIEW)
        }
    }

    private fun ppssppPlan(contentUri: String, graphicsProfile: String): EmulatorIntentPlan {
        val graphicsArgument = when (graphicsProfile) {
            "Performance" -> "--graphics=vulkan"
            "Compatibility" -> "--graphics=gles"
            else -> null
        }
        val quotedUri = quote(contentUri)
        val args = listOfNotNull(graphicsArgument, quotedUri).joinToString(" ")
        return EmulatorIntentPlan(
            style = EmulatorIntentStyle.LAUNCHER_EXTRAS,
            stringExtras = mapOf(PPSSPP_ARGS to args),
            graphicsProfileApplied = graphicsArgument != null,
        )
    }

    private fun quote(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    /**
     * RetroActivityFuture reads these paths before it processes the ROM/core extras.
     * Supplying the target app's own locations prevents it from silently starting with
     * GameBox's process paths, which otherwise results in an unrecoverable black window.
     */
    fun retroArchRuntimeExtras(
        dataDir: String,
        apkPath: String,
        externalDir: String,
        storageRoot: String,
    ): Map<String, String> = mapOf(
        RETROARCH_CONFIG to "$dataDir/retroarch.cfg",
        RETROARCH_DATA to dataDir,
        RETROARCH_APK to apkPath,
        RETROARCH_SDCARD to storageRoot,
        RETROARCH_EXTERNAL to externalDir,
    )
}


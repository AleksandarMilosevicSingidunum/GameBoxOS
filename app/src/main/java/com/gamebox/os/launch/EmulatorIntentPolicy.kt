package com.gamebox.os.launch

import java.net.URI

enum class EmulatorIntentStyle { ACTION_VIEW, LAUNCHER_EXTRAS }

data class EmulatorIntentPlan(
    val style: EmulatorIntentStyle,
    val stringExtras: Map<String, String> = emptyMap(),
    val stringArrayExtras: Map<String, List<String>> = emptyMap(),
    val graphicsProfileApplied: Boolean = false,
)

object EmulatorIntentPolicy {
    const val PPSSPP_PACKAGE = "org.ppsspp.ppsspp"
    const val DOLPHIN_PACKAGE = "org.dolphinemu.dolphinemu"
    const val PPSSPP_ARGS = "org.ppsspp.ppsspp.Args"
    const val DOLPHIN_AUTO_START_FILES = "AutoStartFiles"
    const val RETROARCH_ROM = "ROM"
    private const val RETROARCH_PACKAGE = "com.retroarch"

    fun plan(packageName: String, contentUri: String, graphicsProfile: String): EmulatorIntentPlan {
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
                    stringExtras = mapOf(RETROARCH_ROM to contentUri),
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
}

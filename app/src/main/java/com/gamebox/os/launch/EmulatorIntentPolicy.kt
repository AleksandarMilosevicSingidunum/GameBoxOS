package com.gamebox.os.launch

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

    fun plan(packageName: String, contentUri: String, graphicsProfile: String): EmulatorIntentPlan {
        require(contentUri.startsWith("content://")) { "Emulator content must use a scoped content URI" }
        return when (packageName) {
            PPSSPP_PACKAGE -> ppssppPlan(contentUri, graphicsProfile)
            DOLPHIN_PACKAGE -> EmulatorIntentPlan(
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

package com.gamebox.os.launch

enum class EmulatorReadinessState {
    READY,
    MISSING_SELECTED,
    NONE_INSTALLED,
    UNSUPPORTED,
}

data class EmulatorReadiness(
    val state: EmulatorReadinessState,
    val selectedPackage: String?,
    val installedOptions: List<String>,
    val message: String,
)

object EmulatorReadinessPolicy {
    fun evaluate(
        approvedOptions: List<String>,
        selectedPackage: String?,
        installedPackages: Set<String>,
        displayName: (String) -> String,
    ): EmulatorReadiness {
        if (approvedOptions.isEmpty()) {
            return EmulatorReadiness(
                state = EmulatorReadinessState.UNSUPPORTED,
                selectedPackage = null,
                installedOptions = emptyList(),
                message = "No approved emulator adapter is available for this platform.",
            )
        }

        val installed = approvedOptions.filter(installedPackages::contains)
        val selected = selectedPackage
            ?.takeIf(approvedOptions::contains)
            ?: approvedOptions.first()

        if (selected in installedPackages) {
            return EmulatorReadiness(
                state = EmulatorReadinessState.READY,
                selectedPackage = selected,
                installedOptions = installed,
                message = displayName(selected) + " is installed and ready for verified content handoff.",
            )
        }

        if (installed.isEmpty()) {
            return EmulatorReadiness(
                state = EmulatorReadinessState.NONE_INSTALLED,
                selectedPackage = selected,
                installedOptions = emptyList(),
                message = "Install " + displayName(selected) + " before launching this game.",
            )
        }

        return EmulatorReadiness(
            state = EmulatorReadinessState.MISSING_SELECTED,
            selectedPackage = selected,
            installedOptions = installed,
            message = displayName(selected) + " is not installed. Choose an installed emulator.",
        )
    }
}

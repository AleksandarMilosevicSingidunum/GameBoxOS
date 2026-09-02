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
        if (approvedOptions.isEmpty()) return EmulatorReadiness(
            EmulatorReadinessState.UNSUPPORTED, null, emptyList(),
            "No approved emulator adapter is available for this platform."
        )

        val installed = approvedOptions.filter(installedPackages::contains)
        val preferred = EmulatorPackageResolver.preferred(approvedOptions, selectedPackage)
        val resolved = EmulatorPackageResolver.resolve(approvedOptions, selectedPackage, installedPackages)
        if (resolved != null) return EmulatorReadiness(
            EmulatorReadinessState.READY, resolved, installed,
            displayName(resolved) + " is installed and ready for verified content handoff."
        )
        return EmulatorReadiness(
            EmulatorReadinessState.NONE_INSTALLED, preferred, emptyList(),
            "Install " + displayName(requireNotNull(preferred)) + " before launching this game."
        )
    }
}

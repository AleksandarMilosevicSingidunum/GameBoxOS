package com.gamebox.os.launch

/**
 * Selects only from an allowlisted emulator set. A stored selection wins when it
 * is installed; otherwise the first installed approved option is used.
 */
object EmulatorPackageResolver {
    fun resolve(
        approvedOptions: List<String>,
        selectedPackage: String?,
        installedPackages: Set<String>,
    ): String? {
        val approvedSelection = selectedPackage?.takeIf { it in approvedOptions }
        return approvedSelection?.takeIf { it in installedPackages }
            ?: approvedOptions.firstOrNull { it in installedPackages }
    }

    fun preferred(
        approvedOptions: List<String>,
        selectedPackage: String?,
    ): String? = selectedPackage?.takeIf { it in approvedOptions }
        ?: approvedOptions.firstOrNull()
}

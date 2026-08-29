package com.gamebox.os.download

enum class InstallRecoveryAction { RETRY, CLEAN_PARTIAL, KEEP_VERIFIED, REPORT_FAILURE }

data class InstallRecoveryPolicy(
    val onVerificationFailure: InstallRecoveryAction = InstallRecoveryAction.CLEAN_PARTIAL,
    val onLowStorage: InstallRecoveryAction = InstallRecoveryAction.REPORT_FAILURE,
    val onProcessDeath: InstallRecoveryAction = InstallRecoveryAction.RETRY
)
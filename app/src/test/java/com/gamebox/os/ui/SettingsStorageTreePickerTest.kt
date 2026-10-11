package com.gamebox.os.ui

import com.gamebox.os.storage.ExternalStorageState
import com.gamebox.os.storage.ExternalStorageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsStorageTreePickerTest {
    @Test
    fun successfulSelectionUsesControllerStatusMessage() {
        val status = ExternalStorageStatus(
            state = ExternalStorageState.AVAILABLE_READ_WRITE,
            displayName = "GameBox",
            message = "External library selected",
        )

        assertEquals(
            "External library selected",
            storageTreePickerMessage(Result.success(status), "Fallback"),
        )
    }

    @Test
    fun failureUsesExceptionMessageOrFlowSpecificFallback() {
        assertEquals(
            "Provider unavailable",
            storageTreePickerMessage(Result.failure(IllegalStateException("Provider unavailable")), "Fallback"),
        )
        assertEquals(
            "Fallback",
            storageTreePickerMessage(Result.failure(IllegalStateException()), "Fallback"),
        )
    }
}

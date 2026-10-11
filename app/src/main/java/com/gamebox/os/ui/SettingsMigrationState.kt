package com.gamebox.os.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gamebox.os.storage.ContentMigrationPlan
import com.gamebox.os.storage.ContentMigrationResult
import com.gamebox.os.storage.InstalledContentMigration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Owns migration planning, confirmation state, and the verified cutover operation. */
@Stable
internal class SettingsMigrationState internal constructor(
    private val context: Context,
    private val migration: InstalledContentMigration,
) {
    var plan by mutableStateOf<ContentMigrationPlan?>(null)
        private set
    var showConfirmation by mutableStateOf(false)
        private set
    var previousResult by mutableStateOf<ContentMigrationResult?>(null)
        private set
    var scanFailed by mutableStateOf(false)
        private set

    internal suspend fun refreshPlan() {
        plan = null
        scanFailed = false
        try {
            plan = withContext(Dispatchers.IO) { migration.plan() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            scanFailed = true
        }
    }

    fun requestConfirmation() {
        if (plan != null) showConfirmation = true
    }

    fun dismissConfirmation() {
        showConfirmation = false
    }

    suspend fun confirm(uri: Uri): ContentMigrationResult {
        val currentPlan = checkNotNull(plan) { "Installed content is still being scanned" }
        showConfirmation = false
        return withContext(Dispatchers.IO) {
            migration.execute(context, uri, currentPlan)
        }.also { previousResult = it }
    }
}

@Composable
internal fun rememberSettingsMigrationState(
    context: Context,
    refreshKey: Any,
): SettingsMigrationState {
    val migration = remember(context) {
        InstalledContentMigration(context.filesDir.resolve("installed"))
    }
    val state = remember(migration) {
        SettingsMigrationState(context.applicationContext, migration)
    }
    LaunchedEffect(state, refreshKey) {
        state.refreshPlan()
    }
    return state
}

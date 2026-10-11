package com.gamebox.os.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.gamebox.os.storage.ContentMigrationPlan
import com.gamebox.os.storage.ExternalStorageState
import com.gamebox.os.storage.ExternalStorageStatus
import com.gamebox.os.storage.MigrationConfirmationState
import com.gamebox.os.storage.MigrationConfirmationEvaluator

@Composable
fun MigrationConfirmationDialog(
    plan: ContentMigrationPlan,
    storageStatus: ExternalStorageStatus,
    previousResult: com.gamebox.os.storage.ContentMigrationResult? = null,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
 ) {
    val decision = MigrationConfirmationEvaluator.evaluate(storageStatus, plan, confirmed = false, previousResult)
    val canConfirm = decision.state == MigrationConfirmationState.NEEDS_CONFIRMATION && storageStatus.state == ExternalStorageState.AVAILABLE_READ_WRITE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Migrate installed content") },
        text = {
            Column {
                Text(decision.message)
                if (canConfirm) {
                    Spacer(androidx.compose.ui.Modifier.height(12.dp))
                    Text("Each phone copy is removed only after its external copy is verified. Game records and saves are kept.")
                }
            }
        },
        confirmButton = {
            when {
                canConfirm -> BlueprintButton(onClick = onConfirm, modifier = androidx.compose.ui.Modifier.semantics { contentDescription = "Confirm migration" }) { Text("Copy " + plan.totalBytes + " bytes") }
                decision.state == MigrationConfirmationState.RETRYABLE || decision.state == MigrationConfirmationState.BLOCKED_DISCONNECTED -> BlueprintButton(onClick = onRetry) { Text("Reconnect and retry") }
            }
        },
        dismissButton = { BlueprintTextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


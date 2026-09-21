package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameMetadataOverrides
import kotlinx.coroutines.launch

@Composable
internal fun GameMetadataEditorDialog(
    game: Game,
    repository: GameRepository,
    onDismiss: () -> Unit,
) {
    var title by remember(game.id) { mutableStateOf(game.metadataOverrides.title.orEmpty()) }
    var year by remember(game.id) { mutableStateOf(game.metadataOverrides.year?.toString().orEmpty()) }
    var genre by remember(game.id) { mutableStateOf(game.metadataOverrides.genre.orEmpty()) }
    var artworkUrl by remember(game.id) { mutableStateOf(game.metadataOverrides.artworkUrl.orEmpty()) }
    var description by remember(game.id) { mutableStateOf(game.metadataOverrides.description.orEmpty()) }
    var error by remember(game.id) { mutableStateOf<String?>(null) }
    var saving by remember(game.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun persist(overrides: GameMetadataOverrides) {
        if (saving) return
        saving = true
        error = null
        scope.launch {
            runCatching { repository.setMetadataOverrides(game.id, overrides) }
                .onSuccess { onDismiss() }
                .onFailure {
                    error = it.message ?: "Metadata changes could not be saved"
                    saving = false
                }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Edit game metadata") },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState())
            ) {
                Text("Leave a field blank to keep provider metadata. Your corrections remain through catalog refreshes.")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title override") },
                    supportingText = { Text("Provider: ${game.providerTitle}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it.filter(Char::isDigit).take(4) },
                    label = { Text("Year override") },
                    supportingText = { Text("Provider: ${game.providerYear}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Genre override") },
                    supportingText = { Text("Provider: ${game.providerGenre}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                OutlinedTextField(
                    value = artworkUrl,
                    onValueChange = { artworkUrl = it },
                    label = { Text("HTTPS artwork URL") },
                    supportingText = { Text(game.providerArtworkUrl ?: "No provider artwork") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        .semantics { contentDescription = "HTTPS artwork URL override" },
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description override") },
                    supportingText = { Text(game.providerDescription ?: "No provider description") },
                    minLines = 3,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                error?.let {
                    Text(it, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val parsedYear = if (year.isBlank()) null else year.toIntOrNull()
                    if (year.isNotBlank() && parsedYear == null) {
                        error = "Enter a valid four-digit year"
                    } else {
                        persist(
                            GameMetadataOverrides(
                                title = title,
                                year = parsedYear,
                                genre = genre,
                                artworkUrl = artworkUrl,
                                description = description,
                            )
                        )
                    }
                },
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = {
            Column {
                if (!game.metadataOverrides.isEmpty) {
                    OutlinedButton(
                        enabled = !saving,
                        onClick = { persist(GameMetadataOverrides()) },
                    ) { Text("Use provider metadata") }
                }
                OutlinedButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

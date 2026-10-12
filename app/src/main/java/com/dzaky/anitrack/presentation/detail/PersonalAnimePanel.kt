package com.dzaky.anitrack.presentation.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dzaky.anitrack.data.collectionKey
import com.dzaky.anitrack.presentation.components.ChoiceDropdown
import com.dzaky.anitrack.presentation.components.LoadState

@Composable
fun PersonalAnimePanel(viewModel: AnimeDetailViewModel) {
    val personal by viewModel.personal.collectAsStateWithLifecycle()
    val saving by viewModel.savingPersonal.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val memberships by viewModel.memberships.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) { viewModel.personalSaved.collect { editing = false } }
    val saved = (personal as? LoadState.Ready)?.value
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your library", style = MaterialTheme.typography.titleLarge)
        if (saved?.rating != null) Text("Your rating: " + saved.rating + " / 10", color = MaterialTheme.colorScheme.primary)
        if (!saved?.notes.isNullOrBlank()) Text(checkNotNull(saved).notes, style = MaterialTheme.typography.bodyMedium, maxLines = 4,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(modifier = Modifier.weight(1f), enabled = personal is LoadState.Ready, onClick = { editing = true }) { Text("Rating and notes") }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = { choosing = true }) { Text("Collections (" + memberships.size + ")") }
        }
        if (personal is LoadState.Failed) Text("Couldn't load your notes. Reopen this page to try again.", color = MaterialTheme.colorScheme.error)
        Text("Your ratings, notes, and collections stay on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (editing) PersonalDialog(saved?.rating, saved?.notes.orEmpty(), saving, { if (!saving) editing = false }, viewModel::savePersonal)
    if (choosing) {
        var name by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { choosing = false }, title = { Text("Collections") }, text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (collections.isEmpty()) Text("Create your first collection below.")
                collections.forEach { item ->
                    Row(Modifier.fillMaxWidth().clickable(role = Role.Checkbox) { viewModel.setMembership(item.id, item.id !in memberships) },
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(item.id in memberships, onCheckedChange = { viewModel.setMembership(item.id, it) })
                        Text(item.name, Modifier.weight(1f))
                    }
                }
                OutlinedTextField(name, onValueChange = { name = it.take(40); error = null }, label = { Text("New collection name") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = {
                    error = when {
                        name.isBlank() -> "Enter a name."
                        collections.any { it.nameKey == collectionKey(name) } -> "This name is already used."
                        else -> null
                    }
                    if (error == null) { viewModel.createCollection(name); name = "" }
                }) { Text("Create and add") }
            }
        }, confirmButton = { TextButton(onClick = { choosing = false }) { Text("Done") } })
    }
}

@Composable
private fun PersonalDialog(initialRating: Int?, initialNotes: String, saving: Boolean, onDismiss: () -> Unit, onSave: (Int?, String) -> Unit) {
    var rating by rememberSaveable { mutableStateOf(initialRating) }
    var notes by rememberSaveable { mutableStateOf(initialNotes) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Rating and notes") }, text = {
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoiceDropdown("Rating", rating, listOf<Int?>(null) + (1..10), { it?.toString()?.plus(" / 10") ?: "Not rated" }, { rating = it }, enabled = !saving)
            OutlinedTextField(notes, onValueChange = { notes = it.take(5_000) }, enabled = !saving, label = { Text("Personal notes") }, minLines = 3, maxLines = 6,
                supportingText = { Text(notes.length.toString() + " / 5000") })
        }
    }, confirmButton = { TextButton(enabled = !saving, onClick = { onSave(rating, notes) }) { Text(if (saving) "Saving…" else "Save") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } })
}

package com.dzaky.anitrack.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dzaky.anitrack.R
import com.dzaky.anitrack.data.local.WatchEntry
import com.dzaky.anitrack.domain.WatchStatus

@Composable
fun EpisodeControls(entry: WatchEntry, onAdjust: (Int) -> Unit, onSet: (Int) -> Unit, modifier: Modifier = Modifier) {
    var editing by rememberSaveable(entry.animeId) { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onAdjust(-1) }, enabled = entry.watchedEpisodes > 0) {
                Icon(painterResource(R.drawable.ic_minus), "Previous episode", Modifier.size(24.dp))
            }
            TextButton(onClick = { editing = true }, modifier = Modifier.weight(1f)) {
                Text("${entry.watchedEpisodes} / ${entry.totalEpisodes ?: "?"} episodes")
            }
            IconButton(onClick = { onAdjust(1) }, enabled = entry.watchedEpisodes < (entry.totalEpisodes ?: Int.MAX_VALUE)) {
                Icon(painterResource(R.drawable.ic_plus), "Next episode", Modifier.size(24.dp))
            }
        }
        entry.totalEpisodes?.takeIf { it > 0 }?.let { total ->
            LinearProgressIndicator(progress = { entry.watchedEpisodes.toFloat() / total }, modifier = Modifier.fillMaxWidth())
        }
    }
    if (editing) EpisodeDialog(entry, onDismiss = { editing = false }, onSave = { onSet(it); editing = false })
}

@Composable
fun WatchStatusSelector(selected: WatchStatus, onSelect: (WatchStatus) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WatchStatus.entries.forEach { status ->
            FilterChip(selected = status == selected, onClick = { onSelect(status) }, label = { Text(status.label) })
        }
    }
}

@Composable
private fun EpisodeDialog(entry: WatchEntry, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by rememberSaveable(entry.animeId) { mutableStateOf(entry.watchedEpisodes.toString()) }
    val count = text.toIntOrNull()
    val upper = entry.totalEpisodes ?: Int.MAX_VALUE
    val valid = count != null && count in 0..upper
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Episode progress") },
        text = {
            OutlinedTextField(value = text, onValueChange = { value -> if (value.length <= 10 && value.all(Char::isDigit)) text = value },
                label = { Text("Episodes watched") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = text.isNotEmpty() && !valid,
                supportingText = { Text(if (entry.totalEpisodes != null) "Enter 0–${entry.totalEpisodes}" else "Total episodes is unknown") })
        },
        confirmButton = { TextButton(onClick = { count?.let(onSave) }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

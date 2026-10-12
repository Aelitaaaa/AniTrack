package com.dzaky.anitrack.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dzaky.anitrack.BuildConfig
import com.dzaky.anitrack.data.CatalogSource
import com.dzaky.anitrack.data.LibraryLayout
import com.dzaky.anitrack.data.ThemeMode
import com.dzaky.anitrack.presentation.components.ChoiceDropdown
import java.time.LocalDate

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var clearTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        it?.let(viewModel::exportBackup)
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let(viewModel::restoreBackup)
    }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Text("Appearance", style = MaterialTheme.typography.titleLarge)
            ThemeMode.entries.forEach { mode ->
                Row(Modifier.fillMaxWidth().clickable(enabled = !busy, role = Role.RadioButton) { viewModel.setTheme(mode) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButton(selected = mode == preferences.theme, enabled = !busy, onClick = { viewModel.setTheme(mode) })
                    Text(mode.label)
                }
            }
            ChoiceDropdown("Library view", preferences.layout, LibraryLayout.entries, { it.label }, viewModel::setLayout, enabled = !busy)
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item {
            Text("Privacy", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Incognito", style = MaterialTheme.typography.titleMedium)
                    Text("Does not save new searches or viewed titles. Your library still works.", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = preferences.incognito, enabled = !busy, onCheckedChange = viewModel::setIncognito)
            }
        }
        item {
            Text("Catalog source", style = MaterialTheme.typography.titleLarge)
            ChoiceDropdown("Source", preferences.source, CatalogSource.entries, { it.label }, viewModel::setSource, enabled = !busy)
            Text("Automatic switches sources when a request is slow. Changes apply to new requests; saved data stays available.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Backup and restore", style = MaterialTheme.typography.titleLarge)
                Text("Save your profile, badges, library, collections, notes, settings, and histories to a JSON file.",
                    style = MaterialTheme.typography.bodyMedium)
                Text("Restore merges lists and keeps newer watch progress. Your existing profile is kept when importing a different profile.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(enabled = !busy, onClick = { export.launch("AniTrack-" + LocalDate.now() + ".anitrack.json") }) { Text("Export backup") }
                    TextButton(enabled = !busy, onClick = { restore.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }) { Text("Restore backup") }
                }
                if (busy) CircularProgressIndicator()
            }
        }
        item {
            Text("History", style = MaterialTheme.typography.titleLarge)
            TextButton(enabled = !busy, onClick = { clearTarget = "search history" }) { Text("Clear search history") }
            TextButton(enabled = !busy, onClick = { clearTarget = "recently viewed" }) { Text("Clear recently viewed") }
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("About AniTrack", style = MaterialTheme.typography.titleLarge)
                Text("A place for the anime you find, love, and watch.", style = MaterialTheme.typography.bodyLarge)
                Text("Anime information is provided by Jikan / MyAnimeList and AniList. Your profile, badges, library, collections, and personal notes are stored on this device.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Version " + BuildConfig.VERSION_NAME, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    clearTarget?.let { target ->
        AlertDialog(onDismissRequest = { clearTarget = null }, title = { Text("Clear " + target + "?") },
            text = { Text("This removes " + target + " from this device.") },
            confirmButton = { TextButton(onClick = {
                if (target == "search history") viewModel.clearSearchHistory() else viewModel.clearRecentlyViewed()
                clearTarget = null
            }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { clearTarget = null }) { Text("Cancel") } })
    }
}

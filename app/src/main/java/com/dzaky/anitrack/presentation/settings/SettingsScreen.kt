package com.dzaky.anitrack.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.dzaky.anitrack.data.ThemeMode

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    var clearTarget by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Text("Appearance", style = MaterialTheme.typography.titleLarge)
            ThemeMode.entries.forEach { mode ->
                Row(Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { viewModel.setTheme(mode) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButton(selected = mode == theme, onClick = { viewModel.setTheme(mode) })
                    Text(mode.label)
                }
            }
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item {
            Text("History", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { clearTarget = "search history" }) { Text("Clear search history") }
            TextButton(onClick = { clearTarget = "recently viewed" }) { Text("Clear recently viewed") }
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("About AniTrack", style = MaterialTheme.typography.titleLarge)
                Text("A place for the anime you find, love, and watch.", style = MaterialTheme.typography.bodyLarge)
                Text("Anime information is provided by Jikan / MyAnimeList and AniList. Your profile, badges, favorites, and watch progress are stored on this device.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    clearTarget?.let { target ->
        AlertDialog(onDismissRequest = { clearTarget = null }, title = { Text("Clear $target?") },
            text = { Text("This removes $target from this device.") },
            confirmButton = { TextButton(onClick = {
                if (target == "search history") viewModel.clearSearchHistory() else viewModel.clearRecentlyViewed()
                clearTarget = null
            }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { clearTarget = null }) { Text("Cancel") } })
    }
}

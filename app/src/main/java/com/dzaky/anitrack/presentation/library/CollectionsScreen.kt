package com.dzaky.anitrack.presentation.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dzaky.anitrack.data.LibraryLayout
import com.dzaky.anitrack.data.collectionKey
import com.dzaky.anitrack.data.collectionName
import com.dzaky.anitrack.domain.LibrarySort
import com.dzaky.anitrack.domain.filterLibrary
import com.dzaky.anitrack.presentation.components.AnimeCard
import com.dzaky.anitrack.presentation.components.AnimeResultRow
import com.dzaky.anitrack.presentation.components.LibraryToolbar
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.components.MessageContent
import com.dzaky.anitrack.presentation.components.PosterSkeletonRow

@Composable
fun CollectionsScreen(viewModel: CollectionsViewModel, onAnimeClick: (Int) -> Unit, layout: LibraryLayout = LibraryLayout.List) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable(selected) { mutableStateOf("") }
    var sortName by rememberSaveable(selected) { mutableStateOf(LibrarySort.Recent.name) }
    val sort = LibrarySort.valueOf(sortName)
    when (val current = state) {
        LoadState.Loading -> PosterSkeletonRow(Modifier.padding(top = 24.dp))
        is LoadState.Failed -> MessageContent("Couldn't open collections", current.message, Modifier.fillMaxSize())
        is LoadState.Ready -> {
            val data = current.value
            val collection = data.collections.firstOrNull { it.id == selected }
            Column(Modifier.fillMaxSize()) {
                if (collection == null) {
                    TextButton(onClick = { adding = true }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("New collection") }
                    if (data.collections.isEmpty()) MessageContent("Make room for your favorites",
                        "Create a collection, then add anime from its detail page.", Modifier.weight(1f))
                    else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(data.collections, key = { it.id }) { item ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(Modifier.fillMaxWidth().clickable { selected = item.id }) {
                                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                                        Text(data.members.count { it.collectionId == item.id }.toString() + " anime", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Row {
                                        TextButton(onClick = { renaming = item.id }) { Text("Rename") }
                                        TextButton(onClick = { deleting = item.id }) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(Modifier.padding(horizontal = 12.dp)) {
                        TextButton(onClick = { selected = null }) { Text("All collections") }
                        TextButton(onClick = { renaming = collection.id }) { Text("Rename") }
                    }
                    Text(collection.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
                    LibraryToolbar(query, { query = it }, sort, { sortName = it.name })
                    val visible = filterLibrary(data.members.filter { it.collectionId == collection.id }, query, sort,
                        { it.title }, { it.animeId }, { it.score }, { it.addedAt })
                    if (visible.isEmpty()) MessageContent("No anime here yet", "Add titles from their detail page or adjust your filter.", Modifier.weight(1f))
                    else if (layout == LibraryLayout.Grid) LazyVerticalGrid(GridCells.Adaptive(136.dp), Modifier.weight(1f),
                        contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        items(visible, key = { it.animeId }) { member ->
                            Column {
                                AnimeCard(member.toAnime(), { onAnimeClick(member.animeId) }, Modifier.fillMaxWidth())
                                TextButton(onClick = { viewModel.remove(collection.id, member.animeId) }) { Text("Remove") }
                            }
                        }
                    } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(visible, key = { it.animeId }) { member ->
                            AnimeResultRow(member.toAnime(), { onAnimeClick(member.animeId) }, trailing = {
                                TextButton(onClick = { viewModel.remove(collection.id, member.animeId) }) { Text("Remove") }
                            })
                        }
                    }
                }
            }
            val rename = data.collections.firstOrNull { it.id == renaming }
            if (adding || rename != null) CollectionNameDialog(rename?.name.orEmpty(), data.collections.filter { it.id != rename?.id }.map { it.nameKey }.toSet(),
                onDismiss = { adding = false; renaming = null }, onSave = { name ->
                    if (rename == null) viewModel.create(name) else viewModel.rename(rename.id, name)
                    adding = false; renaming = null
                })
            data.collections.firstOrNull { it.id == deleting }?.let { item ->
                AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete " + item.name + "?") },
                    text = { Text("This removes the collection and its memberships. Your favorites and watch progress are kept.") },
                    confirmButton = { TextButton(onClick = { viewModel.delete(item.id); deleting = null; if (selected == item.id) selected = null }) { Text("Delete") } },
                    dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
            }
        }
    }
}

@Composable
private fun CollectionNameDialog(initial: String, existing: Set<String>, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable(initial) { mutableStateOf(initial) }
    var error by rememberSaveable(initial) { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial.isEmpty()) "New collection" else "Rename collection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, onValueChange = { name = it.take(40); error = null }, singleLine = true, label = { Text("Collection name") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(onClick = {
            val normalized = collectionName(name)
            error = when { normalized.isEmpty() -> "Enter a name."; collectionKey(normalized) in existing -> "This name is already used."; else -> null }
            if (error == null) onSave(normalized)
        }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

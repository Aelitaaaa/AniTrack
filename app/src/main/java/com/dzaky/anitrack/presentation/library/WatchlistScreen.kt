package com.dzaky.anitrack.presentation.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dzaky.anitrack.R
import com.dzaky.anitrack.data.local.WatchEntry
import com.dzaky.anitrack.domain.WatchStatus
import com.dzaky.anitrack.presentation.components.AnimePoster
import com.dzaky.anitrack.presentation.components.EpisodeControls
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.components.MessageContent
import com.dzaky.anitrack.presentation.components.PosterSkeletonRow
import com.dzaky.anitrack.presentation.components.WatchStatusSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(viewModel: LibraryViewModel, onAnimeClick: (Int) -> Unit, onDiscover: () -> Unit) {
    val state by viewModel.watchlist.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize()) {
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") }) }
            items(WatchStatus.entries, key = { it.name }) { status ->
                FilterChip(selected = filter == status.name, onClick = { filter = status.name }, label = { Text(status.label) })
            }
        }
        when (val current = state) {
            LoadState.Loading -> PosterSkeletonRow(Modifier.padding(top = 24.dp))
            is LoadState.Failed -> MessageContent("Couldn't open watchlist", current.message, Modifier.weight(1f), onAction = viewModel::retry)
            is LoadState.Ready -> {
                val visible = current.value.filter { filter == null || it.status.name == filter }
                if (visible.isEmpty()) {
                    MessageContent(if (filter == null) "Your next episode starts here" else "Nothing in this list yet",
                        if (filter == null) "Add anime from its detail page, then track each episode as you watch." else "Anime with this status will appear here.",
                        Modifier.weight(1f), actionLabel = "Discover anime", onAction = onDiscover)
                } else {
                    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
                        items(visible, key = { it.animeId }) { entry ->
                            WatchlistRow(entry, onOpen = { onAnimeClick(entry.animeId) }, onEdit = { editingId = entry.animeId },
                                onAdjust = { viewModel.adjustEpisodes(entry.animeId, it) }, onSet = { viewModel.setEpisodes(entry.animeId, it) })
                            HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
                current.value.firstOrNull { it.animeId == editingId }?.let { entry ->
                    ModalBottomSheet(onDismissRequest = { editingId = null }) {
                        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(entry.title, style = MaterialTheme.typography.titleLarge)
                            WatchStatusSelector(entry.status, onSelect = { viewModel.setStatus(entry.animeId, it) })
                            EpisodeControls(entry, onAdjust = { viewModel.adjustEpisodes(entry.animeId, it) }, onSet = { viewModel.setEpisodes(entry.animeId, it) })
                            TextButton(onClick = { viewModel.removeWatchEntry(entry.animeId); editingId = null }) { Text("Remove from watchlist") }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchlistRow(entry: WatchEntry, onOpen: () -> Unit, onEdit: () -> Unit, onAdjust: (Int) -> Unit, onSet: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnimePoster(entry.posterUrl, entry.title, Modifier.width(64.dp).height(92.dp).clickable(onClick = onOpen))
            Column(Modifier.weight(1f).clickable(onClick = onOpen), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(entry.status.label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onEdit) { Icon(painterResource(R.drawable.ic_edit), "Edit ${entry.title}", Modifier.size(24.dp)) }
        }
        Spacer(Modifier.height(12.dp))
        EpisodeControls(entry, onAdjust, onSet)
    }
}

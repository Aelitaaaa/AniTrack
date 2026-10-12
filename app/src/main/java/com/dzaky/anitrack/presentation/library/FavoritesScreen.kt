package com.dzaky.anitrack.presentation.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.dzaky.anitrack.data.LibraryLayout
import com.dzaky.anitrack.domain.LibrarySort
import com.dzaky.anitrack.domain.filterLibrary
import com.dzaky.anitrack.presentation.components.LibraryToolbar
import com.dzaky.anitrack.presentation.components.AnimeCard
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dzaky.anitrack.R
import com.dzaky.anitrack.presentation.components.AnimeResultRow
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.components.MessageContent
import com.dzaky.anitrack.presentation.components.PosterSkeletonRow

@Composable
fun FavoritesScreen(viewModel: LibraryViewModel, onAnimeClick: (Int) -> Unit, onDiscover: () -> Unit,
    onCollections: () -> Unit = {}, layout: LibraryLayout = LibraryLayout.List) {
    val state by viewModel.favorites.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var sortName by rememberSaveable { mutableStateOf(LibrarySort.Recent.name) }
    val sort = LibrarySort.valueOf(sortName)
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onCollections, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Collections") }
        LibraryToolbar(query, { query = it }, sort, { sortName = it.name })
        when (val current = state) {
            LoadState.Loading -> PosterSkeletonRow(Modifier.padding(top = 24.dp))
            is LoadState.Failed -> MessageContent("Couldn't open favorites", current.message, Modifier.weight(1f), onAction = viewModel::retry)
            is LoadState.Ready -> {
                val visible = filterLibrary(current.value, query, sort, { it.title }, { it.animeId }, { it.score }, { it.addedAt })
                if (visible.isEmpty()) {
                    MessageContent(if (current.value.isEmpty()) "Keep your favorites close" else "No matching favorites",
                        if (current.value.isEmpty()) "Tap the heart on an anime to save it here. Your favorites stay available offline." else "Try a shorter title.",
                        Modifier.weight(1f), actionLabel = if (current.value.isEmpty()) "Discover anime" else "Clear filter",
                        onAction = { if (current.value.isEmpty()) onDiscover() else query = "" })
                } else if (layout == LibraryLayout.Grid) {
                    LazyVerticalGrid(GridCells.Adaptive(136.dp), Modifier.weight(1f), contentPadding = PaddingValues(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        items(visible, key = { it.animeId }) { favorite ->
                            Column {
                                AnimeCard(favorite.toAnime(), { onAnimeClick(favorite.animeId) }, Modifier.fillMaxWidth())
                                TextButton(onClick = { viewModel.removeFavorite(favorite.animeId) }) { Text("Remove favorite") }
                            }
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(visible, key = { it.animeId }) { favorite ->
                            AnimeResultRow(favorite.toAnime(), onClick = { onAnimeClick(favorite.animeId) }, trailing = {
                                IconButton(onClick = { viewModel.removeFavorite(favorite.animeId) }) {
                                    Icon(painterResource(R.drawable.ic_heart), "Remove " + favorite.title + " from favorites", Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            })
                            HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

package com.dzaky.anitrack.presentation.library

import androidx.compose.foundation.layout.PaddingValues
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
fun FavoritesScreen(viewModel: LibraryViewModel, onAnimeClick: (Int) -> Unit, onDiscover: () -> Unit) {
    val state by viewModel.favorites.collectAsStateWithLifecycle()
    when (val current = state) {
        LoadState.Loading -> PosterSkeletonRow(Modifier.padding(top = 24.dp))
        is LoadState.Failed -> MessageContent("Couldn't open favorites", current.message, Modifier.fillMaxSize(), onAction = viewModel::retry)
        is LoadState.Ready -> {
            if (current.value.isEmpty()) {
                MessageContent("Keep your favorites close", "Tap the heart on an anime to save it here. Your favorites stay available offline.",
                    Modifier.fillMaxSize(), actionLabel = "Discover anime", onAction = onDiscover)
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(current.value, key = { it.animeId }) { favorite ->
                        AnimeResultRow(favorite.toAnime(), onClick = { onAnimeClick(favorite.animeId) }, trailing = {
                            IconButton(onClick = { viewModel.removeFavorite(favorite.animeId) }) {
                                Icon(painterResource(R.drawable.ic_heart), "Remove ${favorite.title} from favorites", Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        })
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

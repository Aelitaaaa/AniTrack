package com.dzaky.anitrack.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dzaky.anitrack.presentation.components.AnimeCard
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.components.MessageContent
import com.dzaky.anitrack.presentation.components.PosterSkeletonRow
import com.dzaky.anitrack.presentation.components.SectionHeading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onAnimeClick: (Int) -> Unit) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = { viewModel.refresh() }) {
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item(key = "intro") {
                Text("Find your next watch.", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                Text("Discover anime. Keep your place.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp))
            }
            if (recent.isNotEmpty()) item(key = "recent") {
                SectionHeading("Recently viewed", Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(16.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(recent, key = { it.animeId }) { entry -> AnimeCard(entry.toAnime(), onClick = { onAnimeClick(entry.animeId) }) }
                }
            }
            items(sections, key = { it.section.name }) { row ->
                SectionHeading(row.section.title, Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(16.dp))
                when (val content = row.content) {
                    LoadState.Loading -> PosterSkeletonRow()
                    is LoadState.Failed -> MessageContent("Couldn't load this section", content.message, onAction = { viewModel.retry(row.section) })
                    is LoadState.Ready -> {
                        if (content.value.isEmpty()) {
                            Text("No titles available right now.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
                        } else {
                            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(content.value, key = { it.id }) { anime -> AnimeCard(anime, onClick = { onAnimeClick(anime.id) }) }
                            }
                        }
                    }
                }
            }
            item(key = "attribution") {
                Text("Anime information from MyAnimeList via Jikan", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}

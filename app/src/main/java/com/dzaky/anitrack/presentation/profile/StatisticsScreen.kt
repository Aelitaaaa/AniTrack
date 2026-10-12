package com.dzaky.anitrack.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.components.MessageContent
import com.dzaky.anitrack.presentation.components.PosterSkeletonRow
import com.dzaky.anitrack.presentation.components.scoreText

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel, onAnimeClick: (Int) -> Unit, onDiscover: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        LoadState.Loading -> PosterSkeletonRow(Modifier.padding(top = 24.dp))
        is LoadState.Failed -> MessageContent("Couldn't read statistics", current.message, Modifier.fillMaxSize())
        is LoadState.Ready -> {
            val data = current.value
            val s = data.statistics
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatisticCard(s.episodes.toString(), "Recorded episodes", Modifier.weight(1f))
                        StatisticCard(s.titles.toString(), "Watchlist titles", Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatisticCard(data.favorites.toString(), "Favorites", Modifier.weight(1f))
                        StatisticCard(data.collections.toString(), "Collections", Modifier.weight(1f))
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Watch progress", style = MaterialTheme.typography.titleLarge)
                        StatisticsBar("Completed", s.completed, s.titles)
                        StatisticsBar("Watching", s.watching, s.titles)
                        StatisticsBar("Plan to watch", s.planned, s.titles)
                        if (s.knownTotal > 0) {
                            Text(s.knownWatched.toString() + " / " + s.knownTotal + " episodes with a known total")
                            LinearProgressIndicator(progress = { (s.knownWatched.toFloat() / s.knownTotal).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        }
                        if (s.unknownTotals > 0) Text(s.unknownTotals.toString() + " titles have no known episode total.", style = MaterialTheme.typography.bodySmall)
                        if (s.titles == 0) TextButton(onClick = onDiscover) { Text("Discover anime") }
                    }
                }
                if (s.formats.isNotEmpty()) item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Formats in your watchlist", style = MaterialTheme.typography.titleLarge)
                        s.formats.forEach { (format, count) -> StatisticsBar(format, count, s.titles) }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Personal ratings", style = MaterialTheme.typography.titleLarge)
                        Text(if (s.averageRating == null) "Rate anime on their detail pages." else
                            scoreText(s.averageRating) + " / 10 average across " + s.ratings + " titles")
                        data.rated.take(5).forEach { rated ->
                            Text(rated.title + " · " + rated.rating + " / 10",
                                modifier = Modifier.fillMaxWidth().clickable { onAnimeClick(rated.animeId) }.padding(vertical = 8.dp))
                        }
                    }
                }
                item { Text("These statistics use your saved watch progress and personal ratings.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun StatisticCard(value: String, label: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StatisticsBar(label: String, count: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(count.toString()) }
        LinearProgressIndicator(progress = { count.toFloat() / total.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
    }
}

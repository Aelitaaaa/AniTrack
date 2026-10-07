package com.dzaky.anitrack.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dzaky.anitrack.R
import com.dzaky.anitrack.domain.Anime
import com.dzaky.anitrack.presentation.components.AnimePoster
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.components.MessageContent
import com.dzaky.anitrack.presentation.components.PosterSkeletonRow
import com.dzaky.anitrack.presentation.components.scoreText

@Composable
fun AnimeDetailScreen(viewModel: AnimeDetailViewModel, onAnimeClick: (Int) -> Unit, onTrailer: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val watchEntry by viewModel.watchEntry.collectAsStateWithLifecycle()
    when (val current = state) {
        LoadState.Loading -> Column(Modifier.padding(top = 32.dp)) { PosterSkeletonRow() }
        is LoadState.Failed -> MessageContent("Couldn't load anime", current.message, Modifier.fillMaxSize(), onAction = { viewModel.load(refresh = true) })
        is LoadState.Ready -> {
            val anime = current.value.anime
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item(key = "header") { DetailHeader(anime) }
                if (current.value.isOffline) item(key = "offline") {
                    MessageContent("Viewing saved details", "You're offline or the service is unavailable. Your library still works.",
                        actionLabel = "Refresh details", onAction = { viewModel.load(refresh = true) })
                }
                item(key = "actions") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (watchEntry == null) Button(onClick = viewModel::addToWatchlist, modifier = Modifier.weight(1f)) {
                            Icon(painterResource(R.drawable.ic_plus), null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add to watchlist")
                        } else Text("In your watchlist", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                        IconToggleButton(checked = isFavorite, onCheckedChange = { viewModel.toggleFavorite() }) {
                            Icon(painterResource(if (isFavorite) R.drawable.ic_heart else R.drawable.ic_heart_outline),
                                if (isFavorite) "Remove from favorites" else "Add to favorites", Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                watchEntry?.let { entry -> item(key = "progress") {
                    WatchProgressPanel(entry, viewModel::setStatus, viewModel::adjustEpisodes, viewModel::setEpisodes, viewModel::removeFromWatchlist)
                } }
                if (anime.genres.isNotEmpty()) item(key = "genres") { GenreLabels(anime.genres) }
                anime.synopsis?.let { synopsis -> item(key = "synopsis") { SynopsisSection(anime.id, synopsis) } }
                item(key = "information") { InformationSection(anime) }
                anime.trailerUrl?.let { url -> item(key = "trailer") {
                    Button(onClick = { onTrailer(url) }, modifier = Modifier.padding(horizontal = 20.dp)) {
                        Icon(painterResource(R.drawable.ic_play), null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.watch_trailer))
                    }
                } }
                item(key = "characters") { CharactersSection(characters, viewModel::retryCharacters) }
                if (anime.relations.isNotEmpty()) item(key = "related") { RelatedSection(anime.relations, onAnimeClick) }
                item(key = "source") {
                    Text("Information from Jikan / MyAnimeList and AniList", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(anime: Anime) {
    Box(Modifier.fillMaxWidth()) {
        AnimePoster(anime.posterUrl, anime.title, Modifier.matchParentSize().alpha(0.08f), decorative = true)
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                AnimePoster(anime.posterUrl, anime.title, Modifier.width(124.dp).height(186.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(listOfNotNull(anime.type, anime.year?.toString()).joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    anime.score?.let {
                        Text(scoreText(it), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Catalog score / 10", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    anime.rank?.let { Text("Ranked #$it", style = MaterialTheme.typography.bodyMedium) }
                    anime.episodes?.let { Text("$it episodes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Text(anime.title, style = MaterialTheme.typography.headlineMedium)
            anime.originalTitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            anime.japaneseTitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun SynopsisSection(id: Int, synopsis: String) {
    var expanded by rememberSaveable(id) { mutableStateOf(false) }
    var overflowing by rememberSaveable(id) { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Synopsis", style = MaterialTheme.typography.titleLarge)
        Text(synopsis, style = MaterialTheme.typography.bodyLarge, maxLines = if (expanded) Int.MAX_VALUE else 6,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, onTextLayout = { if (!expanded) overflowing = it.hasVisualOverflow })
        if (overflowing || expanded) androidx.compose.material3.TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Show less" else "Read more")
        }
    }
}

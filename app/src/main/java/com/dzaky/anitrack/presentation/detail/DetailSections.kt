package com.dzaky.anitrack.presentation.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dzaky.anitrack.R
import com.dzaky.anitrack.data.local.WatchEntry
import com.dzaky.anitrack.domain.Anime
import com.dzaky.anitrack.domain.AnimeCharacter
import com.dzaky.anitrack.domain.RelatedAnime
import com.dzaky.anitrack.domain.WatchStatus
import com.dzaky.anitrack.presentation.components.AnimePoster
import com.dzaky.anitrack.presentation.components.EpisodeControls
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.components.MessageContent
import com.dzaky.anitrack.presentation.components.PosterSkeletonRow
import com.dzaky.anitrack.presentation.components.SectionHeading
import com.dzaky.anitrack.presentation.components.WatchStatusSelector
import java.text.NumberFormat

@Composable
internal fun GenreLabels(genres: List<String>) {
    FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        genres.forEach { genre ->
            Text(genre, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp))
        }
    }
}

@Composable
internal fun WatchProgressPanel(entry: WatchEntry, onStatus: (WatchStatus) -> Unit, onAdjust: (Int) -> Unit, onSet: (Int) -> Unit, onRemove: () -> Unit) {
    Surface(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your watch progress", style = MaterialTheme.typography.titleMedium)
            WatchStatusSelector(entry.status, onStatus)
            EpisodeControls(entry, onAdjust, onSet)
            TextButton(onClick = onRemove) { Text("Remove from watchlist") }
        }
    }
}

@Composable
internal fun InformationSection(anime: Anime) {
    val facts = buildList {
        anime.status?.let { add("Status" to it) }
        anime.aired?.let { add("Aired" to it) }
        anime.duration?.let { add("Duration" to it) }
        anime.season?.let { add("Season" to listOfNotNull(it.replaceFirstChar(Char::uppercase), anime.year?.toString()).joinToString(" ")) }
        anime.rating?.let { add("Rating" to it) }
        anime.popularity?.let { add("Popularity" to "#$it") }
        anime.members?.let { add("Members" to NumberFormat.getIntegerInstance().format(it)) }
        if (anime.studios.isNotEmpty()) add("Studios" to anime.studios.joinToString(", "))
        if (anime.producers.isNotEmpty()) add("Producers" to anime.producers.joinToString(", "))
    }
    if (facts.isEmpty()) return
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeading("Information")
        facts.forEach { (label, value) ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(label, Modifier.width(92.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
internal fun CharactersSection(state: LoadState<List<AnimeCharacter>>, onRetry: () -> Unit) {
    if (state is LoadState.Ready && state.value.isEmpty()) return
    SectionHeading("Characters", Modifier.padding(horizontal = 20.dp))
    Spacer(Modifier.height(16.dp))
    when (state) {
        LoadState.Loading -> PosterSkeletonRow()
        is LoadState.Failed -> MessageContent("Characters are unavailable", state.message, onAction = onRetry)
        is LoadState.Ready -> LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(state.value, key = { it.id }) { character ->
                Column(Modifier.width(96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimePoster(character.imageUrl, character.name, Modifier.fillMaxWidth().height(128.dp))
                    Text(character.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (character.role.isNotBlank()) Text(character.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun RelatedSection(relations: List<RelatedAnime>, onAnimeClick: (Int) -> Unit) {
    Column {
        SectionHeading("Related anime", Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        relations.take(12).forEach { related ->
            Row(Modifier.fillMaxWidth().clickable { onAnimeClick(related.id) }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(related.title, style = MaterialTheme.typography.titleMedium)
                    Text(related.relation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(painterResource(R.drawable.ic_chevron), null, Modifier.size(24.dp))
            }
        }
    }
}

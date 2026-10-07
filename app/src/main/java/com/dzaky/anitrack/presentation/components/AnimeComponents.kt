package com.dzaky.anitrack.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.dzaky.anitrack.R
import com.dzaky.anitrack.domain.Anime
import java.util.Locale

@Composable
fun AnimePoster(url: String?, title: String, modifier: Modifier = Modifier, decorative: Boolean = false) {
    val context = LocalContext.current
    val request = remember(url, context) { ImageRequest.Builder(context).data(url).crossfade(true).build() }
    AsyncImage(
        model = request,
        contentDescription = if (decorative) null else "Poster for $title",
        placeholder = painterResource(R.drawable.poster_placeholder),
        error = painterResource(R.drawable.poster_placeholder),
        fallback = painterResource(R.drawable.poster_placeholder),
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

@Composable
fun AnimeCard(anime: Anime, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.width(136.dp).clickable(role = Role.Button, onClickLabel = "Open ${anime.title}", onClick = onClick)) {
        AnimePoster(anime.posterUrl, anime.title, Modifier.fillMaxWidth().aspectRatio(2f / 3f), decorative = true)
        Spacer(Modifier.height(10.dp))
        Text(anime.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
        anime.score?.let {
            Spacer(Modifier.height(3.dp))
            Text("${scoreText(it)} / 10", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AnimeResultRow(anime: Anime, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Open ${anime.title}", onClick = onClick)
        .padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AnimePoster(anime.posterUrl, anime.title, Modifier.width(64.dp).height(92.dp), decorative = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(anime.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val metadata = listOfNotNull(anime.type, anime.year?.toString()).joinToString(" · ")
            if (metadata.isNotEmpty()) Text(metadata, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            anime.score?.let { Text("${scoreText(it)} / 10", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
        }
        trailing?.invoke()
    }
}

fun scoreText(score: Double): String = String.format(Locale.US, "%.2f", score)

@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier = modifier, style = MaterialTheme.typography.titleLarge)
}

package com.dzaky.anitrack.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dzaky.anitrack.R

@Composable
fun MessageContent(title: String, message: String, modifier: Modifier = Modifier, actionLabel: String = stringResource(R.string.retry), onAction: (() -> Unit)? = null) {
    Box(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (onAction != null) OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun PosterSkeletonRow(modifier: Modifier = Modifier) {
    val animation = rememberInfiniteTransition(label = "Loading posters")
    val alpha by animation.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "Poster pulse")
    Row(modifier.padding(horizontal = 20.dp).clearAndSetSemantics { contentDescription = "Loading anime" },
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(3) {
            Column(Modifier.width(136.dp).alpha(alpha)) {
                Box(Modifier.fillMaxWidth().height(204.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh))
                Spacer(Modifier.height(12.dp))
                Box(Modifier.width(108.dp).height(14.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width(76.dp).height(14.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh))
            }
        }
    }
}

package com.dzaky.anitrack.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dzaky.anitrack.R
import com.dzaky.anitrack.domain.AnimeBadge
import com.dzaky.anitrack.domain.LocalProfile
import com.dzaky.anitrack.domain.ProfileInput
import com.dzaky.anitrack.domain.ProfileStats
import java.text.DateFormat
import java.util.Date

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) { viewModel.saved.collect { editing = false } }
    val profile = state.profile
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (state.isLoading) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (profile == null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(painterResource(R.drawable.ic_profile), null, Modifier.size(40.dp))
                        Text("Make this space yours.", style = MaterialTheme.typography.headlineSmall)
                        Text("Create a local profile, track your progress, and earn anime badges.")
                        Button(onClick = { editing = true }) { Text("Create profile") }
                        Text("Your profile stays on this device. No email or password is needed.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                                Text(profile.displayName.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "A",
                                    color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(profile.displayName, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("@${profile.username}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (profile.bio.isNotBlank()) Text(profile.bio, style = MaterialTheme.typography.bodyMedium)
                        val rank = AnimeBadge.entries.lastOrNull { it in state.earned } ?: AnimeBadge.PendatangBaru
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(painterResource(R.drawable.ic_badge), null, Modifier.size(24.dp))
                            Text(rank.title, style = MaterialTheme.typography.titleMedium)
                        }
                        Text("Joined ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(profile.createdAt))}", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { editing = true }) { Text("Edit profile") }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(state.stats.episodes.toString(), "Episodes", Modifier.weight(1f))
                    StatCard(state.stats.completed.toString(), "Finished", Modifier.weight(1f))
                    StatCard(state.stats.favorites.toString(), "Favorites", Modifier.weight(1f))
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Anime badges", style = MaterialTheme.typography.titleLarge)
                Text(if (profile == null) "Create a profile to start earning badges." else "${state.earned.size} of ${AnimeBadge.entries.size} unlocked. Earned badges stay with your profile.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(AnimeBadge.entries, key = { it.name }) { badge -> BadgeCard(badge, badge in state.earned, state.stats, profile != null) }
        item {
            Text("Progress comes from the episodes and completed anime in your watchlist. Your profile and badges are stored on this device.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (editing) ProfileDialog(profile, saving, onDismiss = { if (!saving) editing = false }, onSave = viewModel::save)
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BadgeCard(badge: AnimeBadge, earned: Boolean, stats: ProfileStats, hasProfile: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = if (earned) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.ic_badge), null, Modifier.size(28.dp),
                    tint = if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f)) {
                    Text(badge.title, style = MaterialTheme.typography.titleMedium)
                    Text(if (earned) "Unlocked" else "Locked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(badge.requirement, style = MaterialTheme.typography.bodyMedium)
            if (!earned && hasProfile && badge != AnimeBadge.PendatangBaru) {
                LinearProgressIndicator(progress = { badge.progress(stats) }, modifier = Modifier.fillMaxWidth())
                Text(badge.progressLabel(stats), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProfileDialog(profile: LocalProfile?, saving: Boolean, onDismiss: () -> Unit, onSave: (ProfileInput) -> Unit) {
    var name by rememberSaveable { mutableStateOf(profile?.displayName.orEmpty()) }
    var username by rememberSaveable { mutableStateOf(profile?.username.orEmpty()) }
    var bio by rememberSaveable { mutableStateOf(profile?.bio.orEmpty()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (profile == null) "Create profile" else "Edit profile") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, onValueChange = { name = it; error = null }, label = { Text("Name") }, singleLine = true, enabled = !saving,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
            OutlinedTextField(username, onValueChange = { username = it; error = null }, label = { Text("Username") }, singleLine = true, enabled = !saving,
                prefix = { Text("@") })
            OutlinedTextField(bio, onValueChange = { bio = it; error = null }, label = { Text("Bio (optional)") }, maxLines = 3, enabled = !saving,
                supportingText = { Text("${bio.length} / 120") })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }, confirmButton = {
        TextButton(enabled = !saving, onClick = {
            val input = ProfileInput(name, username, bio)
            error = input.validationError()
            if (error == null) onSave(input)
        }) { Text(if (saving) "Saving…" else "Save") }
    }, dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } })
}

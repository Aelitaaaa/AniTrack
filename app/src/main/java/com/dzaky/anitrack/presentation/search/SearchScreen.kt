package com.dzaky.anitrack.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dzaky.anitrack.R
import com.dzaky.anitrack.presentation.components.AnimeResultRow
import com.dzaky.anitrack.presentation.components.MessageContent
import com.dzaky.anitrack.presentation.components.PosterSkeletonRow

@Composable
fun SearchScreen(viewModel: SearchViewModel, onAnimeClick: (Int) -> Unit) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(value = query, onValueChange = viewModel::updateQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            placeholder = { Text("Search anime") }, singleLine = true,
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, Modifier.size(24.dp)) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { viewModel.updateQuery("") }) {
                    Icon(painterResource(R.drawable.ic_close), "Clear search", Modifier.size(24.dp))
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        )
        when (val current = state) {
            SearchUiState.Idle -> {
                if (history.isEmpty()) {
                    MessageContent("What are you looking for?", "Search by an anime title, then open a result to see more.", Modifier.weight(1f))
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        item { Text("Recent searches", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(20.dp)) }
                        items(history, key = { it.query }) { item ->
                            Row(Modifier.fillMaxWidth().clickable { viewModel.updateQuery(item.query) }.padding(start = 20.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(item.query, modifier = Modifier.weight(1f).padding(vertical = 16.dp))
                                IconButton(onClick = { viewModel.removeHistory(item.query) }) {
                                    Icon(painterResource(R.drawable.ic_close), "Remove ${item.query} from history", Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
            SearchUiState.Loading -> Column(Modifier.padding(top = 24.dp)) { PosterSkeletonRow() }
            is SearchUiState.Failed -> MessageContent("Couldn't search anime", current.message, Modifier.weight(1f), onAction = viewModel::retry)
            is SearchUiState.Results -> {
                if (current.anime.isEmpty()) {
                    MessageContent("No matches for “${current.query}”", "Try a different spelling or a shorter title.", Modifier.weight(1f))
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(current.anime, key = { it.id }) { anime ->
                            AnimeResultRow(anime, onClick = { keyboard?.hide(); onAnimeClick(anime.id) })
                            HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        if (current.hasNext) item(key = "pagination") {
                            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                when (val append = current.append) {
                                    AppendState.Loading -> CircularProgressIndicator(Modifier.size(28.dp))
                                    is AppendState.Failed -> MessageContent("Couldn't load more", append.message, onAction = viewModel::loadMore)
                                    AppendState.Idle -> Button(onClick = viewModel::loadMore) { Text("Load more") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

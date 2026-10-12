package com.dzaky.anitrack.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dzaky.anitrack.domain.AiringStatus
import com.dzaky.anitrack.domain.AnimeFormat
import com.dzaky.anitrack.domain.AnimeGenre
import com.dzaky.anitrack.domain.CatalogSort
import com.dzaky.anitrack.domain.SearchFilters
import com.dzaky.anitrack.presentation.components.ChoiceDropdown

@Composable
fun SearchFilterDialog(selected: SearchFilters, onDismiss: () -> Unit, onApply: (SearchFilters) -> Unit) {
    var format by remember { mutableStateOf(selected.format) }
    var status by remember { mutableStateOf(selected.status) }
    var genre by remember { mutableStateOf(selected.genre) }
    var sort by remember { mutableStateOf(selected.sort) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Search filters") }, text = {
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoiceDropdown("Format", format, listOf(null) + AnimeFormat.entries, { it?.label ?: "Any" }, { format = it })
            ChoiceDropdown("Status", status, listOf(null) + AiringStatus.entries, { it?.label ?: "Any" }, { status = it })
            ChoiceDropdown("Genre", genre, listOf(null) + AnimeGenre.entries, { it?.label ?: "Any" }, { genre = it })
            ChoiceDropdown("Order", sort, CatalogSort.entries, { it.label }, { sort = it })
            Text("You can browse with filters even without a title.")
        }
    }, confirmButton = { TextButton(onClick = { onApply(SearchFilters(format, status, genre, sort)) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

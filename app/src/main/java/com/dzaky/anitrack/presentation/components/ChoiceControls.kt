package com.dzaky.anitrack.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dzaky.anitrack.domain.LibrarySort

@Composable
fun <T> ChoiceDropdown(label: String, selected: T, options: List<T>, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(label + ": " + text(selected)) }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            options.forEach { choice -> DropdownMenuItem(text = { Text(text(choice)) }, enabled = enabled, onClick = { onSelect(choice); expanded = false }) }
        }
    }
}

@Composable
fun LibraryToolbar(query: String, onQuery: (String) -> Unit, sort: LibrarySort, onSort: (LibrarySort) -> Unit, includeProgress: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(query, onValueChange = { onQuery(it.take(120)) }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Filter your library") }, singleLine = true)
        ChoiceDropdown("Sort", sort, LibrarySort.entries.filter { includeProgress || it != LibrarySort.Progress }, { it.label }, onSort)
    }
}

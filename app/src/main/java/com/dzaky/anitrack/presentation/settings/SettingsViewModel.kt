package com.dzaky.anitrack.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dzaky.anitrack.data.LibraryRepository
import com.dzaky.anitrack.data.SettingsRepository
import com.dzaky.anitrack.data.ThemeMode
import com.dzaky.anitrack.presentation.components.StorageError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val library: LibraryRepository,
) : ViewModel() {
    val theme = settings.theme.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()

    fun setTheme(mode: ThemeMode) = change { settings.setTheme(mode) }
    fun clearSearchHistory() = change {
        library.clearSearchHistory()
        messages.send("Search history cleared")
    }
    fun clearRecentlyViewed() = change {
        library.clearRecentlyViewed()
        messages.send("Recently viewed cleared")
    }

    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { messages.send(StorageError) }
        }
    }
}

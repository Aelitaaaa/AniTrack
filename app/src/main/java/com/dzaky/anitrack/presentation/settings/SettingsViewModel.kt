package com.dzaky.anitrack.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dzaky.anitrack.data.AnimeRepository
import com.dzaky.anitrack.data.AppPreferences
import com.dzaky.anitrack.data.CatalogSource
import com.dzaky.anitrack.data.LibraryLayout
import com.dzaky.anitrack.data.LibraryRepository
import com.dzaky.anitrack.data.SettingsRepository
import com.dzaky.anitrack.data.ThemeMode
import com.dzaky.anitrack.data.backup.BackupFiles
import com.dzaky.anitrack.presentation.components.StorageError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val library: LibraryRepository,
    private val backup: BackupFiles,
    private val catalog: AnimeRepository,
) : ViewModel() {
    val preferences = settings.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())
    val theme = preferences.map { it.theme }.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()

    fun setTheme(mode: ThemeMode) = change { settings.setTheme(mode) }
    fun setIncognito(enabled: Boolean) = change { settings.setIncognito(enabled) }
    fun setLayout(layout: LibraryLayout) = change { settings.setLayout(layout) }
    fun setSource(source: CatalogSource) = change { settings.setSource(source); catalog.sourceChanged() }
    fun clearSearchHistory() = change { library.clearSearchHistory(); messages.send("Search history cleared") }
    fun clearRecentlyViewed() = change { library.clearRecentlyViewed(); messages.send("Recently viewed cleared") }

    fun exportBackup(uri: Uri) = backupOperation {
        backup.export(uri)
        messages.send("Backup saved")
    }

    fun restoreBackup(uri: Uri) = backupOperation {
        val result = backup.restore(uri)
        catalog.sourceChanged()
        messages.send(if (result.warnings.isEmpty()) "Backup merged. Newer watch progress was kept."
            else "Library restored. " + result.warnings.joinToString(" "))
    }

    private fun backupOperation(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                messages.send(error.message?.takeIf { error is IllegalArgumentException && it != "Failed requirement." }
                    ?: "Couldn't use this backup. Choose a valid AniTrack file and a readable location.")
            } finally { _busy.value = false }
        }
    }

    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { messages.send(StorageError) }
        }
    }
}

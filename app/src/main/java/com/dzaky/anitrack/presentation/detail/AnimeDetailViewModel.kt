package com.dzaky.anitrack.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dzaky.anitrack.data.AnimeDetailResult
import com.dzaky.anitrack.data.AnimeRepository
import com.dzaky.anitrack.data.LibraryRepository
import com.dzaky.anitrack.domain.AnimeCharacter
import com.dzaky.anitrack.domain.WatchStatus
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.components.StorageError
import com.dzaky.anitrack.presentation.components.catalogMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val animeRepository: AnimeRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val id: Int = checkNotNull(savedState["animeId"])
    private val _state = MutableStateFlow<LoadState<AnimeDetailResult>>(LoadState.Loading)
    val state = _state.asStateFlow()
    private val _characters = MutableStateFlow<LoadState<List<AnimeCharacter>>>(LoadState.Loading)
    val characters = _characters.asStateFlow()
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    val isFavorite = libraryRepository.favorite(id).map { it != null }
        .catch { messages.send(StorageError); emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val watchEntry = libraryRepository.watchEntry(id)
        .catch { messages.send(StorageError); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private var loadJob: Job? = null

    init { load() }

    fun load(refresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = LoadState.Loading
            _characters.value = LoadState.Loading
            try {
                val result = animeRepository.detail(id, refresh)
                _state.value = LoadState.Ready(result)
                change { libraryRepository.recordView(result.anime) }
                if (result.isOffline) _characters.value = LoadState.Ready(emptyList()) else loadCharacters()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.value = LoadState.Failed(error.catalogMessage())
            }
        }
    }

    fun retryCharacters() { viewModelScope.launch { loadCharacters() } }

    private suspend fun loadCharacters() {
        _characters.value = LoadState.Loading
        _characters.value = try {
            LoadState.Ready(animeRepository.characters(id))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LoadState.Failed(error.catalogMessage())
        }
    }

    fun toggleFavorite() {
        val anime = (_state.value as? LoadState.Ready)?.value?.anime ?: return
        change { libraryRepository.toggleFavorite(anime) }
    }

    fun addToWatchlist() {
        val anime = (_state.value as? LoadState.Ready)?.value?.anime ?: return
        change { libraryRepository.addToWatchlist(anime) }
    }

    fun removeFromWatchlist() = change { libraryRepository.removeFromWatchlist(id) }
    fun adjustEpisodes(amount: Int) = change { libraryRepository.adjustEpisodes(id, amount) }
    fun setEpisodes(watched: Int) = change { libraryRepository.setEpisodes(id, watched) }
    fun setStatus(status: WatchStatus) = change { libraryRepository.setStatus(id, status) }

    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { messages.send(StorageError) }
        }
    }
}

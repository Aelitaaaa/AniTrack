package com.dzaky.anitrack.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dzaky.anitrack.data.LibraryRepository
import com.dzaky.anitrack.data.local.FavoriteEntity
import com.dzaky.anitrack.data.local.WatchEntry
import com.dzaky.anitrack.domain.WatchStatus
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.components.StorageError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(private val library: LibraryRepository) : ViewModel() {
    private val retries = MutableStateFlow(0)
    val favorites = retries.flatMapLatest {
        library.favorites.map<List<FavoriteEntity>, LoadState<List<FavoriteEntity>>> { LoadState.Ready(it) }
            .catch { emit(LoadState.Failed(StorageError)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)
    val watchlist = retries.flatMapLatest {
        library.watchlist.map<List<WatchEntry>, LoadState<List<WatchEntry>>> { LoadState.Ready(it) }
            .catch { emit(LoadState.Failed(StorageError)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()

    fun retry() { retries.value += 1 }
    fun removeFavorite(id: Int) = change { library.removeFavorite(id) }
    fun removeWatchEntry(id: Int) = change { library.removeFromWatchlist(id) }
    fun adjustEpisodes(id: Int, amount: Int) = change { library.adjustEpisodes(id, amount) }
    fun setEpisodes(id: Int, watched: Int) = change { library.setEpisodes(id, watched) }
    fun setStatus(id: Int, status: WatchStatus) = change { library.setStatus(id, status) }

    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { messages.send(StorageError) }
        }
    }
}

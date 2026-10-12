package com.dzaky.anitrack.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dzaky.anitrack.data.ExtrasRepository
import com.dzaky.anitrack.data.local.CollectionEntity
import com.dzaky.anitrack.data.local.CollectionAnimeEntity
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.components.StorageError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CollectionsData(val collections: List<CollectionEntity>, val members: List<CollectionAnimeEntity>)

@HiltViewModel
class CollectionsViewModel @Inject constructor(private val extras: ExtrasRepository) : ViewModel() {
    val state = combine(extras.collections, extras.members, ::CollectionsData)
        .map<CollectionsData, LoadState<CollectionsData>> { LoadState.Ready(it) }
        .catch { emit(LoadState.Failed(StorageError)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()

    fun create(name: String) = change { extras.createCollection(name) }
    fun rename(id: String, name: String) = change { extras.renameCollection(id, name) }
    fun delete(id: String) = change { extras.deleteCollection(id) }
    fun remove(collection: String, anime: Int) = change { extras.removeMember(collection, anime) }

    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (error: IllegalArgumentException) { messages.send(error.message ?: "Check the collection name.") }
            catch (error: Exception) { messages.send(StorageError) }
        }
    }
}

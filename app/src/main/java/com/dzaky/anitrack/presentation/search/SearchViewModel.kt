package com.dzaky.anitrack.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dzaky.anitrack.data.AnimeRepository
import com.dzaky.anitrack.data.LibraryRepository
import com.dzaky.anitrack.domain.Anime
import com.dzaky.anitrack.domain.SearchFilters
import com.dzaky.anitrack.domain.AnimeFormat
import com.dzaky.anitrack.domain.AiringStatus
import com.dzaky.anitrack.domain.AnimeGenre
import com.dzaky.anitrack.domain.CatalogSort
import com.dzaky.anitrack.presentation.components.StorageError
import com.dzaky.anitrack.presentation.components.catalogMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Results(
        val query: String,
        val anime: List<Anime>,
        val hasNext: Boolean,
        val nextPage: Int = 2,
        val append: AppendState = AppendState.Idle,
        val filters: SearchFilters = SearchFilters(),
    ) : SearchUiState
    data class Failed(val message: String) : SearchUiState
}

sealed interface AppendState {
    data object Idle : AppendState
    data object Loading : AppendState
    data class Failed(val message: String) : AppendState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val animeRepository: AnimeRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    val query = savedState.getStateFlow("query", "")
    private val _filters = MutableStateFlow(SearchFilters(
        format = AnimeFormat.entries.firstOrNull { it.name == savedState.get<String>("search_format") },
        status = AiringStatus.entries.firstOrNull { it.name == savedState.get<String>("search_status") },
        genre = AnimeGenre.entries.firstOrNull { it.name == savedState.get<String>("search_genre") },
        sort = CatalogSort.entries.firstOrNull { it.name == savedState.get<String>("search_sort") } ?: CatalogSort.Relevance,
    ))
    val filters = _filters.asStateFlow()
    private val retryCount = MutableStateFlow(0)
    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state = _state.asStateFlow()
    val history = libraryRepository.searchHistory.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    private var appendJob: Job? = null

    init {
        viewModelScope.launch {
            combine(query, filters, retryCount) { text, selected, _ -> text.trim() to selected }.collectLatest { (text, selected) ->
                appendJob?.cancel()
                if (text.isEmpty() && selected.isDefault) {
                    _state.value = SearchUiState.Idle
                    return@collectLatest
                }
                _state.value = SearchUiState.Loading
                delay(450)
                val result = try {
                    val page = animeRepository.search(text, 1, selected)
                    SearchUiState.Results(text, page.anime, page.hasNextPage, filters = selected)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    SearchUiState.Failed(error.catalogMessage())
                }
                if (query.value.trim() != text || filters.value != selected) return@collectLatest
                _state.value = result
                if (result is SearchUiState.Results) {
                    try {
                        libraryRepository.recordSearch(text)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        messages.send(StorageError)
                    }
                }
            }
        }
    }

    fun updateQuery(value: String) {
        savedState["query"] = value.take(120)
        if (value.isBlank() && filters.value.isDefault) {
            appendJob?.cancel()
            _state.value = SearchUiState.Idle
        }
    }
    fun updateFilters(value: SearchFilters) {
        savedState["search_format"] = value.format?.name
        savedState["search_status"] = value.status?.name
        savedState["search_genre"] = value.genre?.name
        savedState["search_sort"] = value.sort.name
        _filters.value = value
    }

    fun retry() {
        animeRepository.retryConnection()
        retryCount.value += 1
    }

    fun loadMore() {
        val current = _state.value as? SearchUiState.Results ?: return
        if (!current.hasNext || current.append == AppendState.Loading) return
        _state.value = current.copy(append = AppendState.Loading)
        appendJob = viewModelScope.launch {
            try {
                val page = animeRepository.search(current.query, current.nextPage, current.filters)
                if (query.value.trim() == current.query && filters.value == current.filters) {
                    _state.value = current.copy(
                        anime = (current.anime + page.anime).distinctBy { it.id },
                        hasNext = page.hasNextPage,
                        nextPage = current.nextPage + 1,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (query.value.trim() == current.query && filters.value == current.filters) {
                    _state.value = current.copy(append = AppendState.Failed(error.catalogMessage()))
                }
            }
        }
    }

    fun removeHistory(query: String) {
        viewModelScope.launch {
            try { libraryRepository.removeSearch(query) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { messages.send(StorageError) }
        }
    }
}

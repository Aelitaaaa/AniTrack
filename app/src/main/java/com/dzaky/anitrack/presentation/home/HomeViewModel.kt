package com.dzaky.anitrack.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dzaky.anitrack.data.AnimeRepository
import com.dzaky.anitrack.data.LibraryRepository
import com.dzaky.anitrack.domain.Anime
import com.dzaky.anitrack.domain.DiscoverySection
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.components.catalogMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoveryRow(val section: DiscoverySection, val content: LoadState<List<Anime>> = LoadState.Loading, val isOffline: Boolean = false)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    libraryRepository: LibraryRepository,
) : ViewModel() {
    private val _sections = MutableStateFlow(DiscoverySection.entries.map { DiscoveryRow(it) })
    val sections = _sections.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()
    val recent = libraryRepository.recentlyViewed.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var refreshJob: Job? = null

    init { refresh(force = false) }

    fun refresh(force: Boolean = true) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            if (force) animeRepository.retryConnection()
            _refreshing.value = true
            try {
                DiscoverySection.entries.forEach { load(it, force) }
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun retry(section: DiscoverySection) {
        viewModelScope.launch {
            animeRepository.retryConnection()
            setSection(section, LoadState.Loading)
            load(section, force = true)
        }
    }

    private suspend fun load(section: DiscoverySection, force: Boolean) {
        try {
            val result = animeRepository.discovery(section, force)
            setSection(section, LoadState.Ready(result.anime), result.isOffline)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            setSection(section, LoadState.Failed(error.catalogMessage()))
        }
    }

    private fun setSection(section: DiscoverySection, state: LoadState<List<Anime>>, isOffline: Boolean = false) {
        _sections.update { rows -> rows.map { if (it.section == section) it.copy(content = state, isOffline = isOffline) else it } }
    }
}

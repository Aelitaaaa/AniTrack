package com.dzaky.anitrack.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dzaky.anitrack.data.ExtrasRepository
import com.dzaky.anitrack.data.LibraryRepository
import com.dzaky.anitrack.data.local.PersonalAnimeEntity
import com.dzaky.anitrack.domain.LibraryStatistics
import com.dzaky.anitrack.domain.StatisticsEntry
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.components.StorageError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StatisticsData(val statistics: LibraryStatistics, val favorites: Int, val collections: Int, val rated: List<PersonalAnimeEntity>)

@HiltViewModel
class StatisticsViewModel @Inject constructor(library: LibraryRepository, extras: ExtrasRepository) : ViewModel() {
    val state = combine(library.watchlist, library.favorites, extras.collections, extras.personal) { watchlist, favorites, collections, personal ->
        val rated = personal.filter { it.rating != null }.sortedWith(compareByDescending<PersonalAnimeEntity> { it.rating }.thenBy { it.title })
        StatisticsData(LibraryStatistics.calculate(watchlist.map { StatisticsEntry(it.watchedEpisodes, it.totalEpisodes, it.status, it.type) },
            rated.mapNotNull { it.rating }), favorites.size, collections.size, rated)
    }.map<StatisticsData, LoadState<StatisticsData>> { LoadState.Ready(it) }
        .catch { emit(LoadState.Failed(StorageError)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)
}

package com.dzaky.anitrack.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dzaky.anitrack.data.LibraryRepository
import com.dzaky.anitrack.data.ProfileRepository
import com.dzaky.anitrack.domain.AnimeBadge
import com.dzaky.anitrack.domain.LocalProfile
import com.dzaky.anitrack.domain.ProfileInput
import com.dzaky.anitrack.domain.ProfileStats
import com.dzaky.anitrack.domain.WatchStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: LocalProfile? = null,
    val earned: Set<AnimeBadge> = emptySet(),
    val stats: ProfileStats = ProfileStats(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(private val repository: ProfileRepository, library: LibraryRepository) : ViewModel() {
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    private val savedMessages = Channel<Unit>(Channel.BUFFERED)
    val saved = savedMessages.receiveAsFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()

    val state = combine(repository.profile, repository.earnedBadges, library.watchlist, library.favorites) { profile, earned, watchlist, favorites ->
        ProfileUiState(profile, earned, ProfileStats(
            episodes = watchlist.sumOf { it.watchedEpisodes.coerceAtLeast(0).toLong() },
            completed = watchlist.count { it.status == WatchStatus.Completed },
            favorites = favorites.size,
        ), isLoading = false)
    }.catch {
        messages.send("Couldn't read your profile. Please try again.")
        emit(ProfileUiState(isLoading = false))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileUiState())

    init {
        viewModelScope.launch {
            state.map { current ->
                if (current.profile == null) emptySet()
                else AnimeBadge.entries.filter { it.qualifies(current.stats) && it !in current.earned }.toSet()
            }.distinctUntilChanged().collect { earned ->
                try { repository.unlock(earned) }
                catch (error: CancellationException) { throw error }
                catch (error: Exception) { messages.send("Couldn't save your badges. Please try again.") }
            }
        }
    }

    fun save(input: ProfileInput) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                repository.save(input)
                savedMessages.send(Unit)
                messages.send("Profile saved")
            } catch (error: CancellationException) {
                throw error
            } catch (error: IllegalArgumentException) {
                messages.send(error.message ?: "Check your profile details.")
            } catch (error: Exception) {
                messages.send("Couldn't save your profile. Please try again.")
            } finally { _saving.value = false }
        }
    }
}

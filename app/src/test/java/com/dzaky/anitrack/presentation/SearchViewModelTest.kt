package com.dzaky.anitrack.presentation

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.dzaky.anitrack.data.AnimeRepository
import com.dzaky.anitrack.data.LibraryRepository
import com.dzaky.anitrack.data.local.AniTrackDatabase
import com.dzaky.anitrack.data.remote.AnimeDto
import com.dzaky.anitrack.data.remote.AnimeListResponse
import com.dzaky.anitrack.data.remote.AnimeResponse
import com.dzaky.anitrack.data.remote.CharactersResponse
import com.dzaky.anitrack.data.remote.JikanApi
import com.dzaky.anitrack.data.remote.PaginationDto
import com.dzaky.anitrack.data.remote.RequestGate
import com.dzaky.anitrack.presentation.search.SearchUiState
import com.dzaky.anitrack.presentation.search.SearchViewModel
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var database: AniTrackDatabase
    private lateinit var api: SearchApi
    private lateinit var viewModel: SearchViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AniTrackDatabase::class.java).build()
        api = SearchApi()
    }

    @After fun tearDown() {
        if (::viewModel.isInitialized) viewModel.viewModelScope.cancel()
        database.close()
        Dispatchers.resetMain()
    }

    @Test fun `rapid input sends only the final debounced query`() = runTest(dispatcher) {
        createViewModel()
        viewModel.updateQuery("a")
        advanceTimeBy(200)
        viewModel.updateQuery("ab")
        advanceTimeBy(200)
        viewModel.updateQuery("abc")
        advanceTimeBy(451)
        runCurrent()
        assertEquals(listOf("abc" to 1), api.calls)
        assertEquals("abc", (viewModel.state.value as SearchUiState.Results).query)
    }

    @Test fun `new query cancels old results`() = runTest(dispatcher) {
        createViewModel()
        viewModel.updateQuery("slow")
        advanceTimeBy(451)
        viewModel.updateQuery("latest")
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals("latest", (viewModel.state.value as SearchUiState.Results).query)
    }

    @Test fun `clearing input restores the idle state`() = runTest(dispatcher) {
        createViewModel()
        viewModel.updateQuery("title")
        advanceTimeBy(451)
        viewModel.updateQuery("")
        runCurrent()
        assertEquals(SearchUiState.Idle, viewModel.state.value)
    }

    @Test fun `saved query is restored after recreation`() = runTest(dispatcher) {
        createViewModel(SavedStateHandle(mapOf("query" to "restored")))
        advanceTimeBy(451)
        runCurrent()
        assertEquals("restored", (viewModel.state.value as SearchUiState.Results).query)
    }

    @Test fun `clearing during a request keeps the idle state`() = runTest(dispatcher) {
        createViewModel()
        viewModel.updateQuery("slow")
        advanceTimeBy(451)
        viewModel.updateQuery("")
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(SearchUiState.Idle, viewModel.state.value)
    }

    @Test fun `network failure produces a retryable state`() = runTest(dispatcher) {
        createViewModel()
        api.fail = true
        viewModel.updateQuery("title")
        advanceTimeBy(451)
        runCurrent()
        assertTrue(viewModel.state.value is SearchUiState.Failed)
        api.fail = false
        viewModel.retry()
        advanceTimeBy(2_000)
        runCurrent()
        assertTrue(viewModel.state.value is SearchUiState.Results)
    }

    private fun createViewModel(savedState: SavedStateHandle = SavedStateHandle()) {
        viewModel = SearchViewModel(savedState,
            AnimeRepository(api, RequestGate(), database.animeCache(), Json),
            LibraryRepository(database.favorites(), database.watchlist(), database.history()))
    }

    private class SearchApi : JikanApi {
        val calls = mutableListOf<Pair<String, Int>>()
        var fail = false
        override suspend fun search(query: String, page: Int, limit: Int, safeForWork: Boolean): AnimeListResponse {
            calls += query to page
            if (fail) throw IOException("Offline")
            if (query == "slow") delay(1_000)
            return AnimeListResponse(listOf(AnimeDto(id = 1, title = query)), PaginationDto(false))
        }
        override suspend fun topAnime(filter: String?, limit: Int, safeForWork: Boolean) = AnimeListResponse()
        override suspend fun airing(limit: Int, safeForWork: Boolean) = AnimeListResponse()
        override suspend fun upcoming(limit: Int, safeForWork: Boolean) = AnimeListResponse()
        override suspend fun detail(id: Int) = AnimeResponse()
        override suspend fun characters(id: Int) = CharactersResponse()
    }
}

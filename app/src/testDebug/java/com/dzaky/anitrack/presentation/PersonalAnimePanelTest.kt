package com.dzaky.anitrack.presentation

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.dzaky.anitrack.data.AnimeRepository
import com.dzaky.anitrack.data.ExtrasRepository
import com.dzaky.anitrack.data.LibraryRepository
import com.dzaky.anitrack.data.local.AniTrackDatabase
import com.dzaky.anitrack.data.local.CachedAnimeEntity
import com.dzaky.anitrack.data.remote.AnimeListResponse
import com.dzaky.anitrack.data.remote.AnimeResponse
import com.dzaky.anitrack.data.remote.CharactersResponse
import com.dzaky.anitrack.data.remote.JikanApi
import com.dzaky.anitrack.data.remote.RequestGate
import com.dzaky.anitrack.domain.Anime
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.detail.AnimeDetailViewModel
import com.dzaky.anitrack.presentation.detail.PersonalAnimePanel
import com.dzaky.anitrack.presentation.theme.AniTrackTheme
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PersonalAnimePanelTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: AniTrackDatabase
    private lateinit var vm: AnimeDetailViewModel
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AniTrackDatabase::class.java).build()
        runBlocking { db.animeCache().cache(CachedAnimeEntity(1, Json.encodeToString(Anime(1, "Title", null)), System.currentTimeMillis())) }
        vm = AnimeDetailViewModel(SavedStateHandle(mapOf("animeId" to 1)), AnimeRepository(EmptyApi(), RequestGate(), db.animeCache(), Json),
            LibraryRepository(db.favorites(), db.watchlist(), db.history()), ExtrasRepository(db))
        compose.setContent { AniTrackTheme { PersonalAnimePanel(vm) } }
        waitFor { vm.state.value is LoadState.Ready && vm.personal.value is LoadState.Ready }
    }
    @After fun close() { vm.viewModelScope.cancel(); db.close() }
    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(timeoutMillis = 5_000) { shadowOf(Looper.getMainLooper()).idle(); condition() }

    @Test fun userSavesPersonalRatingAndNotes() {
        compose.onNodeWithText("Rating and notes").performClick()
        compose.onNodeWithText("Rating: Not rated").performClick()
        compose.onNodeWithText("7 / 10").performClick()
        compose.onNodeWithText("Personal notes").performTextReplacement("Rewatch with friends")
        compose.onNodeWithText("Save").performClick()
        waitFor { (vm.personal.value as? LoadState.Ready)?.value?.rating == 7 && !vm.savingPersonal.value }
        compose.onNodeWithText("Your rating: 7 / 10").assertExists()
        compose.onNodeWithText("Rewatch with friends").assertExists()
    }

    @Test fun userCreatesCollectionAndAddsTheAnimeInOneAction() {
        compose.onNodeWithText("Collections (0)").performClick()
        compose.onNodeWithText("New collection name").performTextReplacement("Saturday")
        compose.onNodeWithText("Create and add").performClick()
        waitFor { vm.memberships.value.size == 1 && vm.collections.value.size == 1 }
        compose.onNodeWithText("Saturday").assertExists()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Collections (1)").assertExists()
    }

    private class EmptyApi : JikanApi {
        override suspend fun topAnime(filter: String?, limit: Int, safeForWork: Boolean) = AnimeListResponse()
        override suspend fun airing(limit: Int, safeForWork: Boolean) = AnimeListResponse()
        override suspend fun upcoming(limit: Int, safeForWork: Boolean) = AnimeListResponse()
        override suspend fun search(query: String, page: Int, limit: Int, safeForWork: Boolean) = AnimeListResponse()
        override suspend fun filteredSearch(query: String, page: Int, format: String?, status: String?, genres: String?, orderBy: String?, sort: String?, safeForWork: Boolean, limit: Int) = AnimeListResponse()
        override suspend fun detail(id: Int) = AnimeResponse()
        override suspend fun characters(id: Int) = CharactersResponse()
    }
}

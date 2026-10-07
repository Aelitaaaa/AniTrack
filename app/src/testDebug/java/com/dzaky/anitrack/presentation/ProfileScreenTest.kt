package com.dzaky.anitrack.presentation

import android.app.Application
import android.os.Looper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.dzaky.anitrack.data.LibraryRepository
import com.dzaky.anitrack.data.ProfileRepository
import com.dzaky.anitrack.data.local.AniTrackDatabase
import com.dzaky.anitrack.domain.AnimeBadge
import com.dzaky.anitrack.presentation.profile.ProfileScreen
import com.dzaky.anitrack.presentation.profile.ProfileViewModel
import com.dzaky.anitrack.presentation.theme.AniTrackTheme
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
class ProfileScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: AniTrackDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var viewModel: ProfileViewModel

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AniTrackDatabase::class.java).build()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File.createTempFile("profile-ui", ".preferences_pb").also { it.delete() }
        val profiles = ProfileRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
        viewModel = ProfileViewModel(profiles, LibraryRepository(database.favorites(), database.watchlist(), database.history()))
        compose.setContent { AniTrackTheme { ProfileScreen(viewModel) } }
        compose.waitUntil(timeoutMillis = 5_000) {
            // Room/DataStore resume on Android's main looper, which Robolectric pauses.
            shadowOf(Looper.getMainLooper()).idle()
            !viewModel.state.value.isLoading
        }
    }

    @After fun close() {
        viewModel.viewModelScope.cancel()
        scope.cancel()
        database.close()
    }

    @Test fun `user can create a local profile and see the welcome badge`() {
        compose.onNodeWithText("Create profile").performClick()
        compose.onNodeWithText("Name").performTextReplacement("Dzaky Putra")
        compose.onNodeWithText("Username").performTextReplacement("DZAKY")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            viewModel.state.value.profile?.username == "dzaky" && AnimeBadge.PendatangBaru in viewModel.state.value.earned
        }
        compose.onNodeWithText("@dzaky").assertExists()
        compose.onNodeWithText("Edit profile").assertExists()
    }

    @Test fun `invalid username keeps the profile form open`() {
        compose.onNodeWithText("Create profile").performClick()
        compose.onNodeWithText("Name").performTextReplacement("Dzaky")
        compose.onNodeWithText("Username").performTextReplacement("bad handle")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Use 3–20 letters, numbers, or underscores for your username.").assertExists()
        compose.onNodeWithText("Username").assertExists()
    }
}

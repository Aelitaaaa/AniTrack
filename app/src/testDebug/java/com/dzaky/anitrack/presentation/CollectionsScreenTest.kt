package com.dzaky.anitrack.presentation

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.dzaky.anitrack.data.ExtrasRepository
import com.dzaky.anitrack.data.local.AniTrackDatabase
import com.dzaky.anitrack.presentation.components.LoadState
import com.dzaky.anitrack.presentation.library.CollectionsData
import com.dzaky.anitrack.presentation.library.CollectionsScreen
import com.dzaky.anitrack.presentation.library.CollectionsViewModel
import com.dzaky.anitrack.presentation.theme.AniTrackTheme
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
class CollectionsScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: AniTrackDatabase
    private lateinit var vm: CollectionsViewModel
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AniTrackDatabase::class.java).build()
        vm = CollectionsViewModel(ExtrasRepository(db))
        compose.setContent { AniTrackTheme { CollectionsScreen(vm, {}) } }
        waitFor { vm.state.value is LoadState.Ready }
    }
    @After fun close() { vm.viewModelScope.cancel(); db.close() }
    private fun data() = (vm.state.value as? LoadState.Ready<CollectionsData>)?.value
    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(timeoutMillis = 5_000) { shadowOf(Looper.getMainLooper()).idle(); condition() }
    private fun create() {
        compose.onNodeWithText("New collection").performClick()
        compose.onNodeWithText("Collection name").performTextReplacement("Weekend")
        compose.onNodeWithText("Save").performClick()
        waitFor { data()?.collections?.any { it.name == "Weekend" } == true }
    }

    @Test fun userCreatesCollectionAndDuplicateNameKeepsDialogOpen() {
        create()
        compose.onNodeWithText("Weekend").assertExists()
        compose.onNodeWithText("New collection").performClick()
        compose.onNodeWithText("Collection name").performTextReplacement("WEEKEND")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("This name is already used.").assertExists()
    }

    @Test fun deletionRequiresConfirmationAndRemovesTheCollection() {
        create()
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Delete Weekend?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Weekend").assertExists()
        compose.onNodeWithText("Delete").performClick()
        compose.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
        waitFor { data()?.collections?.isEmpty() == true }
        compose.onNodeWithText("Make room for your favorites").assertExists()
    }
}

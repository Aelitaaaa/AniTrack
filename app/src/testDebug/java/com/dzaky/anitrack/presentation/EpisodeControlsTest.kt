package com.dzaky.anitrack.presentation

import android.app.Application
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.dzaky.anitrack.data.local.WatchEntry
import com.dzaky.anitrack.domain.WatchStatus
import com.dzaky.anitrack.presentation.components.EpisodeControls
import com.dzaky.anitrack.presentation.theme.AniTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class EpisodeControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `previous episode is disabled at zero`() {
        compose.setContent { AniTrackTheme { EpisodeControls(entry(0), {}, {}) } }
        compose.onNodeWithContentDescription("Previous episode").assertIsNotEnabled()
    }

    @Test fun `next episode is disabled at the total`() {
        compose.setContent { AniTrackTheme { EpisodeControls(entry(12), {}, {}) } }
        compose.onNodeWithContentDescription("Next episode").assertIsNotEnabled()
    }

    @Test fun `manual progress rejects values above total and saves a valid value`() {
        var saved = -1
        compose.setContent { AniTrackTheme { EpisodeControls(entry(3), {}, { saved = it }) } }
        compose.onNodeWithText("3 / 12 episodes").performClick()
        compose.onNodeWithText("Episodes watched").performTextReplacement("13")
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("Episodes watched").performTextReplacement("7")
        compose.onNodeWithText("Save").performClick()
        assertEquals(7, saved)
    }

    private fun entry(watched: Int) = WatchEntry(1, "Title", null, null, null, null, 12, WatchStatus.Watching, watched, 1)
}

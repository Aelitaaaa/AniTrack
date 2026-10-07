package com.dzaky.anitrack.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchProgressTest {
    @Test fun `negative progress is clamped to zero`() {
        assertEquals(0, WatchProgress(2, WatchStatus.Watching).changeEpisodes(-1, 12).watched)
    }

    @Test fun `progress cannot exceed the episode count`() {
        assertEquals(WatchProgress(12, WatchStatus.Completed), WatchProgress(4, WatchStatus.Watching).changeEpisodes(99, 12))
    }

    @Test fun `decreasing completed progress resumes watching`() {
        assertEquals(WatchProgress(11, WatchStatus.Watching), WatchProgress(12, WatchStatus.Completed).changeEpisodes(11, 12))
    }

    @Test fun `starting planned anime changes its status`() {
        assertEquals(WatchProgress(1, WatchStatus.Watching), WatchProgress(0, WatchStatus.Planned).changeEpisodes(1, 12))
    }

    @Test fun `unknown totals allow continued progress`() {
        assertEquals(WatchProgress(300, WatchStatus.Watching), WatchProgress(299, WatchStatus.Watching).changeEpisodes(300, null))
    }

    @Test fun `increment cannot overflow an unknown total`() {
        assertEquals(Int.MAX_VALUE, WatchProgress(Int.MAX_VALUE, WatchStatus.Watching).changeEpisodes(Int.MAX_VALUE.toLong() + 1, null).watched)
    }

    @Test fun `completing known anime fills its progress`() {
        assertEquals(WatchProgress(26, WatchStatus.Completed), WatchProgress(5, WatchStatus.Watching).changeStatus(WatchStatus.Completed, 26))
    }

    @Test fun `completing unknown anime preserves its progress`() {
        assertEquals(WatchProgress(15, WatchStatus.Completed), WatchProgress(15, WatchStatus.Watching).changeStatus(WatchStatus.Completed, null))
    }

    @Test fun `planning an anime resets its progress`() {
        assertEquals(WatchProgress(0, WatchStatus.Planned), WatchProgress(12, WatchStatus.Completed).changeStatus(WatchStatus.Planned, 12))
    }
}

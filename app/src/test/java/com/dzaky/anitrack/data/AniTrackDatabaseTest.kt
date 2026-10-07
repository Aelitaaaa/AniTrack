package com.dzaky.anitrack.data

import android.app.Application
import androidx.room.Room
import com.dzaky.anitrack.data.local.AniTrackDatabase
import com.dzaky.anitrack.data.local.FavoriteEntity
import com.dzaky.anitrack.data.local.RecentAnimeEntity
import com.dzaky.anitrack.data.local.SearchHistoryEntity
import com.dzaky.anitrack.data.local.WatchEntry
import com.dzaky.anitrack.domain.WatchStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AniTrackDatabaseTest {
    private lateinit var database: AniTrackDatabase

    @Before fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AniTrackDatabase::class.java).build()
    }

    @After fun closeDatabase() { database.close() }

    @Test fun `saving a favorite twice does not duplicate it`() = runBlocking {
        val favorite = FavoriteEntity(1, "Title", null, null, "TV", 2024, 12, 1)
        database.favorites().save(favorite)
        database.favorites().save(favorite.copy(addedAt = 2))
        assertEquals(1, database.favorites().observeAll().first().size)
    }

    @Test fun `favorite toggle adds then removes`() = runBlocking {
        val favorite = FavoriteEntity(1, "Title", null, null, null, null, null, 1)
        database.favorites().toggle(favorite)
        assertTrue(database.favorites().contains(1))
        database.favorites().toggle(favorite)
        assertFalse(database.favorites().contains(1))
    }

    @Test fun `adding existing watchlist anime preserves progress`() = runBlocking {
        database.watchlist().save(entry().copy(watchedEpisodes = 3, status = WatchStatus.Watching))
        database.watchlist().addIfAbsent(entry())
        assertEquals(3, database.watchlist().get(1)!!.watchedEpisodes)
    }

    @Test fun `concurrent episode updates are atomic and bounded`() = runBlocking {
        database.watchlist().save(entry())
        (1..20).map { async { database.watchlist().adjustEpisodes(1, 1, it.toLong()) } }.awaitAll()
        val saved = database.watchlist().get(1)!!
        assertEquals(12, saved.watchedEpisodes)
        assertEquals(WatchStatus.Completed, saved.status)
    }

    @Test fun `decreasing a completed entry updates the persisted status`() = runBlocking {
        database.watchlist().save(entry().copy(watchedEpisodes = 12, status = WatchStatus.Completed))
        database.watchlist().adjustEpisodes(1, -1, 2)
        assertEquals(WatchStatus.Watching, database.watchlist().get(1)!!.status)
    }

    @Test fun `recent anime are bounded and deduplicated`() = runBlocking {
        for (id in 1..35) database.history().recordView(RecentAnimeEntity(id, "Title $id", null, null, null, null, null, id.toLong()))
        database.history().recordView(RecentAnimeEntity(35, "Title 35", null, null, null, null, null, 99))
        val recent = database.history().observeRecent().first()
        assertEquals(30, recent.size)
        assertEquals(35, recent.first().animeId)
        assertFalse(recent.any { it.animeId == 1 })
    }

    @Test fun `search history is bounded and clearable`() = runBlocking {
        for (id in 1..20) database.history().recordSearch(SearchHistoryEntity("query $id", id.toLong()))
        assertEquals(15, database.history().observeSearches().first().size)
        database.history().removeSearch("query 20")
        assertEquals(14, database.history().observeSearches().first().size)
        database.history().clearSearches()
        assertTrue(database.history().observeSearches().first().isEmpty())
    }

    @Test fun `favorites and progress survive reopening the database`() = runBlocking {
        database.close()
        val context = RuntimeEnvironment.getApplication()
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        try {
            database = Room.databaseBuilder(context, AniTrackDatabase::class.java, name).build()
            database.favorites().save(FavoriteEntity(1, "Title", null, null, "TV", 2024, 12, 1))
            database.watchlist().save(entry().copy(watchedEpisodes = 5, status = WatchStatus.Watching))
            database.close()
            database = Room.databaseBuilder(context, AniTrackDatabase::class.java, name).build()
            assertTrue(database.favorites().contains(1))
            assertEquals(5, database.watchlist().get(1)!!.watchedEpisodes)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    private fun entry() = WatchEntry(1, "Title", null, null, "TV", 2024, 12, WatchStatus.Planned, 0, 1)
}

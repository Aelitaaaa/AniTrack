package com.dzaky.anitrack.data

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.dzaky.anitrack.data.local.AniTrackDatabase
import com.dzaky.anitrack.domain.Anime
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PrivacySettingsTest {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: AniTrackDatabase
    private lateinit var settings: SettingsRepository
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AniTrackDatabase::class.java).build()
        val file = File.createTempFile("privacy", ".preferences_pb").also { it.delete() }
        settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
    }
    @After fun close() { scope.cancel(); db.close() }

    @Test fun incognitoHidesPastHistoryAndNeverRecordsNewQueriesOrViews() = runBlocking {
        val library = LibraryRepository(db.favorites(), db.watchlist(), db.history(), settings)
        library.recordSearch("Old search")
        library.recordView(Anime(1, "Old title", null))
        settings.setIncognito(true)
        library.recordSearch("Private search")
        library.recordView(Anime(2, "Private title", null))
        assertEquals(0, library.searchHistory.first().size)
        assertEquals(0, library.recentlyViewed.first().size)
        assertEquals("old search", db.history().searchSnapshot().single().query)
        assertEquals(1, db.history().recentSnapshot().single().animeId)
        settings.setIncognito(false)
        library.recordSearch(" Public ")
        assertEquals(2, library.searchHistory.first().size)
    }

    @Test fun unknownPrivacyStateFailsClosed() = runBlocking {
        db.history().recordSearch(com.dzaky.anitrack.data.local.SearchHistoryEntity("old", 1))
        val broken = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("Storage unavailable") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw IOException("Storage unavailable")
        }
        val library = LibraryRepository(db.favorites(), db.watchlist(), db.history(), SettingsRepository(broken))
        assertThrows(IOException::class.java) { runBlocking { library.recordSearch("Private") } }
        assertEquals(1, db.history().searchSnapshot().size)
        assertEquals(0, library.searchHistory.first().size)
    }

    @Test fun newPreferencesKeepTheExistingThemeKey() = runBlocking {
        settings.setTheme(ThemeMode.Dark)
        settings.setIncognito(true)
        settings.setLayout(LibraryLayout.Grid)
        settings.setSource(CatalogSource.AniList)
        assertEquals(AppPreferences(ThemeMode.Dark, true, LibraryLayout.Grid, CatalogSource.AniList), settings.preferences.first())
    }
}

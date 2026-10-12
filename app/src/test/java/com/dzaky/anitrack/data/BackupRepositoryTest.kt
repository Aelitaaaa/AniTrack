package com.dzaky.anitrack.data

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import com.dzaky.anitrack.data.backup.BackupDocument
import com.dzaky.anitrack.data.backup.BackupRepository
import com.dzaky.anitrack.data.local.AniTrackDatabase
import com.dzaky.anitrack.data.local.CollectionAnimeEntity
import com.dzaky.anitrack.data.local.FavoriteEntity
import com.dzaky.anitrack.data.local.PersonalAnimeEntity
import com.dzaky.anitrack.data.local.WatchEntry
import com.dzaky.anitrack.domain.AnimeBadge
import com.dzaky.anitrack.domain.ProfileInput
import com.dzaky.anitrack.domain.WatchStatus
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class BackupRepositoryTest {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var source: AniTrackDatabase
    private lateinit var target: AniTrackDatabase
    private lateinit var sourceProfiles: ProfileRepository
    private lateinit var targetProfiles: ProfileRepository
    private lateinit var sourceSettings: SettingsRepository
    private lateinit var targetSettings: SettingsRepository
    private fun store(): DataStore<Preferences> {
        val file = File.createTempFile("backup", ".preferences_pb").also { it.delete() }
        return PreferenceDataStoreFactory.create(scope = scope) { file }
    }
    @Before fun setup() {
        source = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AniTrackDatabase::class.java).build()
        target = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AniTrackDatabase::class.java).build()
        val a = store(); val b = store()
        sourceProfiles = ProfileRepository(a); sourceSettings = SettingsRepository(a)
        targetProfiles = ProfileRepository(b); targetSettings = SettingsRepository(b)
    }
    @After fun close() { scope.cancel(); source.close(); target.close() }

    @Test fun restoringTwiceMergesCollectionsAndPreservesNewerProgressAndExistingProfile() = runBlocking {
        source.favorites().save(FavoriteEntity(1, "Title", null, 8.0, "TV", 2020, 12, 10))
        source.watchlist().save(entry(3, 20))
        val sourceCollection = ExtrasRepository(source).createCollection("Weekend")
        source.extras().saveMember(CollectionAnimeEntity(sourceCollection, 1, "Title", null, 8.0, "TV", 2020, 12, 20))
        source.extras().savePersonal(PersonalAnimeEntity(1, "Title", 8, "猫\nNotes", 20))
        sourceProfiles.save(ProfileInput("Other profile", "other"))
        sourceProfiles.unlock(setOf(AnimeBadge.Maraton))
        sourceSettings.restore(AppPreferences(ThemeMode.Black, true, LibraryLayout.Grid, CatalogSource.AniList))
        target.watchlist().save(entry(7, 30))
        val targetCollection = ExtrasRepository(target).createCollection("Weekend")
        targetProfiles.save(ProfileInput("Dzaky", "ykazd"))
        val profileId = targetProfiles.snapshot().profile!!.id
        val bytes = BackupRepository(source, sourceProfiles, sourceSettings).export()
        val backup = BackupRepository(target, targetProfiles, targetSettings)
        assertEquals(emptyList<String>(), backup.restore(bytes).warnings)
        backup.restore(bytes)
        assertEquals(7, target.watchlist().get(1)!!.watchedEpisodes)
        assertEquals(1, target.favorites().snapshot().size)
        assertEquals(1, target.extras().collections().size)
        assertEquals(targetCollection, target.extras().members().single().collectionId)
        assertEquals("猫\nNotes", target.extras().personal(1)!!.notes)
        assertEquals(profileId, targetProfiles.snapshot().profile!!.id)
        assertFalse(AnimeBadge.Maraton in targetProfiles.snapshot().badges)
        assertEquals(ThemeMode.Black, targetSettings.theme.first())
        assertEquals(LibraryLayout.Grid, targetSettings.preferences.first().layout)
    }

    @Test fun validationFailureLeavesEveryDatabaseSectionUntouched() = runBlocking {
        target.watchlist().save(entry(7, 30))
        val invalid = BackupDocument(favorites = listOf(FavoriteEntity(2, "New", null, null, null, null, null, 1)),
            watchlist = listOf(entry(99, 1)))
        val bytes = Json { encodeDefaults = true }.encodeToString(invalid).toByteArray()
        assertThrows(IllegalArgumentException::class.java) { runBlocking { BackupRepository(target, targetProfiles, targetSettings).restore(bytes) } }
        assertEquals(0, target.favorites().snapshot().size)
        assertEquals(7, target.watchlist().get(1)!!.watchedEpisodes)
    }

    @Test fun dataStoreFailureReportsPartialRestoreWithoutDiscardingLibrary() = runBlocking {
        source.favorites().save(FavoriteEntity(1, "Title", null, null, null, null, null, 1))
        sourceProfiles.save(ProfileInput("Dzaky", "ykazd"))
        val bytes = BackupRepository(source, sourceProfiles, sourceSettings).export()
        val broken = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flowOf(emptyPreferences())
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw IOException("Storage unavailable")
        }
        val result = BackupRepository(target, ProfileRepository(broken), SettingsRepository(broken)).restore(bytes)
        assertEquals(2, result.warnings.size)
        assertEquals(1, target.favorites().snapshot().size)
    }

    private fun entry(watched: Int, time: Long) = WatchEntry(1, "Title", null, 8.0, "TV", 2020, 12, WatchStatus.Watching, watched, time)
}

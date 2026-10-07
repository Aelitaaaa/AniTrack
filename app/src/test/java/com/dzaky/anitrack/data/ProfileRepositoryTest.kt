package com.dzaky.anitrack.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.dzaky.anitrack.domain.AnimeBadge
import com.dzaky.anitrack.domain.ProfileInput
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProfileRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun `profile badges and theme survive edits and reopening`() = runBlocking {
        val file = File(folder.root, "settings.preferences_pb")
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        val repository = ProfileRepository(store)
        repository.save(ProfileInput("Dzaky", "DZAKY"))
        val original = repository.profile.first()!!
        repository.unlock(setOf(AnimeBadge.Maraton, AnimeBadge.SepuhAnime))
        SettingsRepository(store).setTheme(ThemeMode.Dark)
        repository.save(ProfileInput("Dzaky Putra", "dzaky_putra", "My watchlist"))
        scope.cancel()
        job.join()
        val reopenedScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            val reopenedStore = PreferenceDataStoreFactory.create(scope = reopenedScope) { file }
            val reopened = ProfileRepository(reopenedStore)
            val profile = reopened.profile.first()!!
            assertEquals(original.id, profile.id)
            assertEquals(original.createdAt, profile.createdAt)
            assertEquals("dzaky_putra", profile.username)
            assertEquals("My watchlist", profile.bio)
            assertEquals(setOf(AnimeBadge.PendatangBaru, AnimeBadge.Maraton, AnimeBadge.SepuhAnime), reopened.earnedBadges.first())
            assertEquals(ThemeMode.Dark, SettingsRepository(reopenedStore).theme.first())
        } finally { reopenedScope.cancel() }
    }

    @Test fun `invalid input and badge awards cannot create a profile`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            val repository = ProfileRepository(PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "invalid.preferences_pb") })
            repository.unlock(setOf(AnimeBadge.SepuhAnime))
            assertTrue(repository.earnedBadges.first().isEmpty())
            assertTrue(runCatching { repository.save(ProfileInput("D", "invalid handle")) }.exceptionOrNull() is IllegalArgumentException)
            assertNull(repository.profile.first())
        } finally { scope.cancel() }
    }
}

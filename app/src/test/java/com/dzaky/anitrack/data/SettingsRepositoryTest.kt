package com.dzaky.anitrack.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun `theme defaults to system and survives reopening`() = runBlocking {
        val file = File(folder.root, "settings.preferences_pb")
        val firstJob = SupervisorJob()
        val firstScope = CoroutineScope(Dispatchers.IO + firstJob)
        val firstStore = PreferenceDataStoreFactory.create(scope = firstScope) { file }
        val firstRepository = SettingsRepository(firstStore)
        assertEquals(ThemeMode.System, firstRepository.theme.first())
        firstRepository.setTheme(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, firstRepository.theme.first())
        firstScope.cancel()
        firstJob.join()
        val secondScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            val reopened = SettingsRepository(PreferenceDataStoreFactory.create(scope = secondScope) { file })
            assertEquals(ThemeMode.Dark, reopened.theme.first())
        } finally { secondScope.cancel() }
    }
}

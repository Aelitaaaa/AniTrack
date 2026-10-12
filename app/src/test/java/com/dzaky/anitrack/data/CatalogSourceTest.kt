package com.dzaky.anitrack.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.dzaky.anitrack.data.local.AnimeCacheDao
import com.dzaky.anitrack.data.local.CachedAnimeEntity
import com.dzaky.anitrack.data.remote.AniListApi
import com.dzaky.anitrack.data.remote.AniListCatalog
import com.dzaky.anitrack.data.remote.JikanApi
import com.dzaky.anitrack.data.remote.RequestGate
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class CatalogSourceTest {
    @Test fun explicitSourceSelectionUsesOnlyTheChosenProvider() = runBlocking {
        val primary = MockWebServer().also { it.start() }
        val backup = MockWebServer().also { it.start() }
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            val file = File.createTempFile("source", ".preferences_pb").also { it.delete() }
            val settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
            val json = Json { ignoreUnknownKeys = true }
            fun <T> api(server: MockWebServer, type: Class<T>): T = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(type)
            val cache = object : AnimeCacheDao() {
                override suspend fun get(id: Int): CachedAnimeEntity? = null
                override suspend fun save(entry: CachedAnimeEntity) = Unit
                override suspend fun trim() = Unit
            }
            val repository = AnimeRepository(api(primary, JikanApi::class.java), RequestGate(), cache, json,
                AniListCatalog(api(backup, AniListApi::class.java)), settings)
            primary.enqueue(MockResponse.Builder().addHeader("Content-Type", "application/json")
                .body("""{"data":[{"mal_id":20,"title":"Jikan title"}]}""").build())
            backup.enqueue(MockResponse.Builder().addHeader("Content-Type", "application/json")
                .body("""{"data":{"Page":{"media":[{"idMal":20,"title":{"english":"AniList title"}}]}}}""").build())
            settings.setSource(CatalogSource.Jikan)
            assertEquals("Jikan title", repository.search("Naruto", 1).anime.single().title)
            settings.setSource(CatalogSource.AniList)
            repository.sourceChanged()
            assertEquals("AniList title", repository.search("Naruto", 1).anime.single().title)
            assertEquals(1, primary.requestCount)
            assertEquals(1, backup.requestCount)
        } finally { scope.cancel(); primary.close(); backup.close() }
    }
}

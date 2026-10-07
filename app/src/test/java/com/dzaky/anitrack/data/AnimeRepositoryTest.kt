package com.dzaky.anitrack.data

import com.dzaky.anitrack.data.local.AnimeCacheDao
import com.dzaky.anitrack.data.local.CachedAnimeEntity
import com.dzaky.anitrack.data.remote.JikanApi
import com.dzaky.anitrack.data.remote.RequestGate
import com.dzaky.anitrack.domain.DiscoverySection
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AnimeRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: AnimeRepository
    private lateinit var cache: MemoryCache

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(JikanApi::class.java)
        cache = MemoryCache()
        repository = AnimeRepository(api, RequestGate(), cache, json)
    }

    @After fun tearDown() { server.close() }

    @Test fun `discovery cache avoids duplicate requests`() = runBlocking {
        respond("""{"data":[{"mal_id":1,"title":"Title"}]}""")
        repository.discovery(DiscoverySection.Popular)
        repository.discovery(DiscoverySection.Popular)
        assertEquals(1, server.requestCount)
        assertTrue(server.takeRequest().url.toString().contains("filter=bypopularity"))
    }

    @Test fun `empty search responses are valid results`() = runBlocking {
        respond("""{"data":[],"pagination":{"has_next_page":false}}""")
        val result = repository.search("missing", 1)
        assertTrue(result.anime.isEmpty())
        assertFalse(result.hasNextPage)
    }

    @Test fun `search carries pagination and removes duplicates`() = runBlocking {
        respond("""{"data":[{"mal_id":1,"title":"One"},{"mal_id":1,"title":"One"}],"pagination":{"has_next_page":true}}""")
        val result = repository.search("one", 2)
        assertEquals(1, result.anime.size)
        assertTrue(result.hasNextPage)
        assertTrue(server.takeRequest().url.toString().contains("page=2"))
    }

    @Test fun `failed refresh falls back to previously cached details`() = runBlocking {
        respond("""{"data":{"mal_id":1,"title":"Saved title"}}""")
        repository.detail(1)
        server.enqueue(MockResponse.Builder().code(503).build())
        val result = repository.detail(1, refresh = true)
        assertEquals("Saved title", result.anime.title)
        assertTrue(result.isOffline)
    }

    @Test fun `missing detail responses produce a meaningful failure`() = runBlocking {
        respond("""{"data":null}""")
        val error = runCatching { repository.detail(1) }.exceptionOrNull()
        assertTrue(error is CatalogException)
        assertTrue((error as CatalogException).explanation.contains("unavailable"))
    }

    @Test fun `malformed JSON is reported without leaking parser details`() = runBlocking {
        respond("not JSON")
        val error = runCatching { repository.search("title", 1) }.exceptionOrNull()
        assertTrue(error is CatalogException)
        assertTrue((error as CatalogException).explanation.contains("unexpected data"))
    }

    @Test fun `rate limit is retried once and then reported`() = runBlocking {
        repeat(2) { server.enqueue(MockResponse.Builder().code(429).addHeader("Retry-After", "1").build()) }
        assertTrue(runCatching { repository.search("title", 1) }.exceptionOrNull() is CatalogException)
        assertEquals(2, server.requestCount)
    }

    private fun respond(body: String) {
        server.enqueue(MockResponse.Builder().addHeader("Content-Type", "application/json").body(body).build())
    }

    private class MemoryCache : AnimeCacheDao() {
        private val entries = mutableMapOf<Int, CachedAnimeEntity>()
        override suspend fun get(id: Int) = entries[id]
        override suspend fun save(entry: CachedAnimeEntity) { entries[entry.animeId] = entry }
        override suspend fun trim() = Unit
    }
}

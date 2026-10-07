package com.dzaky.anitrack.data

import com.dzaky.anitrack.data.local.AnimeCacheDao
import com.dzaky.anitrack.data.local.CachedAnimeEntity
import com.dzaky.anitrack.data.remote.*
import com.dzaky.anitrack.domain.DiscoverySection
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogFallbackTest {
    @Test fun `unreachable Jikan switches provider without changing MAL ids`() = runTest {
        val primary = Primary().apply { fail = true }
        val backup = Backup()
        val repository = repository(primary, backup)
        val result = repository.discovery(DiscoverySection.Popular)
        assertEquals(20, result.anime.single().id)
        assertEquals("Naruto", result.anime.single().title)
        assertEquals(1, primary.calls)
        assertEquals(1, backup.calls)
        repository.search("naruto", 1)
        assertEquals(1, primary.calls)
        assertEquals(2, backup.calls)
    }

    @Test fun `slow primary is cancelled when the backup answers`() = runTest {
        val primary = Primary().apply { slow = true }
        val repository = repository(primary, Backup())
        assertEquals("Naruto", repository.discovery(DiscoverySection.Popular).anime.single().title)
        runCurrent()
        assertTrue(primary.cancelled)
    }

    @Test fun `both providers failing does not repeat timeouts for every section`() = runTest {
        val primary = Primary().apply { fail = true }
        val backup = Backup().apply { fail = true }
        val repository = repository(primary, backup)
        DiscoverySection.entries.forEach { assertTrue(runCatching { repository.discovery(it) }.exceptionOrNull() is CatalogException) }
        assertEquals(1, primary.calls)
        assertEquals(1, backup.calls)
        primary.fail = false
        repository.retryConnection()
        assertEquals("Primary title", repository.search("title", 1).anime.single().title)
    }

    @Test fun `saved discovery survives repository recreation and a failed refresh`() = runTest {
        val cache = MemoryCache()
        val primary = Primary()
        repository(primary, Backup(), cache).discovery(DiscoverySection.Popular)
        primary.fail = true
        val reopened = repository(primary, Backup().apply { fail = true }, cache)
        assertEquals("Primary title", reopened.discovery(DiscoverySection.Popular).anime.single().title)
        assertEquals(1, primary.calls)
        val stale = reopened.discovery(DiscoverySection.Popular, refresh = true)
        assertTrue(stale.isOffline)
        assertEquals("Primary title", stale.anime.single().title)
    }

    @Test fun `cancelled requests never start a delayed backup`() = runTest {
        val primary = Primary().apply { slow = true }
        val backup = Backup()
        val repository = repository(primary, backup)
        val job = launch { repository.search("title", 1) }
        advanceTimeBy(100)
        job.cancel()
        runCurrent()
        assertEquals(0, backup.calls)
    }

    private fun repository(primary: Primary, backup: Backup, cache: MemoryCache = MemoryCache()) =
        AnimeRepository(primary, RequestGate(), cache, Json, AniListCatalog(backup))

    private class Primary : JikanApi {
        var fail = false
        var slow = false
        var cancelled = false
        var calls = 0
        private suspend fun data(): List<AnimeDto> {
            calls++
            if (slow) try { awaitCancellation() } catch (error: CancellationException) { cancelled = true; throw error }
            if (fail) throw IOException("Cannot connect")
            return listOf(AnimeDto(id = 20, title = "Primary title"))
        }
        override suspend fun topAnime(filter: String?, limit: Int, safeForWork: Boolean) = AnimeListResponse(data())
        override suspend fun airing(limit: Int, safeForWork: Boolean) = AnimeListResponse(data())
        override suspend fun upcoming(limit: Int, safeForWork: Boolean) = AnimeListResponse(data())
        override suspend fun search(query: String, page: Int, limit: Int, safeForWork: Boolean) = AnimeListResponse(data())
        override suspend fun detail(id: Int) = AnimeResponse(data().single())
        override suspend fun characters(id: Int) = CharactersResponse()
    }

    private class Backup : AniListApi {
        var calls = 0
        var fail = false
        override suspend fun query(request: AniListRequest): AniListResponse {
            calls++
            if (fail) throw IOException("Cannot connect")
            return AniListResponse(AniListData(page = AniListPage(listOf(AniListMedia(idMal = 20, title = AniListTitle(english = "Naruto"))))))
        }
    }

    private class MemoryCache : AnimeCacheDao() {
        private val entries = mutableMapOf<Int, CachedAnimeEntity>()
        override suspend fun get(id: Int) = entries[id]
        override suspend fun save(entry: CachedAnimeEntity) { entries[entry.animeId] = entry }
        override suspend fun trim() = Unit
    }
}

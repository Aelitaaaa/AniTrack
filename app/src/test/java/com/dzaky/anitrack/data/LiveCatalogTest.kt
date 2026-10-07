package com.dzaky.anitrack.data

import com.dzaky.anitrack.data.local.AnimeCacheDao
import com.dzaky.anitrack.data.local.CachedAnimeEntity
import com.dzaky.anitrack.data.remote.AniListCatalog
import com.dzaky.anitrack.data.remote.RequestGate
import com.dzaky.anitrack.di.AppModule
import com.dzaky.anitrack.domain.DiscoverySection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class LiveCatalogTest {
    @Test fun `live backup handles discovery search details and characters`() = runBlocking {
        assumeTrue(System.getenv("ANITRACK_LIVE_TESTS") == "true")
        val json = AppModule.json()
        val backup = AniListCatalog(AppModule.alternateApi(AppModule.client(), json))
        val gate = RequestGate()
        gate.awaitTurn(2_100)
        assertTrue(backup.discovery(DiscoverySection.Popular).isNotEmpty())
        gate.awaitTurn(2_100)
        assertTrue(backup.search("Naruto", 1).anime.any { it.id == 20 })
        gate.awaitTurn(2_100)
        assertEquals(20, backup.detail(20).id)
        gate.awaitTurn(2_100)
        assertTrue(backup.characters(20).isNotEmpty())
    }

    @Test fun `live automatic catalog returns real discovery titles`() = runBlocking {
        assumeTrue(System.getenv("ANITRACK_LIVE_TESTS") == "true")
        val json = AppModule.json()
        val client = AppModule.client()
        val cache = object : AnimeCacheDao() {
            override suspend fun get(id: Int): CachedAnimeEntity? = null
            override suspend fun save(entry: CachedAnimeEntity) = Unit
            override suspend fun trim() = Unit
        }
        val repository = AnimeRepository(AppModule.api(client, json), RequestGate(), cache, json,
            AniListCatalog(AppModule.alternateApi(client, json)))
        val result = repository.discovery(DiscoverySection.Popular)
        assertTrue(result.anime.isNotEmpty())
        assertTrue(result.anime.all { it.id > 0 && it.title.isNotBlank() })
    }
}

package com.dzaky.anitrack.data

import com.dzaky.anitrack.data.local.AnimeCacheDao
import com.dzaky.anitrack.data.local.CachedAnimeEntity
import com.dzaky.anitrack.data.remote.JikanApi
import com.dzaky.anitrack.data.remote.RequestGate
import com.dzaky.anitrack.data.remote.toAnime
import com.dzaky.anitrack.data.remote.toCharacter
import com.dzaky.anitrack.domain.Anime
import com.dzaky.anitrack.domain.AnimeCharacter
import com.dzaky.anitrack.domain.AnimePage
import com.dzaky.anitrack.domain.DiscoverySection
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class CatalogException(val explanation: String) : Exception(explanation)

data class AnimeDetailResult(val anime: Anime, val isOffline: Boolean = false)

@Singleton
class AnimeRepository @Inject constructor(
    private val api: JikanApi,
    private val gate: RequestGate,
    private val cache: AnimeCacheDao,
    private val json: Json,
) {
    private val discoveryCache = mutableMapOf<DiscoverySection, Pair<Long, List<Anime>>>()
    private val characterCache = object : LinkedHashMap<Int, List<AnimeCharacter>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<AnimeCharacter>>) = size > 16
    }

    suspend fun discovery(section: DiscoverySection, refresh: Boolean = false): List<Anime> {
        discoveryCache[section]?.takeIf { !refresh && isFresh(it.first, 10 * 60_000L) }?.let { return it.second }
        val response = request {
            when (section) {
                DiscoverySection.Airing -> api.airing()
                DiscoverySection.Popular -> api.topAnime(filter = "bypopularity")
                DiscoverySection.TopRated -> api.topAnime()
                DiscoverySection.Upcoming -> api.upcoming()
            }
        }
        return response.data.mapNotNull { it.toAnime() }.distinctBy { it.id }.also {
            discoveryCache[section] = System.currentTimeMillis() to it
        }
    }

    suspend fun search(query: String, page: Int): AnimePage {
        val response = request { api.search(query.trim(), page) }
        return AnimePage(response.data.mapNotNull { it.toAnime() }.distinctBy { it.id }, response.pagination.hasNextPage)
    }

    suspend fun detail(id: Int, refresh: Boolean = false): AnimeDetailResult {
        val cached = optionalCache { cache.get(id) }
        val saved = cached?.let { optionalCache { json.decodeFromString<Anime>(it.json) } }
        if (!refresh && cached != null && saved != null && isFresh(cached.fetchedAt, 30 * 60_000L)) {
            return AnimeDetailResult(saved)
        }
        return try {
            val anime = request { api.detail(id) }.data?.toAnime()
                ?: throw CatalogException("This anime is unavailable. Please try another title.")
            optionalCache { cache.cache(CachedAnimeEntity(id, json.encodeToString(anime), System.currentTimeMillis())) }
            AnimeDetailResult(anime)
        } catch (error: CatalogException) {
            saved?.let { AnimeDetailResult(it, isOffline = true) } ?: throw error
        }
    }

    suspend fun characters(id: Int): List<AnimeCharacter> {
        characterCache[id]?.let { return it }
        return request { api.characters(id) }.data
            .filter { it.character.id > 0 }
            .distinctBy { it.character.id }
            .sortedBy { if (it.role == "Main") 0 else 1 }
            .take(12).map { it.toCharacter() }
            .also { characterCache[id] = it }
    }

    private suspend fun <T> request(block: suspend () -> T): T {
        repeat(2) { attempt ->
            gate.awaitTurn()
            try {
                return block()
            } catch (error: HttpException) {
                if (error.code() == 429 && attempt == 0) {
                    val retryAfter = error.response()?.headers()?.get("Retry-After")?.toLongOrNull()
                    delay((retryAfter ?: 3).coerceIn(1, 30) * 1_000)
                } else {
                    throw CatalogException(when (error.code()) {
                        429 -> "The anime service is busy. Wait a moment, then try again."
                        404 -> "This anime could not be found."
                        else -> "The anime service is unavailable. Please try again."
                    })
                }
            } catch (error: SocketTimeoutException) {
                throw CatalogException("The request took too long. Please try again.")
            } catch (error: IOException) {
                throw CatalogException("Cannot reach the anime service. Check your internet connection.")
            } catch (error: SerializationException) {
                throw CatalogException("The anime service returned unexpected data. Please try again later.")
            }
        }
        throw CatalogException("The anime service is busy. Please try again later.")
    }

    private fun isFresh(timestamp: Long, ttl: Long): Boolean = (System.currentTimeMillis() - timestamp) in 0 until ttl

    private suspend fun <T> optionalCache(block: suspend () -> T): T? = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        // Cached details are optional; a storage problem must not prevent discovery.
        null
    }
}

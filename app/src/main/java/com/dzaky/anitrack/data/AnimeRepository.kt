package com.dzaky.anitrack.data

import com.dzaky.anitrack.data.local.AnimeCacheDao
import com.dzaky.anitrack.data.local.CachedAnimeEntity
import com.dzaky.anitrack.data.remote.JikanApi
import com.dzaky.anitrack.data.remote.AniListCatalog
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
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class CatalogException(val explanation: String, val isConnectionFailure: Boolean = false) : Exception(explanation)

data class AnimeDetailResult(val anime: Anime, val isOffline: Boolean = false)
data class AnimeDiscoveryResult(val anime: List<Anime>, val isOffline: Boolean = false)

@Singleton
class AnimeRepository @Inject constructor(
    private val api: JikanApi,
    private val gate: RequestGate,
    private val cache: AnimeCacheDao,
    private val json: Json,
    private val alternate: AniListCatalog? = null,
) {
    @Volatile private var preferAlternateUntil = 0L
    @Volatile private var failureUntil = 0L
    @Volatile private var lastFailure = CatalogException("Cannot reach the anime catalog. Check your internet connection.", true)
    private val discoveryCache = mutableMapOf<DiscoverySection, Pair<Long, List<Anime>>>()
    private val characterCache = object : LinkedHashMap<Int, List<AnimeCharacter>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<AnimeCharacter>>) = size > 16
    }

    suspend fun discovery(section: DiscoverySection, refresh: Boolean = false): AnimeDiscoveryResult {
        val memory = discoveryCache[section]
        memory?.takeIf { !refresh && isFresh(it.first, 10 * 60_000L) }?.let { return AnimeDiscoveryResult(it.second) }
        // Negative keys are reserved for discovery; all real anime retain positive MAL ids.
        val key = -(section.ordinal + 1)
        val stored = optionalCache { cache.get(key) }
        val saved = stored?.let { optionalCache { json.decodeFromString<List<Anime>>(it.json) } }
        if (!refresh && stored != null && saved != null && isFresh(stored.fetchedAt, 10 * 60_000L)) {
            discoveryCache[section] = stored.fetchedAt to saved
            return AnimeDiscoveryResult(saved)
        }
        return try {
            val anime = catalogRequest(backup = { checkNotNull(alternate).discovery(section) }) {
                when (section) {
                    DiscoverySection.Airing -> api.airing()
                    DiscoverySection.Popular -> api.topAnime(filter = "bypopularity")
                    DiscoverySection.TopRated -> api.topAnime()
                    DiscoverySection.Upcoming -> api.upcoming()
                }.data.mapNotNull { it.toAnime() }.distinctBy { it.id }
            }
            val now = System.currentTimeMillis()
            discoveryCache[section] = now to anime
            optionalCache { cache.cache(CachedAnimeEntity(key, json.encodeToString(anime), now)) }
            AnimeDiscoveryResult(anime)
        } catch (error: CatalogException) {
            (memory?.second ?: saved)?.let { AnimeDiscoveryResult(it, isOffline = true) } ?: throw error
        }
    }

    suspend fun search(query: String, page: Int): AnimePage {
        return catalogRequest(backup = { checkNotNull(alternate).search(query.trim(), page) }) {
            val response = api.search(query.trim(), page)
            AnimePage(response.data.mapNotNull { it.toAnime() }.distinctBy { it.id }, response.pagination.hasNextPage)
        }
    }

    suspend fun detail(id: Int, refresh: Boolean = false): AnimeDetailResult {
        val cached = optionalCache { cache.get(id) }
        val saved = cached?.let { optionalCache { json.decodeFromString<Anime>(it.json) } }
        if (!refresh && cached != null && saved != null && isFresh(cached.fetchedAt, 30 * 60_000L)) {
            return AnimeDetailResult(saved)
        }
        return try {
            val anime = catalogRequest(backup = { checkNotNull(alternate).detail(id) }) {
                api.detail(id).data?.toAnime() ?: throw CatalogException("This anime is unavailable. Please try another title.")
            }
            optionalCache { cache.cache(CachedAnimeEntity(id, json.encodeToString(anime), System.currentTimeMillis())) }
            AnimeDetailResult(anime)
        } catch (error: CatalogException) {
            saved?.let { AnimeDetailResult(it, isOffline = true) } ?: throw error
        }
    }

    suspend fun characters(id: Int): List<AnimeCharacter> {
        characterCache[id]?.let { return it }
        return catalogRequest(backup = { checkNotNull(alternate).characters(id) }) {
            api.characters(id).data.filter { it.character.id > 0 }
                .distinctBy { it.character.id }
                .sortedBy { if (it.role == "Main") 0 else 1 }
                .take(12).map { it.toCharacter() }
        }
            .also { characterCache[id] = it }
    }

    fun retryConnection() { failureUntil = 0L }

    private suspend fun <T> catalogRequest(backup: suspend () -> T, primary: suspend () -> T): T {
        if (alternate == null) return request(primary)
        if (System.currentTimeMillis() < failureUntil) throw lastFailure
        return supervisorScope {
            val backupFirst = System.currentTimeMillis() < preferAlternateUntil
            val main = async { if (backupFirst) delay(2_000); attempt(primary) }
            // AniList currently limits clients to 30 requests/minute.
            val secondary = async { if (!backupFirst) delay(2_000); attempt(backup, 2_100) }
            try {
                val first = select<Pair<Boolean, Result<T>>> {
                    main.onAwait { false to it }
                    secondary.onAwait { true to it }
                }
                val winner = if (first.second.isSuccess) first
                    else if (first.first) false to main.await() else true to secondary.await()
                if (winner.second.isSuccess) {
                    preferAlternateUntil = if (winner.first) System.currentTimeMillis() + 2 * 60_000L else 0L
                    failureUntil = 0L
                    winner.second.getOrThrow()
                } else {
                    val errors = listOfNotNull(first.second.exceptionOrNull(), winner.second.exceptionOrNull())
                    if (errors.all { it is CatalogException && it.isConnectionFailure }) {
                        lastFailure = CatalogException("Cannot reach the anime catalog. Check your connection, then try again.", true)
                        failureUntil = System.currentTimeMillis() + 30_000L
                        throw lastFailure
                    }
                    throw winner.second.exceptionOrNull() ?: lastFailure
                }
            } finally {
                main.cancel()
                secondary.cancel()
            }
        }
    }

    private suspend fun <T> attempt(block: suspend () -> T, intervalMillis: Long = 1_100): Result<T> = try {
        Result.success(request(block, intervalMillis))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    private suspend fun <T> request(block: suspend () -> T, intervalMillis: Long = 1_100): T {
        repeat(2) { attempt ->
            gate.awaitTurn(intervalMillis)
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
                    }, isConnectionFailure = error.code() == 429 || error.code() >= 500)
                }
            } catch (error: SocketTimeoutException) {
                throw CatalogException("The request took too long. Please try again.", true)
            } catch (error: IOException) {
                throw CatalogException("Cannot reach the anime service. Check your internet connection.", true)
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

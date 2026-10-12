package com.dzaky.anitrack.data.backup

import com.dzaky.anitrack.data.AppPreferences
import com.dzaky.anitrack.data.ProfileSnapshot
import com.dzaky.anitrack.data.collectionKey
import com.dzaky.anitrack.data.local.CollectionEntity
import com.dzaky.anitrack.data.local.CollectionAnimeEntity
import com.dzaky.anitrack.data.local.FavoriteEntity
import com.dzaky.anitrack.data.local.PersonalAnimeEntity
import com.dzaky.anitrack.data.local.RecentAnimeEntity
import com.dzaky.anitrack.data.local.SearchHistoryEntity
import com.dzaky.anitrack.data.local.WatchEntry
import com.dzaky.anitrack.domain.ProfileInput
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class BackupDocument(
    val format: String = "AniTrack",
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val favorites: List<FavoriteEntity> = emptyList(),
    val watchlist: List<WatchEntry> = emptyList(),
    val collections: List<CollectionEntity> = emptyList(),
    val members: List<CollectionAnimeEntity> = emptyList(),
    val personal: List<PersonalAnimeEntity> = emptyList(),
    val recent: List<RecentAnimeEntity> = emptyList(),
    val searches: List<SearchHistoryEntity> = emptyList(),
    val profile: ProfileSnapshot = ProfileSnapshot(),
    val preferences: AppPreferences = AppPreferences(),
)

object BackupCodec {
    const val MaxBytes = 10 * 1024 * 1024
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(document: BackupDocument): ByteArray {
        validate(document)
        return json.encodeToString(document).toByteArray(Charsets.UTF_8).also { require(it.size <= MaxBytes) { "Backup exceeds the 10 MB limit." } }
    }

    fun decode(bytes: ByteArray): BackupDocument {
        require(bytes.size <= MaxBytes) { "Backup exceeds the 10 MB limit." }
        val root = json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
        require(root["format"]?.jsonPrimitive?.content == "AniTrack" && root["version"]?.jsonPrimitive?.intOrNull == 1) {
            "This backup format is not supported."
        }
        return json.decodeFromJsonElement<BackupDocument>(root).also(::validate)
    }

    fun readLimited(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            require(out.size().toLong() + count <= MaxBytes) { "Backup exceeds the 10 MB limit." }
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }

    fun validate(d: BackupDocument) {
        require(d.format == "AniTrack" && d.version == 1) { "This backup format is not supported." }
        require(d.createdAt >= 0)
        val counts = listOf(d.favorites.size, d.watchlist.size, d.collections.size, d.members.size,
            d.personal.size, d.recent.size, d.searches.size)
        require(counts.sumOf { it.toLong() } <= 50_000) { "This backup has too many entries." }
        require(d.favorites.distinctBy { it.animeId }.size == d.favorites.size)
        require(d.watchlist.distinctBy { it.animeId }.size == d.watchlist.size)
        require(d.collections.distinctBy { it.id }.size == d.collections.size)
        require(d.collections.distinctBy { it.nameKey }.size == d.collections.size)
        require(d.members.distinctBy { it.collectionId to it.animeId }.size == d.members.size)
        require(d.personal.distinctBy { it.animeId }.size == d.personal.size)
        require(d.recent.distinctBy { it.animeId }.size == d.recent.size)
        require(d.searches.distinctBy { it.query }.size == d.searches.size)
        d.favorites.forEach { checkAnime(it.animeId, it.title, it.posterUrl, it.score, it.year, it.episodes); require(it.addedAt >= 0) }
        d.watchlist.forEach {
            checkAnime(it.animeId, it.title, it.posterUrl, it.score, it.year, it.totalEpisodes)
            require(it.watchedEpisodes >= 0 && (it.totalEpisodes == null || it.watchedEpisodes <= it.totalEpisodes))
            require(it.updatedAt >= 0)
        }
        d.collections.forEach {
            require(it.id.matches(Regex("[A-Za-z0-9_-]{1,80}")) && it.name.length in 1..40)
            require(it.nameKey == collectionKey(it.name) && it.createdAt >= 0)
        }
        val collectionIds = d.collections.map { it.id }.toSet()
        d.members.forEach {
            require(it.collectionId in collectionIds && it.addedAt >= 0)
            checkAnime(it.animeId, it.title, it.posterUrl, it.score, it.year, it.episodes)
        }
        d.personal.forEach {
            require(it.animeId > 0 && it.title.isNotBlank() && it.title.length <= 500 && it.updatedAt >= 0)
            require(it.rating == null || it.rating in 1..10)
            require(it.notes.length <= 5_000)
        }
        d.recent.forEach { checkAnime(it.animeId, it.title, it.posterUrl, it.score, it.year, it.episodes); require(it.viewedAt >= 0) }
        d.searches.forEach { require(it.query.isNotBlank() && it.query.length <= 120 && it.searchedAt >= 0) }
        d.profile.profile?.let {
            require(it.id.matches(Regex("[A-Za-z0-9_-]{1,80}")) && it.createdAt >= 0)
            require(ProfileInput(it.displayName, it.username, it.bio).validationError() == null)
        } ?: require(d.profile.badges.isEmpty())
    }

    private fun checkAnime(id: Int, title: String, poster: String?, score: Double?, year: Int?, episodes: Int?) {
        require(id > 0 && title.isNotBlank() && title.length <= 500)
        require(poster == null || (poster.startsWith("https://") && poster.length <= 2_048))
        require(score == null || (score.isFinite() && score in 0.0..10.0))
        require(year == null || year in 1..9999)
        require(episodes == null || episodes > 0)
    }
}

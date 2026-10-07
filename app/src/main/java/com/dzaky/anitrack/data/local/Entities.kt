package com.dzaky.anitrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.dzaky.anitrack.domain.Anime
import com.dzaky.anitrack.domain.WatchStatus

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val animeId: Int,
    val title: String,
    val posterUrl: String?,
    val score: Double?,
    val type: String?,
    val year: Int?,
    val episodes: Int?,
    val addedAt: Long,
) {
    fun toAnime() = Anime(animeId, title, posterUrl, score, type, year, episodes)
}

@Entity(tableName = "watchlist")
data class WatchEntry(
    @PrimaryKey val animeId: Int,
    val title: String,
    val posterUrl: String?,
    val score: Double?,
    val type: String?,
    val year: Int?,
    val totalEpisodes: Int?,
    val status: WatchStatus,
    val watchedEpisodes: Int,
    val updatedAt: Long,
) {
    fun toAnime() = Anime(animeId, title, posterUrl, score, type, year, totalEpisodes)
}

@Entity(tableName = "recently_viewed")
data class RecentAnimeEntity(
    @PrimaryKey val animeId: Int,
    val title: String,
    val posterUrl: String?,
    val score: Double?,
    val type: String?,
    val year: Int?,
    val episodes: Int?,
    val viewedAt: Long,
) {
    fun toAnime() = Anime(animeId, title, posterUrl, score, type, year, episodes)
}

@Entity(tableName = "search_history")
data class SearchHistoryEntity(@PrimaryKey val query: String, val searchedAt: Long)

@Entity(tableName = "anime_cache")
data class CachedAnimeEntity(@PrimaryKey val animeId: Int, val json: String, val fetchedAt: Long)

class WatchStatusConverter {
    @TypeConverter
    fun fromStatus(status: WatchStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): WatchStatus = WatchStatus.entries.firstOrNull { it.name == value } ?: WatchStatus.Planned
}

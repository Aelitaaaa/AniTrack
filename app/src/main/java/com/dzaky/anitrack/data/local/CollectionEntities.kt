package com.dzaky.anitrack.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dzaky.anitrack.domain.Anime
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "collections", indices = [Index(value = ["nameKey"], unique = true)])
data class CollectionEntity(@PrimaryKey val id: String, val name: String, val nameKey: String, val createdAt: Long)

@Serializable
@Entity(
    tableName = "collection_anime",
    primaryKeys = ["collectionId", "animeId"],
    foreignKeys = [ForeignKey(entity = CollectionEntity::class, parentColumns = ["id"],
        childColumns = ["collectionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("collectionId"), Index("animeId")],
)
data class CollectionAnimeEntity(
    val collectionId: String, val animeId: Int, val title: String, val posterUrl: String?,
    val score: Double?, val type: String?, val year: Int?, val episodes: Int?, val addedAt: Long,
) {
    fun toAnime() = Anime(animeId, title, posterUrl, score, type, year, episodes)
}

@Serializable
@Entity(tableName = "personal_anime")
data class PersonalAnimeEntity(@PrimaryKey val animeId: Int, val title: String, val rating: Int?, val notes: String, val updatedAt: Long)

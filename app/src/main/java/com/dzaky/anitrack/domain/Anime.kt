package com.dzaky.anitrack.domain

import kotlinx.serialization.Serializable

@Serializable
data class Anime(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val score: Double? = null,
    val type: String? = null,
    val year: Int? = null,
    val episodes: Int? = null,
    val originalTitle: String? = null,
    val japaneseTitle: String? = null,
    val synopsis: String? = null,
    val status: String? = null,
    val rating: String? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val members: Int? = null,
    val aired: String? = null,
    val duration: String? = null,
    val season: String? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val producers: List<String> = emptyList(),
    val trailerUrl: String? = null,
    val relations: List<RelatedAnime> = emptyList(),
)

@Serializable
data class RelatedAnime(val id: Int, val title: String, val relation: String)

data class AnimeCharacter(val id: Int, val name: String, val imageUrl: String?, val role: String)

data class AnimePage(val anime: List<Anime>, val hasNextPage: Boolean)

enum class DiscoverySection(val title: String) {
    Airing("Currently airing"),
    Popular("Popular"),
    TopRated("Top rated"),
    Upcoming("Upcoming"),
}

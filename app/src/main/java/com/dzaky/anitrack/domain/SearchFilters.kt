package com.dzaky.anitrack.domain

import kotlinx.serialization.Serializable

@Serializable
enum class AnimeFormat(val label: String, val jikan: String, val aniList: String) {
    TV("TV", "tv", "TV"), Movie("Movie", "movie", "MOVIE"),
    OVA("OVA", "ova", "OVA"), ONA("ONA", "ona", "ONA"),
}

@Serializable
enum class AiringStatus(val label: String, val jikan: String, val aniList: String) {
    Airing("Airing", "airing", "RELEASING"),
    Finished("Finished", "complete", "FINISHED"),
    Upcoming("Upcoming", "upcoming", "NOT_YET_RELEASED"),
}

@Serializable
enum class AnimeGenre(val label: String, val malId: Int) {
    Action("Action", 1), Adventure("Adventure", 2), Comedy("Comedy", 4),
    Drama("Drama", 8), Fantasy("Fantasy", 10), Romance("Romance", 22),
}

@Serializable
enum class CatalogSort(val label: String, val jikan: String?, val direction: String?, val aniList: String) {
    Relevance("Relevance", null, null, "SEARCH_MATCH"),
    Score("Highest score", "score", "desc", "SCORE_DESC"),
    Popularity("Most popular", "members", "desc", "POPULARITY_DESC"),
    Title("Title A–Z", "title", "asc", "TITLE_ROMAJI"),
}

@Serializable
data class SearchFilters(
    val format: AnimeFormat? = null,
    val status: AiringStatus? = null,
    val genre: AnimeGenre? = null,
    val sort: CatalogSort = CatalogSort.Relevance,
) {
    val isDefault get() = this == SearchFilters()
    val count get() = listOf(format, status, genre).count { it != null } + if (sort == CatalogSort.Relevance) 0 else 1
}

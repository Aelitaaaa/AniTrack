package com.dzaky.anitrack.data.remote

import com.dzaky.anitrack.domain.Anime
import com.dzaky.anitrack.domain.AnimeCharacter
import com.dzaky.anitrack.domain.RelatedAnime
import java.net.URI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeListResponse(
    val data: List<AnimeDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
)

@Serializable
data class PaginationDto(@SerialName("has_next_page") val hasNextPage: Boolean = false)

@Serializable
data class AnimeResponse(val data: AnimeDto? = null)

@Serializable
data class AnimeDto(
    @SerialName("mal_id") val id: Int = 0,
    val title: String? = null,
    @SerialName("title_english") val englishTitle: String? = null,
    @SerialName("title_japanese") val japaneseTitle: String? = null,
    val images: ImagesDto = ImagesDto(),
    val score: Double? = null,
    val type: String? = null,
    val year: Int? = null,
    val episodes: Int? = null,
    val synopsis: String? = null,
    val status: String? = null,
    val rating: String? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val members: Int? = null,
    val aired: AiredDto = AiredDto(),
    val duration: String? = null,
    val season: String? = null,
    val genres: List<NamedDto> = emptyList(),
    val studios: List<NamedDto> = emptyList(),
    val producers: List<NamedDto> = emptyList(),
    val trailer: TrailerDto = TrailerDto(),
    val relations: List<RelationDto> = emptyList(),
)

@Serializable
data class ImagesDto(val jpg: ImageDto = ImageDto(), val webp: ImageDto = ImageDto()) {
    fun url(): String? = listOf(webp.large, jpg.large, webp.normal, jpg.normal)
        .firstOrNull { !it.isNullOrBlank() }
}

@Serializable
data class ImageDto(
    @SerialName("image_url") val normal: String? = null,
    @SerialName("large_image_url") val large: String? = null,
)

@Serializable
data class NamedDto(@SerialName("mal_id") val id: Int = 0, val name: String = "", val type: String? = null)

@Serializable
data class AiredDto(val string: String? = null)

@Serializable
data class TrailerDto(val url: String? = null, @SerialName("youtube_id") val youtubeId: String? = null)

@Serializable
data class RelationDto(val relation: String = "Related", val entry: List<NamedDto> = emptyList())

@Serializable
data class CharactersResponse(val data: List<CharacterEntryDto> = emptyList())

@Serializable
data class CharacterEntryDto(val character: CharacterDto, val role: String = "")

@Serializable
data class CharacterDto(
    @SerialName("mal_id") val id: Int,
    val name: String,
    val images: ImagesDto = ImagesDto(),
)

fun AnimeDto.toAnime(): Anime? {
    val displayTitle = englishTitle.clean() ?: title.clean() ?: return null
    if (id <= 0) return null
    return Anime(
        id = id,
        title = displayTitle,
        posterUrl = images.url(),
        score = score?.takeIf { it in 0.0..10.0 },
        type = type.clean(),
        year = year?.takeIf { it > 0 },
        episodes = episodes?.takeIf { it > 0 },
        originalTitle = title.clean()?.takeUnless { it == displayTitle },
        japaneseTitle = japaneseTitle.clean(),
        synopsis = synopsis.clean(),
        status = status.clean(),
        rating = rating.clean(),
        rank = rank?.takeIf { it > 0 },
        popularity = popularity?.takeIf { it > 0 },
        members = members?.takeIf { it > 0 },
        aired = aired.string.clean(),
        duration = duration.clean(),
        season = season.clean(),
        genres = genres.mapNotNull { it.name.clean() }.distinct(),
        studios = studios.mapNotNull { it.name.clean() }.distinct(),
        producers = producers.mapNotNull { it.name.clean() }.distinct(),
        trailerUrl = trailer.watchUrl(),
        relations = relations.flatMap { relation ->
            relation.entry.filter { it.type == "anime" && it.id > 0 && it.id != id }
                .map { RelatedAnime(it.id, it.name, relation.relation) }
        }.distinctBy { it.id },
    )
}

fun CharacterEntryDto.toCharacter() = AnimeCharacter(character.id, character.name, character.images.url(), role)

private fun String?.clean(): String? = this?.trim()?.takeUnless { it.isEmpty() || it == "Unknown" }

private fun TrailerDto.watchUrl(): String? {
    val address = url?.let { runCatching { URI(it) }.getOrNull() }
    if (address?.scheme == "https" && address.userInfo == null &&
        address.host in setOf("youtube.com", "www.youtube.com", "youtu.be") && address.port in setOf(-1, 443)) {
        return url
    }
    return youtubeId?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
        ?.let { "https://www.youtube.com/watch?v=$it" }
}

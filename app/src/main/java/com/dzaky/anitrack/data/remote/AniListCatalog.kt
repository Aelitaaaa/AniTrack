package com.dzaky.anitrack.data.remote

import com.dzaky.anitrack.data.CatalogException
import com.dzaky.anitrack.domain.Anime
import com.dzaky.anitrack.domain.AnimeCharacter
import com.dzaky.anitrack.domain.AnimePage
import com.dzaky.anitrack.domain.DiscoverySection
import com.dzaky.anitrack.domain.RelatedAnime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put

@Singleton
class AniListCatalog @Inject constructor(private val api: AniListApi) {
    suspend fun discovery(section: DiscoverySection): List<Anime> {
        val variables = buildJsonObject {
            put("page", 1)
            put("perPage", 14)
            put("sort", buildJsonArray { add(if (section == DiscoverySection.TopRated) "SCORE_DESC" else "POPULARITY_DESC") })
            when (section) {
                DiscoverySection.Airing -> put("status", "RELEASING")
                DiscoverySection.Upcoming -> put("status", "NOT_YET_RELEASED")
                else -> Unit
            }
        }
        return fetchPage(AniListRequest(PageQuery, variables)).media.mapNotNull { it.toAnime() }.distinctBy { it.id }
    }

    suspend fun search(query: String, page: Int): AnimePage {
        val result = fetchPage(AniListRequest(PageQuery, buildJsonObject {
            put("page", page)
            put("perPage", 25)
            put("sort", buildJsonArray { add("SEARCH_MATCH") })
            put("search", query)
        }))
        return AnimePage(result.media.mapNotNull { it.toAnime() }.distinctBy { it.id }, result.pageInfo.hasNextPage)
    }

    suspend fun detail(id: Int): Anime = media(id, DetailQuery).toAnime()
        ?: throw CatalogException("This anime could not be found.")

    suspend fun characters(id: Int): List<AnimeCharacter> = media(id, CharactersQuery).characters.edges
        .filter { it.node.id > 0 && it.node.name.full.isNotBlank() }
        .distinctBy { it.node.id }
        .sortedBy { if (it.role == "MAIN") 0 else 1 }
        .take(12)
        .map { AnimeCharacter(it.node.id, it.node.name.full, it.node.image.large, it.role.displayLabel()) }

    private suspend fun fetchPage(request: AniListRequest): AniListPage = checked(api.query(request)).page
        ?: throw CatalogException("The anime service returned unexpected data. Please try again later.")

    private suspend fun media(id: Int, query: String): AniListMedia = checked(api.query(AniListRequest(query,
        buildJsonObject { put("idMal", id) }))).media
        ?: throw CatalogException("This anime could not be found.")

    private fun checked(response: AniListResponse): AniListData {
        if (response.errors.isNotEmpty()) throw CatalogException("The anime service is unavailable. Please try again.")
        return response.data ?: throw CatalogException("The anime service returned unexpected data. Please try again later.")
    }

    companion object {
        private const val Cards = "idMal title { english romaji native } coverImage { extraLarge large } averageScore format status seasonYear episodes"
        val PageQuery = """
            query Catalog(${'$'}page: Int!, ${'$'}perPage: Int!, ${'$'}sort: [MediaSort], ${'$'}status: MediaStatus, ${'$'}search: String) {
                Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                    pageInfo { hasNextPage }
                    media(type: ANIME, isAdult: false, sort: ${'$'}sort, status: ${'$'}status, search: ${'$'}search) { $Cards }
                }
            }
        """.trimIndent()
        val DetailQuery = """
            query Details(${'$'}idMal: Int!) {
                Media(idMal: ${'$'}idMal, type: ANIME) {
                    $Cards
                    description(asHtml: false) duration season genres
                    studios(isMain: true) { nodes { name } }
                    trailer { id site }
                    relations { edges { relationType node { idMal title { english romaji } } } }
                }
            }
        """.trimIndent()
        val CharactersQuery = """
            query Characters(${'$'}idMal: Int!) {
                Media(idMal: ${'$'}idMal, type: ANIME) {
                    idMal
                    characters(perPage: 12, sort: ROLE) { edges { role node { id name { full } image { large } } } }
                }
            }
        """.trimIndent()
    }
}

fun AniListMedia.toAnime(): Anime? {
    // Preserve MAL identifiers so switching catalog providers cannot corrupt saved lists.
    val id = idMal?.takeIf { it > 0 } ?: return null
    val name = title.english.nonBlank() ?: title.romaji.nonBlank() ?: title.nativeTitle.nonBlank() ?: return null
    return Anime(
        id = id,
        title = name,
        posterUrl = coverImage.extraLarge.nonBlank() ?: coverImage.large.nonBlank(),
        score = averageScore?.takeIf { it in 1..100 }?.div(10.0),
        type = when (format) { "TV" -> "TV"; "TV_SHORT" -> "TV Short"; "OVA" -> "OVA"; "ONA" -> "ONA"; else -> format?.displayLabel() },
        year = seasonYear?.takeIf { it > 0 },
        episodes = episodes?.takeIf { it > 0 },
        originalTitle = title.romaji.nonBlank()?.takeUnless { it == name },
        japaneseTitle = title.nativeTitle.nonBlank(),
        synopsis = description?.replace(Regex("<[^>]*>"), "")?.trim().nonBlank(),
        status = when (status) {
            "RELEASING" -> "Currently Airing"
            "NOT_YET_RELEASED" -> "Not yet aired"
            "FINISHED" -> "Finished Airing"
            else -> status?.displayLabel()
        },
        duration = duration?.takeIf { it > 0 }?.let { "$it min per episode" },
        season = season?.displayLabel(),
        genres = genres.filter { it.isNotBlank() }.distinct(),
        studios = studios.nodes.map { it.name }.filter { it.isNotBlank() }.distinct(),
        trailerUrl = trailer?.takeIf { it.site == "youtube" && it.id?.matches(Regex("[A-Za-z0-9_-]{11}")) == true }
            ?.let { "https://www.youtube.com/watch?v=${it.id}" },
        relations = relations.edges.mapNotNull { edge ->
            val relatedId = edge.node.idMal?.takeIf { it > 0 && it != id } ?: return@mapNotNull null
            val relatedTitle = edge.node.title.english.nonBlank() ?: edge.node.title.romaji.nonBlank() ?: return@mapNotNull null
            RelatedAnime(relatedId, relatedTitle, edge.relationType.displayLabel())
        }.distinctBy { it.id },
    )
}

private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
private fun String.displayLabel() = lowercase(Locale.ROOT).replace('_', ' ').replaceFirstChar { it.titlecase(Locale.ROOT) }

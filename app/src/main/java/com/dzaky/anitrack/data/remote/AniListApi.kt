package com.dzaky.anitrack.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST

interface AniListApi {
    @POST(".")
    suspend fun query(@Body request: AniListRequest): AniListResponse
}

@Serializable
data class AniListRequest(val query: String, val variables: JsonObject)

@Serializable
data class AniListResponse(val data: AniListData? = null, val errors: List<AniListError> = emptyList())

@Serializable
data class AniListError(val message: String = "")

@Serializable
data class AniListData(
    @SerialName("Page") val page: AniListPage? = null,
    @SerialName("Media") val media: AniListMedia? = null,
)

@Serializable
data class AniListPage(val media: List<AniListMedia> = emptyList(), val pageInfo: AniListPageInfo = AniListPageInfo())

@Serializable
data class AniListPageInfo(val hasNextPage: Boolean = false)

@Serializable
data class AniListMedia(
    val idMal: Int? = null,
    val title: AniListTitle = AniListTitle(),
    val coverImage: AniListImage = AniListImage(),
    val averageScore: Int? = null,
    val format: String? = null,
    val status: String? = null,
    val seasonYear: Int? = null,
    val episodes: Int? = null,
    val description: String? = null,
    val duration: Int? = null,
    val season: String? = null,
    val genres: List<String> = emptyList(),
    val studios: AniListStudios = AniListStudios(),
    val trailer: AniListTrailer? = null,
    val relations: AniListRelations = AniListRelations(),
    val characters: AniListCharacters = AniListCharacters(),
)

@Serializable
data class AniListTitle(val english: String? = null, val romaji: String? = null, @SerialName("native") val nativeTitle: String? = null)

@Serializable
data class AniListImage(val extraLarge: String? = null, val large: String? = null)

@Serializable
data class AniListStudios(val nodes: List<AniListStudio> = emptyList())

@Serializable
data class AniListStudio(val name: String = "")

@Serializable
data class AniListTrailer(val id: String? = null, val site: String? = null)

@Serializable
data class AniListRelations(val edges: List<AniListRelation> = emptyList())

@Serializable
data class AniListRelation(val relationType: String = "RELATED", val node: AniListMedia = AniListMedia())

@Serializable
data class AniListCharacters(val edges: List<AniListCharacterEdge> = emptyList())

@Serializable
data class AniListCharacterEdge(val role: String = "", val node: AniListCharacter = AniListCharacter())

@Serializable
data class AniListCharacter(val id: Int = 0, val name: AniListCharacterName = AniListCharacterName(), val image: AniListImage = AniListImage())

@Serializable
data class AniListCharacterName(val full: String = "")

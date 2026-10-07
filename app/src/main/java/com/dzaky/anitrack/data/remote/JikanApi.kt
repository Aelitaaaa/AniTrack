package com.dzaky.anitrack.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JikanApi {
    @GET("top/anime")
    suspend fun topAnime(
        @Query("filter") filter: String? = null,
        @Query("limit") limit: Int = 14,
        @Query("sfw") safeForWork: Boolean = true,
    ): AnimeListResponse

    @GET("seasons/now")
    suspend fun airing(@Query("limit") limit: Int = 14, @Query("sfw") safeForWork: Boolean = true): AnimeListResponse

    @GET("seasons/upcoming")
    suspend fun upcoming(@Query("limit") limit: Int = 14, @Query("sfw") safeForWork: Boolean = true): AnimeListResponse

    @GET("anime")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 25,
        @Query("sfw") safeForWork: Boolean = true,
    ): AnimeListResponse

    @GET("anime/{id}/full")
    suspend fun detail(@Path("id") id: Int): AnimeResponse

    @GET("anime/{id}/characters")
    suspend fun characters(@Path("id") id: Int): CharactersResponse
}

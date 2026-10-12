package com.dzaky.anitrack.data

import com.dzaky.anitrack.data.remote.AniListCatalog
import com.dzaky.anitrack.di.AppModule
import com.dzaky.anitrack.domain.AiringStatus
import com.dzaky.anitrack.domain.AnimeFormat
import com.dzaky.anitrack.domain.AnimeGenre
import com.dzaky.anitrack.domain.CatalogSort
import com.dzaky.anitrack.domain.SearchFilters
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import com.dzaky.anitrack.data.remote.JikanApi
import com.dzaky.anitrack.data.remote.AniListApi

class CatalogFiltersTest {
    private lateinit var server: MockWebServer
    private val json = AppModule.json()
    @Before fun setup() { server = MockWebServer().also { it.start() } }
    @After fun close() { server.close() }
    private inline fun <reified T> api() = Retrofit.Builder().baseUrl(server.url("/"))
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(T::class.java)
    private fun respond(body: String) { server.enqueue(MockResponse.Builder().addHeader("Content-Type", "application/json").body(body).build()) }

    @Test fun jikanReceivesFormatStatusGenreSortAndSafeSearch() = runBlocking {
        respond("""{"data":[]}""")
        api<JikanApi>().filteredSearch("Naruto", 2, "tv", "complete", "1", "score", "desc")
        val request = server.takeRequest().toString()
        listOf("type=tv", "status=complete", "genres=1", "order_by=score", "sort=desc", "sfw=true", "page=2").forEach { assertTrue(request, request.contains(it)) }
    }

    @Test fun aniListReceivesEquivalentFiltersAndSupportsBrowsingWithoutTitle() = runBlocking {
        respond("""{"data":{"Page":{"media":[]}}}""")
        AniListCatalog(api<AniListApi>()).search("", 1, SearchFilters(AnimeFormat.TV, AiringStatus.Finished, AnimeGenre.Action, CatalogSort.Score))
        val body = json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        val variables = body.getValue("variables").jsonObject
        assertEquals("TV", variables.getValue("format").jsonPrimitive.content)
        assertEquals("FINISHED", variables.getValue("status").jsonPrimitive.content)
        assertEquals("Action", variables.getValue("genre").jsonPrimitive.content)
        assertEquals("SCORE_DESC", variables.getValue("sort").jsonArray.single().jsonPrimitive.content)
        assertFalse(variables.containsKey("search"))
        assertTrue(body.getValue("query").jsonPrimitive.content.contains("isAdult: false"))
    }
}

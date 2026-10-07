package com.dzaky.anitrack.data

import com.dzaky.anitrack.data.remote.AniListApi
import com.dzaky.anitrack.data.remote.AniListCatalog
import com.dzaky.anitrack.data.remote.AniListMedia
import com.dzaky.anitrack.data.remote.AniListTitle
import com.dzaky.anitrack.data.remote.toAnime
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AniListCatalogTest {
    private lateinit var server: MockWebServer
    private lateinit var catalog: AniListCatalog
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        catalog = AniListCatalog(Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(AniListApi::class.java))
    }
    @After fun tearDown() { server.close() }

    @Test fun `search sends GraphQL variables and preserves pagination`() = runBlocking {
        respond("""{"data":{"Page":{"pageInfo":{"hasNextPage":true},"media":[{"idMal":20,"title":{"english":"Naruto"},"averageScore":79},{"idMal":null,"title":{"romaji":"Unmapped"}}]}}}""")
        val page = catalog.search("Naruto", 2)
        assertTrue(page.hasNextPage)
        assertEquals(20, page.anime.single().id)
        assertEquals(7.9, page.anime.single().score!!, 0.001)
        val body = json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        assertEquals("Naruto", body.getValue("variables").jsonObject.getValue("search").jsonPrimitive.content)
        assertEquals("2", body.getValue("variables").jsonObject.getValue("page").jsonPrimitive.content)
        assertTrue(body.getValue("query").jsonPrimitive.content.contains("isAdult: false"))
    }

    @Test fun `details map related MAL ids and valid trailers`() = runBlocking {
        respond("""{"data":{"Media":{"idMal":20,"title":{"romaji":"Naruto"},"description":"A <b>story</b>","trailer":{"id":"abcdefghijk","site":"youtube"},"relations":{"edges":[{"relationType":"SEQUEL","node":{"idMal":1735,"title":{"english":"Shippuden"}}},{"node":{"idMal":null,"title":{"romaji":"Unmapped"}}}]}}}}""")
        val detail = catalog.detail(20)
        assertEquals("A story", detail.synopsis)
        assertEquals(1735, detail.relations.single().id)
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", detail.trailerUrl)
        val body = json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        assertEquals("20", body.getValue("variables").jsonObject.getValue("idMal").jsonPrimitive.content)
    }

    @Test fun `GraphQL failures are not mistaken for an empty catalog`() = runBlocking {
        respond("""{"errors":[{"message":"Internal query details"}],"data":null}""")
        val error = runCatching { catalog.search("title", 1) }.exceptionOrNull()
        assertTrue(error is CatalogException)
        assertFalse(error!!.message!!.contains("Internal query details"))
    }

    @Test fun `entries without a usable MAL id never enter saved lists`() {
        assertNull(AniListMedia(idMal = null, title = AniListTitle(english = "Unknown")).toAnime())
        assertNull(AniListMedia(idMal = 0, title = AniListTitle(english = "Unknown")).toAnime())
    }

    private fun respond(body: String) { server.enqueue(MockResponse.Builder().addHeader("Content-Type", "application/json").body(body).build()) }
}

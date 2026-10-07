package com.dzaky.anitrack.data

import com.dzaky.anitrack.data.remote.AnimeDto
import com.dzaky.anitrack.data.remote.AnimeResponse
import com.dzaky.anitrack.data.remote.ImageDto
import com.dzaky.anitrack.data.remote.ImagesDto
import com.dzaky.anitrack.data.remote.NamedDto
import com.dzaky.anitrack.data.remote.RelationDto
import com.dzaky.anitrack.data.remote.TrailerDto
import com.dzaky.anitrack.data.remote.toAnime
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JikanMapperTest {
    @Test fun `English title is preferred and original is preserved`() {
        val anime = AnimeDto(id = 1, title = "Original", englishTitle = "English").toAnime()!!
        assertEquals("English", anime.title)
        assertEquals("Original", anime.originalTitle)
    }

    @Test fun `blank English title falls back to original`() {
        assertEquals("Original", AnimeDto(id = 1, title = "Original", englishTitle = " ").toAnime()!!.title)
    }

    @Test fun `invalid ids and untitled entries are discarded`() {
        assertNull(AnimeDto(id = 0, title = "Invalid").toAnime())
        assertNull(AnimeDto(id = 1).toAnime())
    }

    @Test fun `missing and unknown fields remain absent`() {
        val anime = AnimeDto(id = 1, title = "Title", episodes = 0, status = "Unknown", duration = "").toAnime()!!
        assertNull(anime.episodes)
        assertNull(anime.status)
        assertNull(anime.duration)
        assertNull(anime.score)
    }

    @Test fun `poster prefers a large webp and falls back to jpg`() {
        val images = ImagesDto(jpg = ImageDto(large = "jpg"), webp = ImageDto(large = "webp"))
        assertEquals("webp", images.url())
        assertEquals("jpg", images.copy(webp = ImageDto()).url())
    }

    @Test fun `relations only navigate to distinct anime ids`() {
        val anime = AnimeDto(id = 1, title = "Title", relations = listOf(
            RelationDto("Sequel", listOf(NamedDto(2, "Sequel", "anime"), NamedDto(9, "Manga", "manga"), NamedDto(1, "Self", "anime"))),
            RelationDto("Other", listOf(NamedDto(2, "Sequel", "anime"))),
        )).toAnime()!!
        assertEquals(listOf(2), anime.relations.map { it.id })
    }

    @Test fun `null API fields and extra fields decode safely`() {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        val anime = json.decodeFromString<AnimeResponse>("""{"data":{"mal_id":1,"title":"Title","images":null,"episodes":null,"genres":null,"unrecognized":true}}""").data!!.toAnime()!!
        assertTrue(anime.genres.isEmpty())
        assertNull(anime.posterUrl)
    }

    @Test fun `out of range scores are omitted`() {
        assertNull(AnimeDto(id = 1, title = "Title", score = 11.0).toAnime()!!.score)
    }

    @Test fun `trailer URLs only open HTTPS YouTube links`() {
        val anime = AnimeDto(id = 1, title = "Title", trailer = TrailerDto(url = "https://example.org/trailer"))
        assertNull(anime.toAnime()!!.trailerUrl)
        assertEquals("https://www.youtube.com/watch?v=abcDEF_123-",
            anime.copy(trailer = TrailerDto(youtubeId = "abcDEF_123-")).toAnime()!!.trailerUrl)
        assertNull(anime.copy(trailer = TrailerDto(youtubeId = "invalid&id=bad")).toAnime()!!.trailerUrl)
    }
}

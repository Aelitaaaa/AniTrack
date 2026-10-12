package com.dzaky.anitrack.data

import androidx.room.withTransaction
import com.dzaky.anitrack.data.local.AniTrackDatabase
import com.dzaky.anitrack.data.local.CollectionEntity
import com.dzaky.anitrack.data.local.CollectionAnimeEntity
import com.dzaky.anitrack.data.local.PersonalAnimeEntity
import com.dzaky.anitrack.domain.Anime
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

fun collectionName(value: String) = value.trim().replace(Regex("\\s+"), " ")
fun collectionKey(value: String) = collectionName(value).lowercase(Locale.ROOT)

@Singleton
class ExtrasRepository @Inject constructor(private val database: AniTrackDatabase) {
    private val dao = database.extras()
    val collections = dao.observeCollections()
    val members = dao.observeMembers()
    val personal = dao.observePersonal()
    fun personal(id: Int) = dao.observePersonal(id)

    suspend fun createCollection(name: String): String = database.withTransaction {
        val display = checkedName(name)
        require(dao.byName(collectionKey(display)) == null) { "A collection with this name already exists." }
        val id = UUID.randomUUID().toString()
        dao.saveCollection(CollectionEntity(id, display, collectionKey(display), System.currentTimeMillis()))
        id
    }

    suspend fun renameCollection(id: String, name: String) = database.withTransaction {
        val display = checkedName(name)
        val existing = dao.collection(id) ?: return@withTransaction
        require(dao.byName(collectionKey(display))?.id.let { it == null || it == id }) { "A collection with this name already exists." }
        dao.saveCollection(existing.copy(name = display, nameKey = collectionKey(display)))
    }
    suspend fun deleteCollection(id: String) = dao.deleteCollection(id)
    suspend fun removeMember(collectionId: String, id: Int) = dao.removeMember(collectionId, id)

    suspend fun setMembership(collectionId: String, anime: Anime, included: Boolean) {
        if (included) dao.saveMember(CollectionAnimeEntity(collectionId, anime.id, anime.title, anime.posterUrl,
            anime.score, anime.type, anime.year, anime.episodes, System.currentTimeMillis()))
        else dao.removeMember(collectionId, anime.id)
    }

    suspend fun savePersonal(anime: Anime, rating: Int?, notes: String) {
        require(rating == null || rating in 1..10) { "Choose a rating from 1 to 10." }
        require(notes.length <= 5_000) { "Keep your notes within 5,000 characters." }
        if (rating == null && notes.isBlank()) dao.removePersonal(anime.id)
        else dao.savePersonal(PersonalAnimeEntity(anime.id, anime.title, rating, notes.trim(), System.currentTimeMillis()))
    }

    private fun checkedName(value: String) = collectionName(value).also {
        require(it.length in 1..40) { "Use a collection name with 1–40 characters." }
    }
}

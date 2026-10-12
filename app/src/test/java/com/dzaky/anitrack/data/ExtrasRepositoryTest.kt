package com.dzaky.anitrack.data

import android.app.Application
import androidx.room.Room
import com.dzaky.anitrack.data.local.AniTrackDatabase
import com.dzaky.anitrack.data.local.FavoriteEntity
import com.dzaky.anitrack.domain.Anime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ExtrasRepositoryTest {
    private lateinit var db: AniTrackDatabase
    private lateinit var extras: ExtrasRepository
    private val anime = Anime(1, "Title", null)

    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AniTrackDatabase::class.java).build(); extras = ExtrasRepository(db) }
    @After fun close() { db.close() }

    @Test fun collectionsAreUniqueAndMembershipsCascadeWithoutRemovingFavorites() = runBlocking {
        val first = extras.createCollection(" Weekend ")
        val second = extras.createCollection("Rewatch")
        assertThrows(IllegalArgumentException::class.java) { runBlocking { extras.createCollection("WEEKEND") } }
        extras.setMembership(first, anime, true)
        extras.setMembership(first, anime, true)
        extras.setMembership(second, anime, true)
        db.favorites().save(FavoriteEntity(1, "Title", null, null, null, null, null, 1))
        assertEquals(2, db.extras().members().size)
        extras.renameCollection(first, " Saturday  shows ")
        assertEquals("Saturday shows", db.extras().collection(first)!!.name)
        extras.deleteCollection(first)
        assertEquals(second, db.extras().members().single().collectionId)
        assertEquals(1, db.favorites().snapshot().size)
    }

    @Test fun personalRatingsAndNotesCanBeSavedAndClearedIndependently() = runBlocking {
        extras.savePersonal(anime, 8, "猫\nRewatch soon")
        assertEquals("猫\nRewatch soon", db.extras().personal(1)!!.notes)
        assertEquals(8, db.extras().personal(1)!!.rating)
        assertThrows(IllegalArgumentException::class.java) { runBlocking { extras.savePersonal(anime, 11, "") } }
        extras.savePersonal(anime, null, "")
        assertNull(db.extras().personal(1))
    }
}

package com.dzaky.anitrack.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.dzaky.anitrack.data.local.AniTrackDatabase
import com.dzaky.anitrack.data.local.MIGRATION_1_2
import com.dzaky.anitrack.domain.Anime
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class DatabaseMigrationTest {
    @Test fun versionOneLibrarySurvivesUpgradeAndNewTablesPersist() = runBlocking {
        val context = RuntimeEnvironment.getApplication() as Context
        val name = "migration-" + System.nanoTime() + ".db"
        val legacy = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        val schema = checkNotNull(javaClass.classLoader?.getResourceAsStream("com.dzaky.anitrack.data.local.AniTrackDatabase/1.json"))
            .bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject.getValue("database").jsonObject }
        schema.getValue("entities").jsonArray.forEach {
            val entity = it.jsonObject
            legacy.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("${'$'}{TABLE_NAME}", entity.getValue("tableName").jsonPrimitive.content))
        }
        schema.getValue("setupQueries").jsonArray.forEach { legacy.execSQL(it.jsonPrimitive.content) }
        legacy.execSQL("INSERT INTO favorites VALUES (1, 'Old title', NULL, 8.0, 'TV', 2020, 12, 10)")
        legacy.execSQL("INSERT INTO watchlist VALUES (1, 'Old title', NULL, 8.0, 'TV', 2020, 12, 'Watching', 4, 11)")
        legacy.execSQL("INSERT INTO search_history VALUES ('old search', 12)")
        legacy.execSQL("INSERT INTO anime_cache VALUES (1, '{}', 13)")
        legacy.version = 1
        legacy.close()
        var database = Room.databaseBuilder(context, AniTrackDatabase::class.java, name).addMigrations(MIGRATION_1_2).build()
        try {
            assertEquals("Old title", database.favorites().get(1)!!.title)
            assertEquals(4, database.watchlist().get(1)!!.watchedEpisodes)
            assertEquals("old search", database.history().searchSnapshot().single().query)
            assertNotNull(database.animeCache().get(1))
            val extras = ExtrasRepository(database)
            val id = extras.createCollection("Weekend")
            extras.setMembership(id, Anime(1, "Old title", null), true)
            extras.savePersonal(Anime(1, "Old title", null), 9, "Rewatch")
            database.close()
            database = Room.databaseBuilder(context, AniTrackDatabase::class.java, name).addMigrations(MIGRATION_1_2).build()
            assertEquals(1, database.extras().members().size)
            assertEquals("Rewatch", database.extras().personal(1)!!.notes)
            assertEquals(4, database.watchlist().get(1)!!.watchedEpisodes)
        } finally { database.close(); context.deleteDatabase(name) }
    }
}

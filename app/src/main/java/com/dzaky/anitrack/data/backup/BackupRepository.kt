package com.dzaky.anitrack.data.backup

import androidx.room.withTransaction
import com.dzaky.anitrack.data.ProfileRepository
import com.dzaky.anitrack.data.SettingsRepository
import com.dzaky.anitrack.data.local.AniTrackDatabase
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

data class RestoreResult(val warnings: List<String> = emptyList())

@Singleton
class BackupRepository @Inject constructor(
    private val database: AniTrackDatabase,
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository,
) {
    suspend fun export(): ByteArray {
        val document = database.withTransaction {
            val extras = database.extras()
            BackupDocument(
                favorites = database.favorites().snapshot(), watchlist = database.watchlist().snapshot(),
                collections = extras.collections(), members = extras.members(), personal = extras.personal(),
                recent = database.history().recentSnapshot(), searches = database.history().searchSnapshot(),
                profile = profiles.snapshot(), preferences = settings.preferences.first(),
            )
        }
        return BackupCodec.encode(document)
    }

    suspend fun restore(bytes: ByteArray): RestoreResult {
        // Validate every section before the first write.
        val d = BackupCodec.decode(bytes)
        database.withTransaction {
            val favorites = database.favorites()
            val watchlist = database.watchlist()
            val extras = database.extras()
            d.favorites.forEach { incoming ->
                val old = favorites.get(incoming.animeId)
                if (old == null || old.addedAt < incoming.addedAt) favorites.save(incoming)
            }
            d.watchlist.forEach { incoming ->
                val old = watchlist.get(incoming.animeId)
                if (old == null || old.updatedAt < incoming.updatedAt) watchlist.save(incoming)
            }
            val aliases = mutableMapOf<String, String>()
            d.collections.forEach { incoming ->
                val existing = extras.byName(incoming.nameKey)
                val id = existing?.id ?: if (extras.collection(incoming.id) == null) incoming.id else UUID.randomUUID().toString()
                aliases[incoming.id] = id
                if (existing == null) extras.saveCollection(incoming.copy(id = id))
            }
            val currentMembers = extras.members().associateBy { it.collectionId to it.animeId }
            d.members.forEach { incoming ->
                val id = checkNotNull(aliases[incoming.collectionId])
                val old = currentMembers[id to incoming.animeId]
                if (old == null || old.addedAt < incoming.addedAt) extras.saveMember(incoming.copy(collectionId = id))
            }
            d.personal.forEach { incoming ->
                val old = extras.personal(incoming.animeId)
                if (old == null || old.updatedAt < incoming.updatedAt) extras.savePersonal(incoming)
            }
            val history = database.history()
            val recent = history.recentSnapshot().associateBy { it.animeId }
            d.recent.forEach { if ((recent[it.animeId]?.viewedAt ?: -1) < it.viewedAt) history.saveRecent(it) }
            val searches = history.searchSnapshot().associateBy { it.query }
            d.searches.forEach { if ((searches[it.query]?.searchedAt ?: -1) < it.searchedAt) history.saveSearch(it) }
            history.trimRecent()
            history.trimSearches()
        }
        // Preferences live in DataStore; report a partial restore rather than hide a failure.
        val warnings = mutableListOf<String>()
        try { profiles.restore(d.profile) }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { warnings += "Profile could not be restored. Try importing again." }
        try { settings.restore(d.preferences) }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { warnings += "Settings could not be restored. Try importing again." }
        return RestoreResult(warnings)
    }
}

package com.dzaky.anitrack.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.dzaky.anitrack.domain.WatchProgress
import com.dzaky.anitrack.domain.WatchStatus
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY updatedAt DESC, animeId")
    abstract fun observeAll(): Flow<List<WatchEntry>>

    @Query("SELECT * FROM watchlist WHERE animeId = :id")
    abstract fun observe(id: Int): Flow<WatchEntry?>

    @Query("SELECT * FROM watchlist WHERE animeId = :id")
    abstract suspend fun get(id: Int): WatchEntry?

    @Upsert
    abstract suspend fun save(entry: WatchEntry)

    @Query("DELETE FROM watchlist WHERE animeId = :id")
    abstract suspend fun remove(id: Int)

    @Transaction
    open suspend fun addIfAbsent(entry: WatchEntry) {
        if (get(entry.animeId) == null) save(entry)
    }

    @Transaction
    open suspend fun adjustEpisodes(id: Int, amount: Int, now: Long) {
        val entry = get(id) ?: return
        applyProgress(entry, WatchProgress(entry.watchedEpisodes, entry.status)
            .changeEpisodes(entry.watchedEpisodes.toLong() + amount, entry.totalEpisodes), now)
    }

    @Transaction
    open suspend fun setEpisodes(id: Int, watched: Int, now: Long) {
        val entry = get(id) ?: return
        applyProgress(entry, WatchProgress(entry.watchedEpisodes, entry.status)
            .changeEpisodes(watched.toLong(), entry.totalEpisodes), now)
    }

    @Transaction
    open suspend fun setStatus(id: Int, status: WatchStatus, now: Long) {
        val entry = get(id) ?: return
        applyProgress(entry, WatchProgress(entry.watchedEpisodes, entry.status)
            .changeStatus(status, entry.totalEpisodes), now)
    }

    private suspend fun applyProgress(entry: WatchEntry, progress: WatchProgress, now: Long) {
        save(entry.copy(watchedEpisodes = progress.watched, status = progress.status, updatedAt = now))
    }
}

package com.dzaky.anitrack.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HistoryDao {
    @Query("SELECT * FROM recently_viewed ORDER BY viewedAt DESC, animeId")
    abstract fun observeRecent(): Flow<List<RecentAnimeEntity>>

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC, query")
    abstract fun observeSearches(): Flow<List<SearchHistoryEntity>>

    @Upsert
    abstract suspend fun saveRecent(entry: RecentAnimeEntity)

    @Query("DELETE FROM recently_viewed WHERE animeId NOT IN (SELECT animeId FROM recently_viewed ORDER BY viewedAt DESC, animeId LIMIT 30)")
    abstract suspend fun trimRecent()

    @Upsert
    abstract suspend fun saveSearch(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query NOT IN (SELECT query FROM search_history ORDER BY searchedAt DESC, query LIMIT 15)")
    abstract suspend fun trimSearches()

    @Query("DELETE FROM search_history WHERE query = :query")
    abstract suspend fun removeSearch(query: String)

    @Query("DELETE FROM search_history")
    abstract suspend fun clearSearches()

    @Query("DELETE FROM recently_viewed")
    abstract suspend fun clearRecent()

    @Transaction
    open suspend fun recordView(entry: RecentAnimeEntity) {
        saveRecent(entry)
        trimRecent()
    }

    @Transaction
    open suspend fun recordSearch(entry: SearchHistoryEntity) {
        saveSearch(entry)
        trimSearches()
    }
}

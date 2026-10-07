package com.dzaky.anitrack.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
abstract class AnimeCacheDao {
    @Query("SELECT * FROM anime_cache WHERE animeId = :id")
    abstract suspend fun get(id: Int): CachedAnimeEntity?

    @Upsert
    abstract suspend fun save(entry: CachedAnimeEntity)

    @Query("DELETE FROM anime_cache WHERE animeId NOT IN (SELECT animeId FROM anime_cache ORDER BY fetchedAt DESC LIMIT 100) AND animeId NOT IN (SELECT animeId FROM favorites) AND animeId NOT IN (SELECT animeId FROM watchlist)")
    abstract suspend fun trim()

    @Transaction
    open suspend fun cache(entry: CachedAnimeEntity) {
        save(entry)
        trim()
    }
}

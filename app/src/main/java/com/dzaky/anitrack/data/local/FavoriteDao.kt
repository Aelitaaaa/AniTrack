package com.dzaky.anitrack.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC, animeId")
    abstract fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE animeId = :id")
    abstract fun observe(id: Int): Flow<FavoriteEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE animeId = :id)")
    abstract suspend fun contains(id: Int): Boolean

    @Upsert
    abstract suspend fun save(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE animeId = :id")
    abstract suspend fun remove(id: Int)

    @Transaction
    open suspend fun toggle(favorite: FavoriteEntity) {
        if (contains(favorite.animeId)) remove(favorite.animeId) else save(favorite)
    }
}

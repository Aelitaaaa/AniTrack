package com.dzaky.anitrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [FavoriteEntity::class, WatchEntry::class, RecentAnimeEntity::class,
        SearchHistoryEntity::class, CachedAnimeEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(WatchStatusConverter::class)
abstract class AniTrackDatabase : RoomDatabase() {
    abstract fun favorites(): FavoriteDao
    abstract fun watchlist(): WatchlistDao
    abstract fun history(): HistoryDao
    abstract fun animeCache(): AnimeCacheDao
}

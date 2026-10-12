package com.dzaky.anitrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [FavoriteEntity::class, WatchEntry::class, RecentAnimeEntity::class,
        SearchHistoryEntity::class, CachedAnimeEntity::class, CollectionEntity::class,
        CollectionAnimeEntity::class, PersonalAnimeEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(WatchStatusConverter::class)
abstract class AniTrackDatabase : RoomDatabase() {
    abstract fun favorites(): FavoriteDao
    abstract fun watchlist(): WatchlistDao
    abstract fun history(): HistoryDao
    abstract fun animeCache(): AnimeCacheDao
    abstract fun extras(): ExtrasDao
}

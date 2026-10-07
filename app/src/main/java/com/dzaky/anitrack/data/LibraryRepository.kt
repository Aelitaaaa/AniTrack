package com.dzaky.anitrack.data

import com.dzaky.anitrack.data.local.FavoriteDao
import com.dzaky.anitrack.data.local.FavoriteEntity
import com.dzaky.anitrack.data.local.HistoryDao
import com.dzaky.anitrack.data.local.RecentAnimeEntity
import com.dzaky.anitrack.data.local.SearchHistoryEntity
import com.dzaky.anitrack.data.local.WatchEntry
import com.dzaky.anitrack.data.local.WatchlistDao
import com.dzaky.anitrack.domain.Anime
import com.dzaky.anitrack.domain.WatchStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val favoritesDao: FavoriteDao,
    private val watchlistDao: WatchlistDao,
    private val historyDao: HistoryDao,
) {
    val favorites = favoritesDao.observeAll()
    val watchlist = watchlistDao.observeAll()
    val recentlyViewed = historyDao.observeRecent()
    val searchHistory = historyDao.observeSearches()

    fun favorite(id: Int) = favoritesDao.observe(id)
    fun watchEntry(id: Int) = watchlistDao.observe(id)

    suspend fun toggleFavorite(anime: Anime) = favoritesDao.toggle(
        FavoriteEntity(anime.id, anime.title, anime.posterUrl, anime.score, anime.type,
            anime.year, anime.episodes, System.currentTimeMillis()),
    )

    suspend fun removeFavorite(id: Int) = favoritesDao.remove(id)

    suspend fun addToWatchlist(anime: Anime) = watchlistDao.addIfAbsent(
        WatchEntry(anime.id, anime.title, anime.posterUrl, anime.score, anime.type, anime.year,
            anime.episodes, WatchStatus.Planned, 0, System.currentTimeMillis()),
    )

    suspend fun removeFromWatchlist(id: Int) = watchlistDao.remove(id)
    suspend fun adjustEpisodes(id: Int, amount: Int) = watchlistDao.adjustEpisodes(id, amount, System.currentTimeMillis())
    suspend fun setEpisodes(id: Int, watched: Int) = watchlistDao.setEpisodes(id, watched, System.currentTimeMillis())
    suspend fun setStatus(id: Int, status: WatchStatus) = watchlistDao.setStatus(id, status, System.currentTimeMillis())

    suspend fun recordView(anime: Anime) = historyDao.recordView(
        RecentAnimeEntity(anime.id, anime.title, anime.posterUrl, anime.score, anime.type,
            anime.year, anime.episodes, System.currentTimeMillis()),
    )

    suspend fun recordSearch(query: String) {
        val normalized = query.trim().lowercase()
        if (normalized.isNotEmpty()) historyDao.recordSearch(SearchHistoryEntity(normalized, System.currentTimeMillis()))
    }

    suspend fun removeSearch(query: String) = historyDao.removeSearch(query)
    suspend fun clearSearchHistory() = historyDao.clearSearches()
    suspend fun clearRecentlyViewed() = historyDao.clearRecent()
}

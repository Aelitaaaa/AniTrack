package com.dzaky.anitrack.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryToolsTest {
    private data class Entry(val id: Int, val title: String, val score: Double?, val updated: Long, val watched: Int)
    private val entries = listOf(Entry(3, "Beta", null, 9, 1), Entry(2, "alpha", 8.0, 5, 3), Entry(1, "Alpha", 8.0, 5, Int.MAX_VALUE))
    private fun sorted(query: String, sort: LibrarySort) = filterLibrary(entries, query, sort, { it.title }, { it.id }, { it.score }, { it.updated }, { it.watched }).map { it.id }

    @Test fun sortingHandlesNullScoresAndStableTies() {
        assertEquals(listOf(1, 2, 3), sorted("", LibrarySort.Score))
        assertEquals(listOf(1, 2, 3), sorted("", LibrarySort.Title))
        assertEquals(listOf(3, 1, 2), sorted("", LibrarySort.Recent))
        assertEquals(listOf(1, 2, 3), sorted("", LibrarySort.Progress))
        assertEquals(listOf(1, 2), sorted("  ALP ", LibrarySort.Title))
    }

    @Test fun statisticsKeepUnknownTotalsSeparateAndAvoidOverflow() {
        val entries = listOf(
            StatisticsEntry(Int.MAX_VALUE, Int.MAX_VALUE, WatchStatus.Completed, "TV"),
            StatisticsEntry(Int.MAX_VALUE, null, WatchStatus.Watching, "TV"),
            StatisticsEntry(0, 12, WatchStatus.Planned, "Movie"),
        )
        val stats = LibraryStatistics.calculate(entries, listOf(8, 10, 0, 11))
        assertEquals(2L * Int.MAX_VALUE, stats.episodes)
        assertEquals(Int.MAX_VALUE.toLong(), stats.knownWatched)
        assertEquals(Int.MAX_VALUE.toLong() + 12, stats.knownTotal)
        assertEquals(1, stats.unknownTotals)
        assertEquals(9.0, stats.averageRating!!, 0.001)
        assertEquals(listOf("TV" to 2, "Movie" to 1), stats.formats)
        assertEquals(1, stats.completed)
    }

    @Test fun emptyStatisticsHaveNoInventedRating() {
        val stats = LibraryStatistics.calculate(emptyList(), emptyList())
        assertEquals(0L, stats.episodes)
        assertNull(stats.averageRating)
    }
}

package com.dzaky.anitrack.domain

import java.util.Locale

enum class LibrarySort(val label: String) {
    Recent("Recent"), Title("Title A–Z"), Score("Highest score"), Progress("Most episodes"),
}

fun <T> filterLibrary(
    items: List<T>, query: String, sort: LibrarySort,
    title: (T) -> String, id: (T) -> Int, score: (T) -> Double?,
    updated: (T) -> Long, progress: (T) -> Int = { 0 },
): List<T> {
    val text = query.trim()
    val names = compareBy<T> { title(it).lowercase(Locale.ROOT) }.thenBy(id)
    val comparator = when (sort) {
        LibrarySort.Recent -> compareByDescending(updated).then(names)
        LibrarySort.Title -> names
        LibrarySort.Score -> compareByDescending<T> { score(it) ?: Double.NEGATIVE_INFINITY }.then(names)
        LibrarySort.Progress -> compareByDescending(progress).then(names)
    }
    return items.filter { title(it).contains(text, ignoreCase = true) }.sortedWith(comparator)
}

data class StatisticsEntry(val watched: Int, val total: Int?, val status: WatchStatus, val format: String?)

data class LibraryStatistics(
    val titles: Int = 0, val episodes: Long = 0, val completed: Int = 0,
    val watching: Int = 0, val planned: Int = 0, val onHold: Int = 0, val dropped: Int = 0,
    val knownTotal: Long = 0,
    val unknownTotals: Int = 0, val knownWatched: Long = 0, val ratings: Int = 0, val averageRating: Double? = null,
    val formats: List<Pair<String, Int>> = emptyList(),
) {
    companion object {
        fun calculate(entries: List<StatisticsEntry>, ratings: List<Int>): LibraryStatistics {
            val validRatings = ratings.filter { it in 1..10 }
            return LibraryStatistics(
                titles = entries.size,
                episodes = entries.sumOf { it.watched.coerceAtLeast(0).toLong() },
                completed = entries.count { it.status == WatchStatus.Completed },
                watching = entries.count { it.status == WatchStatus.Watching },
                planned = entries.count { it.status == WatchStatus.Planned },
                onHold = entries.count { it.status == WatchStatus.OnHold },
                dropped = entries.count { it.status == WatchStatus.Dropped },
                knownTotal = entries.sumOf { (it.total ?: 0).coerceAtLeast(0).toLong() },
                unknownTotals = entries.count { it.total == null || it.total <= 0 },
                knownWatched = entries.filter { it.total != null && it.total > 0 }
                    .sumOf { it.watched.coerceIn(0, checkNotNull(it.total)).toLong() },
                ratings = validRatings.size,
                averageRating = validRatings.takeIf { it.isNotEmpty() }?.average(),
                formats = entries.groupingBy { it.format ?: "Other" }.eachCount().toList()
                    .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first }),
            )
        }
    }
}

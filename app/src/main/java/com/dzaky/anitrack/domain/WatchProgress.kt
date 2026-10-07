package com.dzaky.anitrack.domain

enum class WatchStatus(val label: String) {
    Planned("Plan to watch"),
    Watching("Watching"),
    Completed("Completed"),
}

data class WatchProgress(val watched: Int, val status: WatchStatus) {
    fun changeEpisodes(requested: Long, total: Int?): WatchProgress {
        val upperBound = total?.takeIf { it > 0 } ?: Int.MAX_VALUE
        val next = requested.coerceIn(0, upperBound.toLong()).toInt()
        val nextStatus = when {
            total != null && total > 0 && next == total -> WatchStatus.Completed
            status == WatchStatus.Completed -> WatchStatus.Watching
            next > 0 && status == WatchStatus.Planned -> WatchStatus.Watching
            else -> status
        }
        return WatchProgress(next, nextStatus)
    }

    fun changeStatus(next: WatchStatus, total: Int?): WatchProgress = when (next) {
        WatchStatus.Planned -> WatchProgress(0, next)
        WatchStatus.Completed -> WatchProgress(total?.takeIf { it > 0 } ?: watched, next)
        WatchStatus.Watching -> WatchProgress(watched, next)
    }
}

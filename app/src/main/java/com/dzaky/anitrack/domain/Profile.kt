package com.dzaky.anitrack.domain

import java.util.Locale

data class LocalProfile(val id: String, val displayName: String, val username: String, val bio: String, val createdAt: Long)

data class ProfileInput(val displayName: String, val username: String, val bio: String = "") {
    fun normalized() = copy(displayName = displayName.trim(), username = username.trim().lowercase(Locale.ROOT), bio = bio.trim())
    fun validationError(): String? {
        val input = normalized()
        return when {
            input.displayName.length !in 2..30 -> "Use a name with 2–30 characters."
            !input.username.matches(Regex("[a-z0-9_]{3,20}")) -> "Use 3–20 letters, numbers, or underscores for your username."
            input.bio.length > 120 -> "Keep your bio within 120 characters."
            else -> null
        }
    }
}

data class ProfileStats(val episodes: Long = 0, val completed: Int = 0, val favorites: Int = 0)

enum class AnimeBadge(val title: String, val requirement: String, val episodes: Long = 0, val completed: Int = 0, val favorites: Int = 0) {
    PendatangBaru("Pendatang Baru", "Create your local profile."),
    MulaiNonton("Mulai Nonton", "Record your first watched episode.", episodes = 1),
    Kolektor("Kolektor", "Save 5 anime to your favorites.", favorites = 5),
    Maraton("Maraton", "Record 100 watched episodes.", episodes = 100),
    SepuhAnime("Sepuh Anime", "Finish 25 anime and record 500 watched episodes.", episodes = 500, completed = 25),
    ;

    fun qualifies(stats: ProfileStats): Boolean = stats.episodes >= episodes && stats.completed >= completed && stats.favorites >= favorites

    fun progress(stats: ProfileStats): Float = listOfNotNull(
        episodes.takeIf { it > 0 }?.let { stats.episodes.toFloat() / it },
        completed.takeIf { it > 0 }?.let { stats.completed.toFloat() / it },
        favorites.takeIf { it > 0 }?.let { stats.favorites.toFloat() / it },
    ).minOrNull()?.coerceIn(0f, 1f) ?: 1f

    fun progressLabel(stats: ProfileStats): String = buildList {
        if (episodes > 0) add("${stats.episodes.coerceAtMost(episodes)} / $episodes episodes")
        if (completed > 0) add("${stats.completed.coerceAtMost(completed)} / $completed finished")
        if (favorites > 0) add("${stats.favorites.coerceAtMost(favorites)} / $favorites favorites")
    }.joinToString(" · ")
}
